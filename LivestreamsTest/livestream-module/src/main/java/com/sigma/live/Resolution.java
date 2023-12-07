package com.sigma.live;

public enum Resolution {

    FULLHD15(1920, 1080, 2000, 15),
    HD15(1280, 720, 1500, 15),
    SD15(854, 480, 800, 15),
    SSD15(640, 360, 400, 15),
    FULLHD25(1920, 1080, 3800, 25),
    HD25(1280, 720, 2200, 25),
    SD25(854, 480, 900, 25),
    SSD25(640, 360, 500, 25),
    FULLHD30(1920, 1080, 4000, 30),
    HD30(1280, 720, 2500, 30),
    SD30(854, 480, 1000, 30),
    SSD30(640, 360, 600, 30),
    FULLHD50(1920, 1080, 6000, 50),
    HD50(1280, 720, 4500, 50),
    SD50(854, 480, 3000, 50),
    SSD50(640, 360, 1600, 50),
    FULLHD60(1920, 1080, 6500, 60),
    HD60(1280, 720, 5000, 60),
    SD60(854, 480, 3500, 60),
    SSD60(640, 360, 2100, 60);
    private int mWidth, mHeight, mVBitrate, mABitrate, mFps;

    public static int[] MFPS = {15,25, 30, 50, 60};

    Resolution(int width, int height, int bitrate, int fps) {
        mWidth = width;
        mHeight = height;
        mFps = fps;
        mVBitrate = bitrate * 1000;
        mABitrate = 160000;
    }

    public int getValue() {
        return mWidth * mHeight;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getAudioBitrate() {
        return mABitrate;
    }

    public int getVideoBitrate() {
        return mVBitrate;
    }

    public int getFps() {
        return mFps;
    }
}