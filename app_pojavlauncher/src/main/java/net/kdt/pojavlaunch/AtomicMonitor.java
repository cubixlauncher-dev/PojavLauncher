package net.kdt.pojavlaunch;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicMonitor implements Tools.DownloaderFeedback{
    private int mLastCurr = 0;
    private AtomicLong mAllDownloads;
    public AtomicMonitor(AtomicLong globalCounter) {
        this.mAllDownloads = globalCounter;
    }
    @Override
    public void updateProgress(int curr, int max) {
        mAllDownloads.addAndGet(curr - mLastCurr);
        mLastCurr = curr;
    }
}
