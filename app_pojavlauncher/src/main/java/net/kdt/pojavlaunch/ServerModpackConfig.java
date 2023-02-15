package net.kdt.pojavlaunch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ServerModpackConfig {
    private final Properties properties;
    private final File homeGameDirectory;
    private final File configFile;
    private final String versionName;
    private ServerModpackConfig(Properties properties, File homeGameDirectory, File configFile, String versionName) {
        this.properties = properties;
        this.homeGameDirectory = homeGameDirectory;
        this.configFile = configFile;
        this.versionName = versionName;
    }
    public void save() {
        try {
            FileOutputStream configOut = new FileOutputStream(configFile);
            properties.store(configOut, "Pojav Properties");
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static ServerModpackConfig load(String versionName) {

        File homeGameDirectory = new File(Tools.DIR_GAME_HOME, versionName);
        if(!homeGameDirectory.exists()) homeGameDirectory.mkdirs();
        File configFile = new File(homeGameDirectory, "pojav-config.properties");
        Properties properties = new Properties();
        try {
            FileInputStream configStream = new FileInputStream(configFile);
            properties.load(configStream);
            configStream.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
        return new ServerModpackConfig(properties, homeGameDirectory, configFile, versionName);
    }
    public @NonNull String getGameDirectory() {
        return homeGameDirectory.getAbsolutePath();
    }
    public @Nullable String getJavaRuntime() {
        String args = properties.getProperty("javaRuntime");
        if(args != null && args.isEmpty()) return null;
        return args;
    }
    public void setJavaRuntime(String runtime) {
        properties.setProperty("javaRuntime", runtime);
        save();
    }
    public @Nullable String getRenderer() {
        String args = properties.getProperty("renderer");
        if(args != null && args.isEmpty()) return null;
        return args;
    }
    public void setRenderer(String renderer) {
        properties.setProperty("renderer", renderer);
        save();
    }
    public @NonNull String getVersionName() {
        return versionName;
    }
    public @Nullable String getControlFile() {
        String args = properties.getProperty("controlFile");
        if(args != null && args.isEmpty()) return null;
        return args;
    }
    public void setControlFile(String controlFile) {
        properties.setProperty("controlFile", controlFile);
        save();
    }
    public @Nullable String getJvmArgs() {
        String args = properties.getProperty("jvmArgs");
        if(args != null && args.isEmpty()) return null;
        return args;
    }
    public void setJvmArgs(String args) {
        properties.setProperty("jvmArgs", args);
        save();
    }
}

