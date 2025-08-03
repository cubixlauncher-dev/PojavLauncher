package net.kdt.pojavlaunch.value;

import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CubixFileInfo {
    public String path;
    public String sha1;
    public String url;
    public long size;
    public boolean check = true;
}
