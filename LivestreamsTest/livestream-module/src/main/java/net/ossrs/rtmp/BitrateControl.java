package net.ossrs.rtmp;

public class BitrateControl {
    private Thread mThread;
    private static BitrateControl mInstance = new BitrateControl();
    private ConnectCheckerRtmp mListener;
    private int mLimit;
    private long mLastTime;
    private int mLastRemain;
    private long mBitrate;
    private boolean mEnabled;

    public static BitrateControl getInstance() {
        return mInstance;
    }

    private BitrateControl() {
    }

    void setup(ConnectCheckerRtmp listener, int limit) {
        mListener = listener;
        mLimit = limit;
        mEnabled = mBitrate > 0 && mLimit > 0;
    }

    public void setMaxBitrate(long bitrate) {
        mBitrate = bitrate;
        mEnabled = mBitrate > 0 && mLimit > 0;
    }

    public void update(int current) {
        if (!mEnabled) return;
        long now = System.currentTimeMillis();
        long pass = now - mLastTime;
        if (pass < 1000 || !mEnabled) return;
        int remain = 100 - current * 100 / mLimit;
        int top = 90;
        int bottom = 20;
        if (remain > top)
            remain = 100;
        else if (remain < bottom)
            remain = bottom;
        else remain = (remain - bottom) * 80 / (top - bottom) + 20;
        if ((remain > mLastRemain && pass > 2000) || remain < mLastRemain) {
            mLastRemain = remain;
            mListener.onNewBitrateRtmp(mBitrate * remain / 100);
            mLastTime = now;
        }
    }
}
