package com.sigma.live;

public enum Resolution {
    FULLHD(1920, 1080, 4000),
    HD(1280, 720, 2500),
    SD(854, 480, 1000),
    SSD(640, 360, 600);
    private int mWidth, mHeight, mVBitrate, mABitrate;

    Resolution(int width, int height, int bitrate) {
        mWidth = width;
        mHeight = height;
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
}