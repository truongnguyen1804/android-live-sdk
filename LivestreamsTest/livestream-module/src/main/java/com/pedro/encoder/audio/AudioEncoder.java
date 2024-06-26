package com.pedro.encoder.audio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.pedro.encoder.input.audio.GetMicrophoneData;
import com.pedro.encoder.utils.CodecUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pedro on 19/01/17.
 * <p>
 * Encode PCM audio data to ACC and return in a callback
 */

public class AudioEncoder implements GetMicrophoneData {

    long last = 0;
    private String TAG = "AudioEncoder";
    private MediaCodec audioEncoder;
    private GetAacData getAacData;
    private MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();
    private long presentTimeUs;
    private boolean running;
    //default parameters for com.sigma.live.encoder
    private CodecUtil.Force force = CodecUtil.Force.FIRST_COMPATIBLE_FOUND;
    private int bitRate = 64 * 1024;  //in kbps
    private int sampleRate = 32000; //in hz
    private boolean isStereo = true;

    public AudioEncoder(GetAacData getAacData) {
        this.getAacData = getAacData;
    }

    /**
     * Prepare com.sigma.live.encoder with custom parameters
     */
    public boolean prepareAudioEncoder(int bitRate, int sampleRate, boolean isStereo) {
        this.sampleRate = sampleRate;
        try {
            List<MediaCodecInfo> encoders = new ArrayList<>();
            if (force == CodecUtil.Force.HARDWARE) {
                encoders = CodecUtil.getAllHardwareEncoders(CodecUtil.AAC_MIME);
            } else if (force == CodecUtil.Force.SOFTWARE) {
                encoders = CodecUtil.getAllSoftwareEncoders(CodecUtil.AAC_MIME);
            }

            if (force == CodecUtil.Force.FIRST_COMPATIBLE_FOUND) {
                MediaCodecInfo encoder = chooseAudioEncoder(CodecUtil.AAC_MIME);
                if (encoder != null) {
                    audioEncoder = MediaCodec.createByCodecName(encoder.getName());
                } else {
                    Log.e(TAG, "Valid com.sigma.live.encoder not found");
                    return false;
                }
            } else {
                if (encoders.isEmpty()) {
                    Log.e(TAG, "Valid com.sigma.live.encoder not found");
                    return false;
                } else {
                    audioEncoder = MediaCodec.createByCodecName(encoders.get(0).getName());
                }
            }

            int a = (isStereo) ? 2 : 1;
            MediaFormat audioFormat = MediaFormat.createAudioFormat(CodecUtil.AAC_MIME, sampleRate, a);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            audioEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            running = false;
            return true;
        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "Create AudioEncoder failed.", e);
            return false;
        }
    }

    public void setForce(CodecUtil.Force force) {
        this.force = force;
    }

    /**
     * Prepare com.sigma.live.encoder with default parameters
     */
    public boolean prepareAudioEncoder() {
        return prepareAudioEncoder(bitRate, sampleRate, isStereo);
    }

    public void start() {
        presentTimeUs = System.nanoTime() / 1000;
        audioEncoder.start();
        running = true;
        Log.i(TAG, "AudioEncoder started");
    }

    public void stop() {
        running = false;
        if (audioEncoder != null) {
            audioEncoder.stop();
            audioEncoder.release();
            audioEncoder = null;
        }
        Log.i(TAG, "AudioEncoder stopped");
    }

    /**
     * Set custom PCM data.
     * Use it after prepareAudioEncoder(int sampleRate, int channel).
     * Used too with microphone.
     *
     * @param buffer PCM buffer
     * @param size   Min PCM buffer size
     */
    @Override
    public void inputPCMData(final byte[] buffer, final int size) {
        if (Build.VERSION.SDK_INT >= 21) {
            getDataFromEncoderAPI21(buffer, size);
        } else {
            getDataFromEncoder(buffer, size);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void getDataFromEncoderAPI21(byte[] data, int size) {
        int inBufferIndex = audioEncoder.dequeueInputBuffer(-1);
        if (inBufferIndex >= 0) {
            ByteBuffer bb = audioEncoder.getInputBuffer(inBufferIndex);
            bb.put(data, 0, size);
            long pts = System.nanoTime() / 1000 - presentTimeUs;
            audioEncoder.queueInputBuffer(inBufferIndex, 0, size, pts, 0);
        }

        for (; ; ) {
            int outBufferIndex = audioEncoder.dequeueOutputBuffer(audioInfo, 0);
            if (outBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                getAacData.onAudioFormat(audioEncoder.getOutputFormat());
            } else if (outBufferIndex >= 0) {
                //This ByteBuffer is AAC
                ByteBuffer bb = audioEncoder.getOutputBuffer(outBufferIndex);
                getAacData.getAacData(bb, audioInfo);
                audioEncoder.releaseOutputBuffer(outBufferIndex, false);
            } else {
                try {
                    Thread.sleep(1);
                } catch (Exception ex) {
                }
                break;
            }
        }
    }

    private void getDataFromEncoder(byte[] data, int size) {
        ByteBuffer[] inputBuffers = audioEncoder.getInputBuffers();
        ByteBuffer[] outputBuffers = audioEncoder.getOutputBuffers();

        int inBufferIndex = audioEncoder.dequeueInputBuffer(-1);
        if (inBufferIndex >= 0) {
            ByteBuffer bb = inputBuffers[inBufferIndex];
            bb.clear();
            bb.put(data, 0, size);
            long pts = System.nanoTime() / 1000 - presentTimeUs;
            audioEncoder.queueInputBuffer(inBufferIndex, 0, size, pts, 0);
        }

        for (; ; ) {
            int outBufferIndex = audioEncoder.dequeueOutputBuffer(audioInfo, 0);
            if (outBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                getAacData.onAudioFormat(audioEncoder.getOutputFormat());
            } else if (outBufferIndex >= 0) {
                //This ByteBuffer is AAC
                ByteBuffer bb = outputBuffers[outBufferIndex];
                getAacData.getAacData(bb, audioInfo);
                audioEncoder.releaseOutputBuffer(outBufferIndex, false);
            } else {
                break;
            }
        }
    }

    private MediaCodecInfo chooseAudioEncoder(String mime) {
        List<MediaCodecInfo> mediaCodecInfoList = CodecUtil.getAllEncoders(mime);
        for (MediaCodecInfo mediaCodecInfo : mediaCodecInfoList) {
            String name = mediaCodecInfo.getName().toLowerCase();
            if (!name.contains("omx.google")) return mediaCodecInfo;
        }
        if (mediaCodecInfoList.size() > 0) return mediaCodecInfoList.get(0);
        else return null;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public boolean isRunning() {
        return running;
    }
}
