package com.pedro.rtplibrary.base;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.pedro.encoder.audio.AudioEncoder;
import com.pedro.encoder.audio.GetAacData;
import com.pedro.encoder.input.audio.GetMicrophoneData;
import com.pedro.encoder.input.audio.MicrophoneManager;
import com.pedro.encoder.input.video.CameraHelper;
import com.pedro.encoder.utils.CodecUtil;
import com.pedro.encoder.video.FormatVideoEncoder;
import com.pedro.encoder.video.GetVideoData;
import com.pedro.encoder.video.VideoEncoder;
import com.pedro.rtplibrary.view.GlInterface;
import com.pedro.rtplibrary.view.OffScreenGlThread;
import com.sigma.FullLog;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class SourceBase implements GetMicrophoneData, GetVideoData, GetAacData {
    protected Context context;
    protected VideoEncoder videoEncoder;
    private GlInterface glInterface;
    private boolean streaming = false;
    private boolean videoEnabled = true;
    private boolean onPreview = false;
    private RecordController recordController;
    private AudioEncoder audioEncoder;
    private MicrophoneManager microphoneManager;
    private int dpi = 320;


    private void init() {
        videoEncoder = new VideoEncoder(this);
        microphoneManager = new MicrophoneManager(this);
        audioEncoder = new AudioEncoder(this);
        recordController = new RecordController();
    }

    public abstract void setAuthorization(String user, String password);

    public boolean prepareVideo(int width, int height, int fps, int bitrate, int rotation, int dpi) {
        this.dpi = dpi;
        boolean result =
                videoEncoder.prepareVideoEncoder(width, height, fps, bitrate, rotation, true, 2,
                        FormatVideoEncoder.SURFACE);
        if (glInterface != null) {
            glInterface = new OffScreenGlThread(context);
            glInterface.init();
            glInterface.setEncoderSize(videoEncoder.getWidth(), videoEncoder.getHeight());
        }
        return result;
    }
    protected abstract void prepareAudioRtp(boolean isStereo, int sampleRate);

    public boolean prepareAudio(int bitrate, int sampleRate, boolean isStereo, boolean echoCanceler,
                                boolean noiseSuppressor, boolean autoGainControl) {
        microphoneManager.createMicrophone(sampleRate, isStereo, echoCanceler, noiseSuppressor, autoGainControl);
        prepareAudioRtp(isStereo, sampleRate);
        return audioEncoder.prepareAudioEncoder(bitrate, sampleRate, isStereo);
    }
    public boolean prepareVideo() {
        return prepareVideo(640, 480, 30, 1200 * 1024, 0, 320);
    }

    public boolean prepareAudio() {
        return prepareAudio(64 * 1024, 32000, true, false, false, false);
    }

    public void setForce(CodecUtil.Force forceVideo, CodecUtil.Force forceAudio) {
        videoEncoder.setForce(forceVideo);
        audioEncoder.setForce(forceAudio);
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void startRecord(final String path, RecordController.Listener listener)
            throws IOException {
        recordController.startRecord(path, listener);
        if (!streaming) {
            startEncoders();
        } else if (videoEncoder.isRunning()) {
            resetVideoEncoder();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void startRecord(final String path)
            throws IOException {
        startRecord(path, null);
    }

    /**
     * Stop record MP4 video started with @startRecord. If you don't call it file will be unreadable.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void stopRecord() {
        recordController.stopRecord();
        if (!streaming) stopStream();
    }


    public void startStream(String url) {
        streaming = true;
        if (!recordController.isRecording()) {
            startEncoders();
        } else {
            resetVideoEncoder();
        }
        startStreamRtp(url);
        onPreview = true;
    }

    private void startEncoders() {
        videoEncoder.start();
        audioEncoder.start();
        prepareGlView();
        microphoneManager.start();
        onPreview = true;
    }

    private void resetVideoEncoder() {
        if (glInterface != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            glInterface.removeMediaCodecSurface();
        }
        videoEncoder.reset();
        if (glInterface != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            glInterface.addMediaCodecSurface(videoEncoder.getInputSurface());
        }
    }

    private void prepareGlView() {
        if (glInterface != null && Build.VERSION.SDK_INT >= 18) {
            if (glInterface instanceof OffScreenGlThread) {
                glInterface = new OffScreenGlThread(context);
                glInterface.init();
                ((OffScreenGlThread) glInterface).setFps(videoEncoder.getFps());
            }
            if (videoEncoder.getRotation() == 90 || videoEncoder.getRotation() == 270) {
                glInterface.setEncoderSize(videoEncoder.getHeight(), videoEncoder.getWidth());
            } else {
                glInterface.setEncoderSize(videoEncoder.getWidth(), videoEncoder.getHeight());
            }

            FullLog.LogD("setRotation " + CameraHelper.getCameraOrientation(context));

//            glInterface.setRotation(0);

            /*truong edit*/
//            glInterface.setRotation(360 - CameraHelper.getCameraOrientation(context));
            FullLog.LogD("prepareGlView: "+ CameraHelper.getRotationGl(CameraHelper.getCameraOrientation(context)));
            glInterface.setRotation(CameraHelper.getRotationGl(CameraHelper.getCameraOrientation(context)));
            /*end*/
            glInterface.start();

            if (videoEncoder.getInputSurface() != null) {
                glInterface.addMediaCodecSurface(videoEncoder.getInputSurface());
//                glInterfaceDouble.addMediaCodecSurface(videoEncoder.getInputSurface());
            }
//            cameraManager.setSurfaceTexture(glInterfaceDouble.getSurfaceTexture());
        }
    }

    protected abstract void stopStreamRtp();

    /**
     * Stop stream started with @startStream.
     */
    public void stopStream() {
        if (streaming) {
            streaming = false;
            stopStreamRtp();
        }
        if (!recordController.isRecording()) {
            microphoneManager.stop();
            if (glInterface != null && Build.VERSION.SDK_INT >= 18) {
                glInterface.removeMediaCodecSurface();
                if (glInterface instanceof OffScreenGlThread) {
                    glInterface.stop();
                }
            }
            videoEncoder.stop();
            audioEncoder.stop();
            recordController.resetFormats();
        }
    }

    public void reTry(long delay) {
        resetVideoEncoder();

    }

    protected abstract void startStreamRtp(String url);

    @Override
    public void inputPCMData(byte[] buffer, int size) {

    }


    @Override
    public void onSpsPps(ByteBuffer sps, ByteBuffer pps) {

    }

    @Override
    public void onSpsPpsVps(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps) {

    }

    @Override
    public void getVideoData(ByteBuffer h264Buffer, MediaCodec.BufferInfo info) {

    }

    @Override
    public void onVideoFormat(MediaFormat mediaFormat) {

    }

    @Override
    public void getAacData(ByteBuffer aacBuffer, MediaCodec.BufferInfo info) {

    }

    @Override
    public void onAudioFormat(MediaFormat mediaFormat) {

    }
}
