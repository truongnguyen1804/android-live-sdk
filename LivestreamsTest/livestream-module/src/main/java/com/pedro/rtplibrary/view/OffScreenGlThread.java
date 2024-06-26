package com.pedro.rtplibrary.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.pedro.encoder.input.gl.SurfaceManager;
import com.pedro.encoder.input.gl.render.ManagerRender;
import com.pedro.encoder.input.gl.render.filters.BaseFilterRender;
import com.pedro.encoder.input.video.FpsLimiter;
import com.pedro.encoder.utils.gl.GlUtil;

import net.ossrs.rtmp.FpsCounter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * Created by pedro on 4/03/18.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class OffScreenGlThread
        implements GlInterface, Runnable, SurfaceTexture.OnFrameAvailableListener {

    private final Context context;
    private final Semaphore semaphore = new Semaphore(0);
    private final BlockingQueue<Filter> filterQueue = new LinkedBlockingQueue<>();
    private final Object sync = new Object();
    private Thread thread = null;
    private boolean frameAvailable = false;
    private boolean running = true;
    private boolean initialized = false;
    private SurfaceManager surfaceManager = null;
    private SurfaceManager surfaceManagerEncoder = null;
    private ManagerRender textureManager = null;
    private int encoderWidth, encoderHeight;
    private boolean loadAA = false;

    private boolean AAEnabled = false;
    private int fps = 30;
    private FpsLimiter fpsLimiter = new FpsLimiter();
    //used with camera
    private TakePhotoCallback takePhotoCallback;
    FpsCounter counter = new FpsCounter();

    protected int takePhotoRetryCount;
    private boolean videoEnable = true;

    public OffScreenGlThread(Context context) {
        this.context = context;
    }

    @Override
    public void init() {
        if (!initialized) textureManager = new ManagerRender();
        initialized = true;
    }

    public void setImageThumbnail(Bitmap bitmap){
        if (textureManager!=null){
            textureManager.setImageThumbnail(bitmap);
        }
    }
    @Override
    public void setEncoderSize(int width, int height) {
        this.encoderWidth = width;
        this.encoderHeight = height;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    @Override
    public SurfaceTexture getSurfaceTexture() {
        return textureManager.getSurfaceTexture();
    }

    @Override
    public Surface getSurface() {
        return textureManager.getSurface();
    }

    @Override
    public void addMediaCodecSurface(Surface surface) {
        synchronized (sync) {
            surfaceManagerEncoder = new SurfaceManager(surface, surfaceManager);
        }
    }

    @Override
    public void removeMediaCodecSurface() {
        synchronized (sync) {
            if (surfaceManagerEncoder != null) {
                surfaceManagerEncoder.release();
                surfaceManagerEncoder = null;
            }
        }
    }

    @Override
    public void takePhoto(TakePhotoCallback takePhotoCallback) {
        this.takePhotoCallback = takePhotoCallback;
        takePhotoRetryCount = 0;
    }

    @Override
    public void setFilter(int filterPosition, BaseFilterRender baseFilterRender) {
        filterQueue.add(new Filter(filterPosition, baseFilterRender));
    }

    @Override
    public void setFilter(BaseFilterRender baseFilterRender) {
        setFilter(0, baseFilterRender);
    }

    @Override
    public void enableAA(boolean AAEnabled) {
        this.AAEnabled = AAEnabled;
        loadAA = true;
    }

    @Override
    public void setRotation(int rotation) {
        textureManager.setCameraRotation(rotation);
    }

    @Override
    public boolean isAAEnabled() {
        return textureManager != null && textureManager.isAAEnabled();
    }

    @Override
    public void start() {
        synchronized (sync) {
            thread = new Thread(this);
            running = true;
            thread.start();
            semaphore.acquireUninterruptibly();
        }
    }

    @Override
    public void stop() {
        synchronized (sync) {
            if (thread != null) {
                thread.interrupt();
                try {
                    thread.join(100);
                } catch (InterruptedException e) {
                    thread.interrupt();
                }
                thread = null;
            }
            running = false;
            fpsLimiter.reset();
        }
    }

    private void releaseSurfaceManager() {
        if (surfaceManager != null) {
            surfaceManager.release();
            surfaceManager = null;
        }
    }

    @Override
    public void run() {
        releaseSurfaceManager();
        surfaceManager = new SurfaceManager();
        surfaceManager.makeCurrent();
        textureManager.setStreamSize(encoderWidth, encoderHeight);
        textureManager.initGl(context);
        textureManager.getSurfaceTexture().setOnFrameAvailableListener(this);
        semaphore.release();
        try {
            while (running) {
                if (fpsLimiter.limitFPS(fps)) {
                    try {
                        Thread.sleep(1);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    continue;
                }
                if (frameAvailable) {
                    frameAvailable = false;
                    surfaceManager.makeCurrent();
                    textureManager.updateFrame();
                    textureManager.drawOffScreen();
                    textureManager.drawScreen(true, encoderWidth, encoderHeight, false);
                    surfaceManager.swapBuffer();
                }
                synchronized (sync) {
                    if (surfaceManagerEncoder != null) {
                        surfaceManagerEncoder.makeCurrent();
                        textureManager.drawScreen(videoEnable, encoderWidth, encoderHeight, false);
//                        long ts = textureManager.getSurfaceTexture().getTimestamp();
                        //surfaceManagerEncoder.setPresentationTime(ts);
                        surfaceManagerEncoder.swapBuffer();
                        if (takePhotoCallback != null) {
                            Bitmap bitmap = GlUtil.getBitmap(encoderWidth, encoderHeight, encoderWidth, encoderHeight);
                            takePhotoCallback.onTakePhoto(bitmap);
                            takePhotoCallback = null;
                        }
                    }
                }
                if (!filterQueue.isEmpty()) {
                    Filter filter = filterQueue.take();
                    textureManager.setFilter(filter.getPosition(), filter.getBaseFilterRender());
                } else if (loadAA) {
                    textureManager.enableAA(AAEnabled);
                    loadAA = false;
                }
            }
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        } finally {
            textureManager.release();
            releaseSurfaceManager();
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (sync) {
            frameAvailable = true;
            sync.notifyAll();
        }
    }

    public void setVideoEnable(boolean enable) {
        videoEnable = enable;
    }

    public boolean isVideoEnable() {
        return videoEnable;
    }
}
