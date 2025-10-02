package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.Architecture.archAsString;

import android.app.Activity;
import android.util.Log;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.multirt.Runtime;
import net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper;
import net.kdt.pojavlaunch.utils.DownloadUtils;
import net.kdt.pojavlaunch.utils.MathUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import git.artdeell.mojo.R;

public class NewJREUtil {
    private static boolean checkInternalRuntime(InternalRuntime internalRuntime) throws IOException {
        String launcher_runtime_version;
        String installed_runtime_version = MultiRTUtils.readInternalRuntimeVersion(internalRuntime.name);
        launcher_runtime_version = DownloadUtils.downloadString(internalRuntime.url+".version");
        // this implicitly checks for null, so it will unpack the runtime even if we don't have one installed
        if(!launcher_runtime_version.equals(installed_runtime_version))
            return unpackInternalRuntime(internalRuntime, launcher_runtime_version);
        else return true;
    }

    private static boolean unpackInternalRuntime(InternalRuntime internalRuntime, String version) {
        try {
            File runtimeFile = new File(Tools.DIR_CACHE, "install_runtime");
            DownloadUtils.downloadFileMonitored(internalRuntime.url, runtimeFile, new byte[8192], new DownloaderProgressWrapper(R.string.download_java_runtime, ProgressLayout.UNPACK_RUNTIME));
            try(ZipFile zipFile = new ZipFile(runtimeFile)) {
                ZipEntry universalEntry = zipFile.getEntry("universal.tar.xz");
                ZipEntry binaryEntry = zipFile.getEntry("bin-" + archAsString(Tools.DEVICE_ARCHITECTURE) + ".tar.xz");
                if(universalEntry == null ||  binaryEntry == null) return false;
                MultiRTUtils.installRuntimeNamedBinpack(
                        zipFile.getInputStream(universalEntry),
                        zipFile.getInputStream(binaryEntry),
                                internalRuntime.name, version);
                MultiRTUtils.postPrepare(internalRuntime.name);
            } finally {
                runtimeFile.deleteOnExit();
            }

            return true;
        }catch (IOException e) {
            Log.e("NewJREAuto", "Internal JRE unpack failed", e);
            return false;
        }
    }

    private static InternalRuntime getInternalRuntime(Runtime runtime) {
        for(InternalRuntime internalRuntime : InternalRuntime.values()) {
            if(internalRuntime.name.equals(runtime.name)) return internalRuntime;
        }
        return null;
    }

    private static MathUtils.RankedValue<Runtime> getNearestInstalledRuntime(int targetVersion) {
        List<Runtime> runtimes = MultiRTUtils.getRuntimes();
        return MathUtils.findNearestPositive(targetVersion, runtimes, (runtime)->runtime.javaVersion);
    }

    private static MathUtils.RankedValue<InternalRuntime> getNearestInternalRuntime(int targetVersion) {
        List<InternalRuntime> runtimeList = Arrays.asList(InternalRuntime.values());
        return MathUtils.findNearestPositive(targetVersion, runtimeList, (runtime)->runtime.majorVersion);
    }


    /** @return true if everything is good, false otherwise.  */
    public static boolean installNewJreIfNeeded(Activity activity, JMinecraftVersionList.Version versionInfo, ServerModpackConfig config) throws IOException {
        //Now we have the reliable information to check if our runtime settings are good enough
        if (versionInfo.javaVersion == null || versionInfo.javaVersion.component.equalsIgnoreCase("jre-legacy"))
            return true;

        int gameRequiredVersion = versionInfo.javaVersion.majorVersion;

        String profileRuntime = Tools.getSelectedRuntime(config);
        Runtime runtime = MultiRTUtils.read(profileRuntime);
        // Partly trust the user with his own selection, if the game can even try to run in this case
        if (runtime.javaVersion >= gameRequiredVersion) {
            // Check whether the selection is an internal runtime
            InternalRuntime internalRuntime = getInternalRuntime(runtime);
            // If it is, check if updates are available from the APK file
            if(internalRuntime != null) {
                // Not calling showRuntimeFail on failure here because we did, technically, find the compatible runtime
                return checkInternalRuntime(internalRuntime);
            }
            return true;
        }

        // If the runtime version selected by the user is not appropriate for this version (which means the game won't run at all)
        // automatically pick from either an already installed runtime, or a runtime packed with the launcher
        MathUtils.RankedValue<?> nearestInstalledRuntime = getNearestInstalledRuntime(gameRequiredVersion);
        MathUtils.RankedValue<?> nearestInternalRuntime = getNearestInternalRuntime(gameRequiredVersion);

        MathUtils.RankedValue<?> selectedRankedRuntime = MathUtils.objectMin(
                nearestInternalRuntime, nearestInstalledRuntime, (value)->value.rank
        );

        // No possible selections
        if(selectedRankedRuntime == null) {
            showRuntimeFail(activity, versionInfo);
            return false;
        }

        Object selected = selectedRankedRuntime.value;
        String appropriateRuntime;
        InternalRuntime internalRuntime;

        // Perform checks on the picked runtime
        if(selected instanceof Runtime) {
            // If it's an already installed runtime, save its name and check if
            // it's actually an internal one (just in case)
            Runtime selectedRuntime = (Runtime) selected;
            appropriateRuntime = selectedRuntime.name;
            internalRuntime = getInternalRuntime(selectedRuntime);
        } else if (selected instanceof InternalRuntime) {
            // If it's an internal runtime, set it's name as the appropriate one.
            internalRuntime = (InternalRuntime) selected;
            appropriateRuntime = internalRuntime.name;
        } else {
            throw new RuntimeException("Unexpected type of selected: "+selected.getClass().getName());
        }

        // If it turns out the selected runtime is actually an internal one, attempt automatic installation or update
        if(internalRuntime != null && !checkInternalRuntime(internalRuntime)) {
            // Not calling showRuntimeFail here because we did, technically, find the compatible runtime
            return false;
        }

        config.setJavaRuntime(appropriateRuntime);
        try {
            config.save();
        }catch (Exception e) {
            Tools.showErrorRemote(e);
            return false;
        }
        return true;
    }

    private static void showRuntimeFail(Activity activity, JMinecraftVersionList.Version verInfo) {
        Tools.dialogOnUiThread(activity, activity.getString(R.string.global_error),
                activity.getString(R.string.multirt_nocompatiblert, verInfo.javaVersion.majorVersion));
    }

    private enum InternalRuntime {
        JRE_21(21, "Internal-21", "https://launcher2.cubixworld.mobi/updates/libs/jre21-pojav.zip");
        public final int majorVersion;
        public final String name;
        public final String url;
        InternalRuntime(int majorVersion, String name, String path) {
            this.majorVersion = majorVersion;
            this.name = name;
            this.url = path;
        }
    }

}