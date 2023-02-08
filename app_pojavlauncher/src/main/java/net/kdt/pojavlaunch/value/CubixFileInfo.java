package net.kdt.pojavlaunch.value;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CubixFileInfo implements Runnable {
    private File destinationPath = null;
    private Tools.DownloaderFeedback monitor = null;
    private AtomicBoolean interrupt = null;
    public String path;
    public String sha1;
    public String url;
    public long size;
    public boolean check = true;

    public void setDownloaderData(File path, Tools.DownloaderFeedback monitor, AtomicBoolean interrupt) {
        this.destinationPath = path;
        this.monitor = monitor;
        this.interrupt = interrupt;
    }

    @Override
    public void run() {
        if(path.startsWith("/")) path = "."+path; // нельзя засовывать файлы в /; ай-ай-ай предыдущему разрабу!
        destinationPath = new File(destinationPath, path);
        boolean fileInvalid = !destinationPath.exists();
        if(check && !fileInvalid) fileInvalid = Tools.compareSHA1(destinationPath, sha1);
        if(fileInvalid) {
            try {
                DownloadUtils.downloadFileMonitored(url, destinationPath, null, monitor);
            }catch (IOException e) {
                e.printStackTrace();
                interrupt.set(false);
            }
        }

    }
}
