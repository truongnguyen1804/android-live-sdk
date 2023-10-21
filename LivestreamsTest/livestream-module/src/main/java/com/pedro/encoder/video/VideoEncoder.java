package com.pedro.encoder.video;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.pedro.encoder.input.video.FpsLimiter;
import com.pedro.encoder.input.video.Frame;
import com.pedro.encoder.input.video.GetCameraData;
import com.pedro.encoder.utils.CodecUtil;
import com.pedro.encoder.utils.yuv.YUVUtil;
import com.sigma.FullLog;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by pedro on 19/01/17.
 * This class need use same resolution, fps and imageFormat that Camera1ApiManagerGl
 */

public class VideoEncoder implements GetCameraData {
    private static final int TIMEOUT = 0;

    private void sleep() {
        try {
            Thread.currentThread().sleep(1);
        } catch (Exception ex) {
        }
    }

    private String TAG = "VideoEncoder";
    private MediaCodec videoEncoder;
    private Thread thread;
    private GetVideoData getVideoData;
    private MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();
    private long presentTimeUs;
    private volatile boolean running = false;
    private boolean spsPpsSetted = false;
    private boolean hardwareRotation = false;

    //surface to buffer com.sigma.live.encoder
    private Surface inputSurface;
    //buffer to buffer, 3 queue to optimize frames on rotation
    private BlockingQueue<Frame> queue = new LinkedBlockingQueue<>(80);
    private final Object sync = new Object();

    //default parameters for com.sigma.live.encoder
    private CodecUtil.Force force = CodecUtil.Force.FIRST_COMPATIBLE_FOUND;
    private int width = 640;
    private int height = 480;
    private int fps = 30;
    private int bitRate = 1200 * 1024; //in kbps
    private int rotation = 90;
    private int iFrameInterval = 2;
    private FormatVideoEncoder formatVideoEncoder = FormatVideoEncoder.YUV420Dynamical;
    //    private FormatVideoEncoder formatVideoEncoder = FormatVideoEncoder.SURFACE;
    //for disable video
    private boolean sendBlackImage = false;
    private byte[] blackImage;
    private FpsLimiter fpsLimiter = new FpsLimiter();
    private String type = CodecUtil.H264_MIME;

    public VideoEncoder(GetVideoData getVideoData) {
        this.getVideoData = getVideoData;
    }

    /**
     * Prepare com.sigma.live.encoder with custom parameters
     */
    public boolean prepareVideoEncoder(int width, int height, int fps, int bitRate, int rotation,
                                       boolean hardwareRotation, int iFrameInterval, FormatVideoEncoder formatVideoEncoder) {
        this.width = width;
        this.height = height;

        this.fps = fps;
        this.bitRate = bitRate;
        this.rotation = rotation;
        this.hardwareRotation = hardwareRotation;
        this.formatVideoEncoder = formatVideoEncoder;
        MediaCodecInfo encoder = chooseVideoEncoder(type);
        FullLog.LogD("prepareVideoEncoder");
        try {
            if (encoder != null) {
                videoEncoder = MediaCodec.createByCodecName(encoder.getName());
                if (this.formatVideoEncoder == FormatVideoEncoder.YUV420Dynamical) {
                    this.formatVideoEncoder = chooseColorDynamically(encoder);
                    if (this.formatVideoEncoder == null) {
                        Log.e(TAG, "YUV420 dynamical choose failed");
                        return false;
                    }
                }
            } else {
                Log.e(TAG, "Valid com.sigma.live.encoder not found");
                return false;
            }
            MediaFormat videoFormat;
            //if you dont use mediacodec rotation you need swap width and height in rotation 90 or 270
            // for correct encoding resolution
            String resolution;
            if (!hardwareRotation && (rotation == 90 || rotation == 270)) {
                resolution = height + "x1" + width;
                videoFormat = MediaFormat.createVideoFormat(type, height, width);
            } else {
                resolution = width + "x" + height;
                videoFormat = MediaFormat.createVideoFormat(type, width, height);
            }

            Log.i(TAG, "Prepare video info: " + this.formatVideoEncoder.name() + ", " + resolution);
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    this.formatVideoEncoder.getFormatCodec());
            videoFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval);
//            videoFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
//            videoFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel3);
            if (hardwareRotation) {
                videoFormat.setInteger("rotation-degrees"/*MediaFormat.KEY_ROTATION*/, rotation);
            }
            videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            running = false;
            if (formatVideoEncoder == FormatVideoEncoder.SURFACE
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                inputSurface = videoEncoder.createInputSurface();

            }
            prepareBlackImage();
            return true;
        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "Create VideoEncoder failed.", e);
            return false;
        }
    }

    private FormatVideoEncoder chooseColorDynamically(MediaCodecInfo mediaCodecInfo) {
        for (int color : mediaCodecInfo.getCapabilitiesForType(CodecUtil.H264_MIME).colorFormats) {
            if (color == FormatVideoEncoder.YUV420PLANAR.getFormatCodec()) {
                return FormatVideoEncoder.YUV420PLANAR;
            } else if (color == FormatVideoEncoder.YUV420SEMIPLANAR.getFormatCodec()) {
                return FormatVideoEncoder.YUV420SEMIPLANAR;
            }
        }
        return null;
    }

    /**
     * Prepare com.sigma.live.encoder with default parameters
     */
    public boolean prepareVideoEncoder() {
        return prepareVideoEncoder(width, height, fps, bitRate, rotation, false, iFrameInterval,
                formatVideoEncoder);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void setVideoBitrateOnFly(int bitrate) {
        if (isRunning()) {
            this.bitRate = bitrate;
            Bundle bundle = new Bundle();
            bundle.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bitrate);
            try {
                videoEncoder.setParameters(bundle);
            } catch (IllegalStateException e) {
                Log.e(TAG, "com.sigma.live.encoder need be running", e);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void forceSyncFrame() {
        if (isRunning()) {
            Bundle bundle = new Bundle();
            bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
            try {
                videoEncoder.setParameters(bundle);
            } catch (IllegalStateException e) {
                Log.e(TAG, "com.sigma.live.encoder need be running", e);
            }
        }
    }

    public void setForce(CodecUtil.Force force) {
        this.force = force;
    }

    public Surface getInputSurface() {
        return inputSurface;
    }

    public void setInputSurface(Surface inputSurface) {
        this.inputSurface = inputSurface;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isHardwareRotation() {
        return hardwareRotation;
    }

    public boolean isRunning() {
        return running;
    }

    public int getRotation() {
        return rotation;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public int getFps() {
        return fps;
    }

    public void start() {
        start(true);
    }

    public int getBitRate() {
        return bitRate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void start(boolean resetTs) {
        synchronized (sync) {
            spsPpsSetted = false;
            if (resetTs) presentTimeUs = System.nanoTime() / 1000;
            videoEncoder.start();
            //surface to buffer

            if (formatVideoEncoder == FormatVideoEncoder.SURFACE
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

                FullLog.LogD("surface to buffer");
                thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= 21) {
                            getDataFromSurfaceAPI21();
                        } else {
                            getDataFromSurface();
                        }
                    }
                });
                //buffer to buffer
            } else {
                if (!(rotation == 0 || rotation == 90 || rotation == 180 || rotation == 270)) {
                    throw new RuntimeException("rotation value unsupported, select value 0, 90, 180 or 270[" + rotation + "]");
                }
                FullLog.LogD("buffer to buffer");
                thread = new Thread(new Runnable() {
                    @Override
                    public void run() {

                        YUVUtil.preAllocateBuffers(width * height * 3 / 2);
                        while (running && !Thread.interrupted()) {
                            try {
                                Frame frame = queue.take();
//                                if (fpsLimiter.limitFPS(fps)) continue;
                                byte[] buffer = frame.getBuffer();
                                boolean isYV12 = frame.getFormat() == ImageFormat.YV12;
                                if (!hardwareRotation) {
                                    int orientation =
                                            frame.isFlip() ? frame.getOrientation() + 180 : frame.getOrientation();
                                    if (orientation >= 360) orientation -= 360;
                                    if (orientation != 0)
                                        buffer = isYV12 ? YUVUtil.rotateYV12(buffer, width, height, orientation)
                                                : YUVUtil.rotateNV21(buffer, width, height, orientation);
                                }
                                buffer = (sendBlackImage) ? blackImage : isYV12 ? YUVUtil.YV12toYUV420byColor(buffer, width, height, formatVideoEncoder) : YUVUtil.NV21toYUV420byColor(buffer, width, height, formatVideoEncoder);

                                if (Thread.currentThread().isInterrupted()) return;
                                if (Build.VERSION.SDK_INT >= 21) {
                                    getDataFromEncoderAPI21(buffer);
                                } else {
                                    getDataFromEncoder(buffer);
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                });
            }
            running = true;
            thread.start();
        }
    }

    public void stop() {
        synchronized (sync) {
            running = false;
            if (thread != null) {
                thread.interrupt();
                try {
                    thread.join(100);
                } catch (InterruptedException e) {
                    thread.interrupt();
                }
                thread = null;
            }
            if (videoEncoder != null) {
                videoEncoder.flush();
                videoEncoder.stop();
                videoEncoder.release();
                videoEncoder = null;
            }
            queue.clear();
            fpsLimiter.reset();
            spsPpsSetted = false;
            inputSurface = null;
        }
    }

    public void reset() {
        synchronized (sync) {
            stop();
            prepareVideoEncoder(width, height, fps, bitRate, rotation, hardwareRotation, iFrameInterval,
                    formatVideoEncoder);
            start(false);
        }
    }

    @Override
    public void inputYUVData(Frame frame) {
        synchronized (sync) {
            if (running) {
                try {
                    FullLog.LogD("checkLogFrameInput " + frame.getOrientation());
                    queue.add(frame);
                } catch (IllegalStateException e) {
                    Log.i(TAG, "frame discarded");
                }
            }
        }
    }

    private void sendSPSandPPS(MediaFormat mediaFormat) {
        //H265
        if (type.equals(CodecUtil.H265_MIME)) {
            List<ByteBuffer> byteBufferList =
                    extractVpsSpsPpsFromH265(mediaFormat.getByteBuffer("csd-0"));
            getVideoData.onSpsPpsVps(byteBufferList.get(1), byteBufferList.get(2), byteBufferList.get(0));

            //H264
        } else {
            getVideoData.onSpsPps(mediaFormat.getByteBuffer("csd-0"), mediaFormat.getByteBuffer("csd-1"));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void getDataFromSurfaceAPI21() {
        while (!Thread.interrupted() && running) {
            for (; running; ) {
                int outBufferIndex = -3;
                try {
                    outBufferIndex = videoEncoder.dequeueOutputBuffer(videoInfo, TIMEOUT);
                } catch (RuntimeException exception) {
                    FullLog.LogE("getDataFromSurfaceAPI21"+" -- "+exception.getMessage());
                }

                if (outBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    FullLog.LogD("INFO_OUTPUT_FORMAT_CHANGED");
                    MediaFormat mediaFormat = videoEncoder.getOutputFormat();
                    getVideoData.onVideoFormat(mediaFormat);
                    sendSPSandPPS(mediaFormat);
                    spsPpsSetted = true;
                }
                else if (outBufferIndex >= 0) {
                    /*Truong edit*/
//                    int inBufferIndex = videoEncoder.dequeueInputBuffer(-1);
//
//                    ByteBuffer inputBb = videoEncoder.getInputBuffer(inBufferIndex);
//                    byte[] bytes = rotateImageByByteBuffer(inputBb, 1920, 1080, 90);
//                    inputBb.clear();
//                    inputBb.put(bytes);
                    /*end*/

                    //This ByteBuffer is H264
                    //readonly
                    ByteBuffer bb = videoEncoder.getOutputBuffer(outBufferIndex);
                    if ((videoInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {

                        if (!spsPpsSetted) {

                            Pair<ByteBuffer, ByteBuffer> buffers =
                                    decodeSpsPpsFromBuffer(bb.duplicate(), videoInfo.size);
                            if (buffers != null) {

                                getVideoData.onSpsPps(buffers.first, buffers.second);
                                spsPpsSetted = true;
                            }
                        }
                    }

                    videoInfo.presentationTimeUs = System.nanoTime() / 1000 - presentTimeUs;
                    getVideoData.getVideoData(bb, videoInfo);
                    videoEncoder.releaseOutputBuffer(outBufferIndex, false);

                } else {
                    sleep();
                    break;
                }
            }
        }
    }


    public static byte[] rotateImageByByteBuffer(ByteBuffer byteBuffer, int width, int height, int degrees) {
        // Step 1: Convert ByteBuffer to a byte array
        byte[] byteArray = new byte[byteBuffer.remaining()];
        byteBuffer.get(byteArray);

        // Step 2: Create a Bitmap from the byte array
        Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

        // Step 3: Rotate the Bitmap
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);

        // Step 4: Create a new ByteBuffer from the rotated Bitmap
        ByteBuffer rotatedByteBuffer = ByteBuffer.allocate(rotatedBitmap.getRowBytes() * rotatedBitmap.getHeight());
        rotatedBitmap.copyPixelsToBuffer(rotatedByteBuffer);

        return rotatedByteBuffer.array();
    }

    private void getDataFromSurface() {
        while (!Thread.interrupted() && running) {
            ByteBuffer[] outputBuffers = videoEncoder.getOutputBuffers();
            for (; running; ) {
                int outBufferIndex = videoEncoder.dequeueOutputBuffer(videoInfo, TIMEOUT);
                if (outBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat mediaFormat = videoEncoder.getOutputFormat();
                    getVideoData.onVideoFormat(mediaFormat);
                    sendSPSandPPS(mediaFormat);
                    spsPpsSetted = true;
                } else if (outBufferIndex >= 0) {
                    //This ByteBuffer is H264
                    ByteBuffer bb = outputBuffers[outBufferIndex];
                    if ((videoInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        if (!spsPpsSetted) {
                            Pair<ByteBuffer, ByteBuffer> buffers =
                                    decodeSpsPpsFromBuffer(bb.duplicate(), videoInfo.size);
                            if (buffers != null) {
                                getVideoData.onSpsPps(buffers.first, buffers.second);
                                spsPpsSetted = true;
                            }
                        }
                    }
                    videoInfo.presentationTimeUs = System.nanoTime() / 1000 - presentTimeUs;
                    getVideoData.getVideoData(bb, videoInfo);
                    videoEncoder.releaseOutputBuffer(outBufferIndex, false);
                } else {
                    sleep();
                    break;
                }
            }
        }
    }

    int last;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void getDataFromEncoderAPI21(byte[] buffer) {
        int inBufferIndex = videoEncoder.dequeueInputBuffer(-1);
        if (inBufferIndex >= 0) {
            ByteBuffer bb = videoEncoder.getInputBuffer(inBufferIndex);
            bb.put(buffer, 0, buffer.length);
            long pts = System.nanoTime() / 1000 - presentTimeUs;
            videoEncoder.queueInputBuffer(inBufferIndex, 0, buffer.length, pts, 0);
        }
        for (; running; ) {
            int outBufferIndex = videoEncoder.dequeueOutputBuffer(videoInfo, TIMEOUT);
            if (outBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat mediaFormat = videoEncoder.getOutputFormat();
                getVideoData.onVideoFormat(mediaFormat);
                sendSPSandPPS(mediaFormat);
                spsPpsSetted = true;
            } else if (outBufferIndex >= 0) {
                //This ByteBuffer is H264
                ByteBuffer bb = videoEncoder.getOutputBuffer(outBufferIndex);
                if ((videoInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (!spsPpsSetted) {
                        Pair<ByteBuffer, ByteBuffer> buffers =
                                decodeSpsPpsFromBuffer(bb.duplicate(), videoInfo.size);
                        if (buffers != null) {
                            getVideoData.onSpsPps(buffers.first, buffers.second);
                            spsPpsSetted = true;
                        }
                    }
                }
                videoInfo.presentationTimeUs = System.nanoTime() / 1000 - presentTimeUs;
                getVideoData.getVideoData(bb, videoInfo);
                videoEncoder.releaseOutputBuffer(outBufferIndex, false);
            } else {
                sleep();
                break;
            }
        }
    }

    private void getDataFromEncoder(byte[] buffer) {
        ByteBuffer[] inputBuffers = videoEncoder.getInputBuffers();
        ByteBuffer[] outputBuffers = videoEncoder.getOutputBuffers();

        int inBufferIndex = videoEncoder.dequeueInputBuffer(-1);
        if (inBufferIndex >= 0) {
            ByteBuffer bb = inputBuffers[inBufferIndex];
            bb.clear();
            bb.put(buffer, 0, buffer.length);
            long pts = System.nanoTime() / 1000 - presentTimeUs;
            videoEncoder.queueInputBuffer(inBufferIndex, 0, buffer.length, pts, 0);
        }

        for (; running; ) {
            int outBufferIndex = videoEncoder.dequeueOutputBuffer(videoInfo, TIMEOUT);
            if (outBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat mediaFormat = videoEncoder.getOutputFormat();
                getVideoData.onVideoFormat(mediaFormat);
                sendSPSandPPS(mediaFormat);
                spsPpsSetted = true;
            } else if (outBufferIndex >= 0) {
                //This ByteBuffer is H264
                ByteBuffer bb = outputBuffers[outBufferIndex];
                if ((videoInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (!spsPpsSetted) {
                        Pair<ByteBuffer, ByteBuffer> buffers =
                                decodeSpsPpsFromBuffer(bb.duplicate(), videoInfo.size);
                        if (buffers != null) {
                            getVideoData.onSpsPps(buffers.first, buffers.second);
                            spsPpsSetted = true;
                        }
                    }
                }
                videoInfo.presentationTimeUs = System.nanoTime() / 1000 - presentTimeUs;
                getVideoData.getVideoData(bb, videoInfo);
                videoEncoder.releaseOutputBuffer(outBufferIndex, false);
            } else {
                sleep();
                break;
            }
        }
    }

    /**
     * choose the video com.sigma.live.encoder by mime.
     */
    private MediaCodecInfo chooseVideoEncoder(String mime) {
        List<MediaCodecInfo> mediaCodecInfoList;
        if (force == CodecUtil.Force.HARDWARE) {
            mediaCodecInfoList = CodecUtil.getAllHardwareEncoders(mime);
        } else if (force == CodecUtil.Force.SOFTWARE) {
            mediaCodecInfoList = CodecUtil.getAllSoftwareEncoders(mime);
        } else {
            mediaCodecInfoList = CodecUtil.getAllEncoders(mime);
        }
        for (MediaCodecInfo mci : mediaCodecInfoList) {
            Log.i(TAG, String.format("VideoEncoder %s", mci.getName()));
            MediaCodecInfo.CodecCapabilities codecCapabilities = mci.getCapabilitiesForType(mime);
            for (int color : codecCapabilities.colorFormats) {
                Log.i(TAG, "Color supported: " + color);
                if (formatVideoEncoder == FormatVideoEncoder.SURFACE) {
                    if (color == FormatVideoEncoder.SURFACE.getFormatCodec()) return mci;
                } else {
                    //check if com.sigma.live.encoder support any yuv420 color
                    if (color == FormatVideoEncoder.YUV420PLANAR.getFormatCodec()
                            || color == FormatVideoEncoder.YUV420SEMIPLANAR.getFormatCodec()) {
                        return mci;
                    }
                }
            }
        }
        return null;
    }

    /*truong edit*/
    private void prepareBlackImage() {
        if (blackImage == null) {
            Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(b);
            canvas.drawColor(Color.BLACK);
            int x = b.getWidth();
            int y = b.getHeight();
            int[] data = new int[x * y];
            b.getPixels(data, 0, x, 0, 0, x, y);
            blackImage = YUVUtil.ARGBtoYUV420SemiPlanar(data, width, height);
        }

    }

    public void prepareImage(Bitmap bitmap) {
        Bitmap b = Bitmap.createScaledBitmap(bitmap, width, height, false);

//        Canvas canvas = new Canvas(b);
//        canvas.drawColor(Color.BLACK);
        int x = b.getWidth();
        int y = b.getHeight();
        int[] data = new int[x * y];
        b.getPixels(data, 0, x, 0, 0, x, y);
        blackImage = YUVUtil.ARGBtoYUV420SemiPlanar(data, width, height);
    }


    public void startSendBlackImage() {
        sendBlackImage = true;
        if (Build.VERSION.SDK_INT >= 19) {
            if (isRunning()) {
                Bundle bundle = new Bundle();
                bundle.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, 100 * 1024);
                try {
                    videoEncoder.setParameters(bundle);
                } catch (IllegalStateException e) {
                    Log.e(TAG, "com.sigma.live.encoder need be running", e);
                }
            }
        }
    }

    public void stopSendBlackImage() {
        sendBlackImage = false;
        if (Build.VERSION.SDK_INT >= 19) {
            setVideoBitrateOnFly(bitRate);
        }
    }

    /**
     * decode sps and pps if the com.sigma.live.encoder never call to MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
     */
    private Pair<ByteBuffer, ByteBuffer> decodeSpsPpsFromBuffer(ByteBuffer outputBuffer, int length) {
        byte[] mSPS = null, mPPS = null;
        byte[] csd = new byte[length];
        outputBuffer.get(csd, 0, length);
        int i = 0;
        int spsIndex = -1;
        int ppsIndex = -1;
        while (i < length - 4) {
            if (csd[i] == 0 && csd[i + 1] == 0 && csd[i + 2] == 0 && csd[i + 3] == 1) {
                if (spsIndex == -1) {
                    spsIndex = i;
                } else {
                    ppsIndex = i;
                    break;
                }
            }
            i++;
        }
        if (spsIndex != -1 && ppsIndex != -1) {
            mSPS = new byte[ppsIndex];
            System.arraycopy(csd, spsIndex, mSPS, 0, ppsIndex);
            mPPS = new byte[length - ppsIndex];
            System.arraycopy(csd, ppsIndex, mPPS, 0, length - ppsIndex);
        }
        if (mSPS != null && mPPS != null) {
            return new Pair<>(ByteBuffer.wrap(mSPS), ByteBuffer.wrap(mPPS));
        }
        return null;
    }

    /**
     * You need find 0 0 0 1 byte sequence that is the initiation of vps, sps and pps
     * buffers.
     *
     * @param csd0byteBuffer get in mediacodec case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
     * @return list with vps, sps and pps
     */
    private List<ByteBuffer> extractVpsSpsPpsFromH265(ByteBuffer csd0byteBuffer) {
        List<ByteBuffer> byteBufferList = new ArrayList<>();
        int vpsPosition = -1;
        int spsPosition = -1;
        int ppsPosition = -1;
        int contBufferInitiation = 0;
        byte[] csdArray = csd0byteBuffer.array();
        for (int i = 0; i < csdArray.length; i++) {
            if (contBufferInitiation == 3 && csdArray[i] == 1) {
                if (vpsPosition == -1) {
                    vpsPosition = i - 3;
                } else if (spsPosition == -1) {
                    spsPosition = i - 3;
                } else {
                    ppsPosition = i - 3;
                }
            }
            if (csdArray[i] == 0) {
                contBufferInitiation++;
            } else {
                contBufferInitiation = 0;
            }
        }
        byte[] vps = new byte[spsPosition];
        byte[] sps = new byte[ppsPosition - spsPosition];
        byte[] pps = new byte[csdArray.length - ppsPosition];
        for (int i = 0; i < csdArray.length; i++) {
            if (i < spsPosition) {
                vps[i] = csdArray[i];
            } else if (i < ppsPosition) {
                sps[i - spsPosition] = csdArray[i];
            } else {
                pps[i - ppsPosition] = csdArray[i];
            }
        }
        byteBufferList.add(ByteBuffer.wrap(vps));
        byteBufferList.add(ByteBuffer.wrap(sps));
        byteBufferList.add(ByteBuffer.wrap(pps));
        return byteBufferList;
    }
}