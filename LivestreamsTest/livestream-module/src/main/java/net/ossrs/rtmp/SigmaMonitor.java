package net.ossrs.rtmp;

import com.sigma.live.TrackInfo;

public class SigmaMonitor {
    private static final int mLength = 200;
    private static long mTime[] = new long[mLength];
    private static long mSize[] = new long[mLength];
    private static boolean mVideo[] = new boolean[mLength];
    private static int mIndex;
    private static int mDuration = 2000;

    static void track(long size, boolean isVideo) {
        mTime[mIndex] = System.currentTimeMillis();
        mSize[mIndex] = size;
        mVideo[mIndex] = isVideo;
        mIndex++;
        if (mIndex == mLength)
            mIndex = 0;
    }

    public static TrackInfo getInfo() {
        long now = System.currentTimeMillis();
        long from = now - mDuration;
        long time = now;
        int totalFrame = 0;
        long totalByte = 0;
        for (int i = 0; i < mLength; i++) {
            if (mTime[i] >= from) {
                totalByte += mSize[i];
                if (mVideo[i])
                    totalFrame++;
                time = Math.min(time, mTime[i]);
            }
        }
        int delta = (int) (now - time);
        if (delta == 0)
            delta = 1000;
        int fps = totalFrame * 1000 / delta;
        int bitrate = (int) (totalByte * 8 * 1000 / delta);
        return new TrackInfo(fps, bitrate);
    }
}
