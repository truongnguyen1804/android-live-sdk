package net.ossrs.rtmp;

public class FpsCounter {
    private long mLastTime;
    private int mFrameCount;

    public void onFrame() {
        mFrameCount++;
        long now = System.currentTimeMillis();
        long delta = now - mLastTime;
        if (delta > 1000) {
            mLastTime = now;
            System.out.println("FpsCounter: " + mFrameCount * 1000 / delta);
            mFrameCount = 0;
        }
    }
}
