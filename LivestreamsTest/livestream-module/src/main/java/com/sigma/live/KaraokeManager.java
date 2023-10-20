package com.sigma.live;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class KaraokeManager {
    public enum EffectType {
        Echo,
        //Reverb,
        //Equalizer
    }

    private static KaraokeManager mInstance = new KaraokeManager();

    public static KaraokeManager getInstance() {
        return mInstance;
    }

    class BaseEffect {
        void apply() {
        }

        void refresh() {
        }
    }

    class EchoEffect extends BaseEffect {
        float mInGain, mOutGane;
        int[] mDelays;
        float[] mDecays;
        int mDistances[];
        short mHistories[];
        int mMaxDistance;
        int mHistoryLength;
        int mIndex;

        void config(float inGain, float outGain, int[] delays, float[] decays) {
            mInGain = inGain;
            mOutGane = outGain;
            mDelays = delays;
            mDecays = decays;
            mDistances = new int[mDelays.length];
            refresh();
        }

        @Override
        void refresh() {
            mMaxDistance = 0;
            for (int i = 0; i < mDelays.length; i++) {
                mDistances[i] = mDelays[i] * mSampleRate / 1000;
                mMaxDistance = Math.max(mMaxDistance, mDistances[i]);
            }
            mIndex = 0;
            mHistoryLength = mChannel * mMaxDistance;
            mHistories = new short[mHistoryLength];
        }

        int mod(int value, int max) {
            if (value >= max)
                return value % max;
            return value;
        }

        short clip(float value) {
            if (value > Short.MAX_VALUE)
                return Short.MAX_VALUE;
            if (value < Short.MIN_VALUE)
                return Short.MIN_VALUE;
            return (short) value;
        }

        @Override
        void apply() {
            float out;
            for (int i = 0; i < mSampleCount; i++) {
                out = mSampleBuffer[i] * mInGain;
                for (int j = 0; j < mDecays.length; j++) {
                    int ix = mIndex + (mMaxDistance - mDistances[j]) * mChannel;
                    ix = mod(ix, mHistoryLength);
                    out += mHistories[ix] * mDecays[j];
                }
                out *= mOutGane;
                mHistories[mIndex] = mSampleBuffer[i];
                mIndex = mod(mIndex + 1, mHistoryLength);
                mSampleBuffer[i] = clip(out);
            }
        }
    }

    private EchoEffect mEcho = new EchoEffect();
    private int mChannel;
    private int mSampleRate;
    private short mSampleBuffer[];
    private byte mResultBuffer[];
    private int mSampleCount;

    private ArrayList<BaseEffect> mEffects = new ArrayList<>();

    public void enableEcho() {
        enableEcho(0.6f, 0.3f, new int[]{1000}, new float[]{0.5f});
    }

    public void enableEcho(float inGain, float outGain, int[] delays, float[] decays) {
        disableEcho();
        mEcho.config(inGain, outGain, delays, decays);
        mEffects.add(mEcho);
    }

    public void disableEcho() {
        mEffects.remove(mEcho);
    }

    public void start(int channel, int sampleRate, int bufferSize) {
        mChannel = channel;
        mSampleRate = sampleRate;
        mSampleBuffer = new short[bufferSize / 2];
        mResultBuffer = new byte[bufferSize];
        for (BaseEffect effect : mEffects)
            effect.refresh();
    }

    public byte[] apply(byte[] data, int len) {
        if (mEffects.isEmpty()) return data;
        mSampleCount = len / 2;
        ByteBuffer.wrap(data, 0, len).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(mSampleBuffer);
        for (BaseEffect effect : mEffects)
            effect.apply();
        ByteBuffer.wrap(mResultBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(mSampleBuffer, 0, mSampleCount);
        return mResultBuffer;
    }
}
