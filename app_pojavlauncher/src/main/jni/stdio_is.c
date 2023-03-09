#include <jni.h>
#include <sys/types.h>
#include <stdbool.h>
#include <unistd.h>
#include <pthread.h>
#include <stdio.h>
#include <xhook.h>

//
// Created by maks on 17.02.21.
//
static JavaVM *stdiois_jvm;
static volatile jmethodID log_cbMethod;
static volatile jobject log_cbObject;
static volatile jobject exitTrap_ctx;
static volatile jclass exitTrap_exitClass;
static volatile jmethodID exitTrap_staticMethod;
static JavaVM *exitTrap_jvm;
static int pfd[2];
static pthread_t logger;

unsigned char is_utf8(const char * string)
{
    if(!string)
        return 0;

    const unsigned char * bytes = (const unsigned char *)string;
    while(*bytes)
    {
        if( (// ASCII
                // use bytes[0] <= 0x7F to allow ASCII control characters
                bytes[0] == 0x09 ||
                bytes[0] == 0x0A ||
                bytes[0] == 0x0D ||
                (0x20 <= bytes[0] && bytes[0] <= 0x7E)
        )
                ) {
            bytes += 1;
            continue;
        }

        if( (// non-overlong 2-byte
                (0xC2 <= bytes[0] && bytes[0] <= 0xDF) &&
                (0x80 <= bytes[1] && bytes[1] <= 0xBF)
        )
                ) {
            bytes += 2;
            continue;
        }

        if( (// excluding overlongs
                    bytes[0] == 0xE0 &&
                    (0xA0 <= bytes[1] && bytes[1] <= 0xBF) &&
                    (0x80 <= bytes[2] && bytes[2] <= 0xBF)
            ) ||
            (// straight 3-byte
                    ((0xE1 <= bytes[0] && bytes[0] <= 0xEC) ||
                     bytes[0] == 0xEE ||
                     bytes[0] == 0xEF) &&
                    (0x80 <= bytes[1] && bytes[1] <= 0xBF) &&
                    (0x80 <= bytes[2] && bytes[2] <= 0xBF)
            ) ||
            (// excluding surrogates
                    bytes[0] == 0xED &&
                    (0x80 <= bytes[1] && bytes[1] <= 0x9F) &&
                    (0x80 <= bytes[2] && bytes[2] <= 0xBF)
            )
                ) {
            bytes += 3;
            continue;
        }

        if( (// planes 1-3
                    bytes[0] == 0xF0 &&
                    (0x90 <= bytes[1] && bytes[1] <= 0xBF) &&
                    (0x80 <= bytes[2] && bytes[2] <= 0xBF) &&
                    (0x80 <= bytes[3] && bytes[3] <= 0xBF)
            ) ||
            (// planes 4-15
                    (0xF1 <= bytes[0] && bytes[0] <= 0xF3) &&
                    (0x80 <= bytes[1] && bytes[1] <= 0xBF) &&
                    (0x80 <= bytes[2] && bytes[2] <= 0xBF) &&
                    (0x80 <= bytes[3] && bytes[3] <= 0xBF)
            ) ||
            (// plane 16
                    bytes[0] == 0xF4 &&
                    (0x80 <= bytes[1] && bytes[1] <= 0x8F) &&
                    (0x80 <= bytes[2] && bytes[2] <= 0xBF) &&
                    (0x80 <= bytes[3] && bytes[3] <= 0xBF)
            )
                ) {
            bytes += 4;
            continue;
        }

        return 0;
    }

    return 1;
}


static void *logger_thread() {
    JNIEnv *env;
    jstring str;
    (*stdiois_jvm)->AttachCurrentThread(stdiois_jvm, &env, NULL);
    ssize_t  rsize;
    char buf[2048];
    while((rsize = read(pfd[0], buf, sizeof(buf)-1)) > 0) {
        if(buf[rsize-1]=='\n') {
            rsize=rsize-1;
        }
        buf[rsize]=0x00;
        if(is_utf8(buf)) {
            str = (*env)->NewStringUTF(env, buf);
            (*env)->CallVoidMethod(env, log_cbObject, log_cbMethod, str);
            (*env)->DeleteLocalRef(env, str);
        }
    }
    (*env)->DeleteGlobalRef(env, log_cbMethod);
    (*env)->DeleteGlobalRef(env, log_cbObject);
    (*stdiois_jvm)->DetachCurrentThread(stdiois_jvm);
    return NULL;
}
JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_utils_JREUtils_logToLogger(JNIEnv *env, __attribute((unused)) jclass clazz, jobject javaLogger) {
    // TODO: implement logToActivity()
    jclass loggableActivityClass = (*env)->FindClass(env,"net/kdt/pojavlaunch/Logger");
    log_cbMethod = (*env)->GetMethodID(env, loggableActivityClass, "appendToLog", "(Ljava/lang/String;)V");
    (*env)->GetJavaVM(env,&stdiois_jvm);
    log_cbObject = (*env)->NewGlobalRef(env, javaLogger);

    setvbuf(stdout, 0, _IOLBF, 0); // make stdout line-buffered
    setvbuf(stderr, 0, _IONBF, 0); // make stderr unbuffered

    /* create the pipe and redirect stdout and stderr */
    pipe(pfd);
    dup2(pfd[1], 1);
    dup2(pfd[1], 2);

    /* spawn the logging thread */
    if(pthread_create(&logger, 0, logger_thread, 0) != 0) {
        jstring str = (*env)->NewStringUTF(env,"Failed to start logging!");
        (*env)->CallVoidMethod(env, log_cbObject, log_cbMethod, str);
        (*env)->DeleteLocalRef(env,str);
        (*env)->DeleteGlobalRef(env, log_cbMethod);
        (*env)->DeleteGlobalRef(env, log_cbObject);
    }
    pthread_detach(logger);

}
void (*old_exit)(int code);
void custom_exit(int code) {
    if(code != 0) {
        JNIEnv *env;
        (*exitTrap_jvm)->AttachCurrentThread(exitTrap_jvm, &env, NULL);
        (*env)->CallStaticVoidMethod(env, exitTrap_exitClass, exitTrap_staticMethod, exitTrap_ctx,
                                     code);
        (*env)->DeleteGlobalRef(env, exitTrap_ctx);
        (*env)->DeleteGlobalRef(env, exitTrap_exitClass);
        (*exitTrap_jvm)->DetachCurrentThread(exitTrap_jvm);
    }
    old_exit(code);
}
JNIEXPORT void JNICALL Java_net_kdt_pojavlaunch_utils_JREUtils_setupExitTrap(JNIEnv *env, __attribute((unused)) jclass clazz, jobject context) {
    exitTrap_ctx = (*env)->NewGlobalRef(env,context);
    (*env)->GetJavaVM(env,&exitTrap_jvm);
    exitTrap_exitClass = (*env)->NewGlobalRef(env,(*env)->FindClass(env,"net/kdt/pojavlaunch/ExitActivity"));
    exitTrap_staticMethod = (*env)->GetStaticMethodID(env,exitTrap_exitClass,"showExitMessage","(Landroid/content/Context;I)V");
    xhook_enable_debug(0);
    xhook_register(".*\\.so$", "exit", custom_exit, (void **) &old_exit);
    xhook_refresh(1);
}