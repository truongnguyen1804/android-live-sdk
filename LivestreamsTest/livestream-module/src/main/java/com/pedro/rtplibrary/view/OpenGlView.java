package com.pedro.rtplibrary.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.RequiresApi;

import com.pedro.encoder.input.gl.SurfaceManager;
import com.pedro.encoder.input.gl.render.ManagerRender;
import com.pedro.encoder.input.gl.render.filters.BaseFilterRender;
import com.pedro.encoder.input.video.FpsLimiter;
import com.pedro.encoder.utils.gl.GlUtil;
import com.sigma.live.R;

/**
 * Created by pedro on 9/09/17.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class OpenGlView extends OpenGlViewBase {

    private ManagerRender managerRender = null;
    private boolean loadAA = false;
    private boolean videoEnable = true;

    private boolean AAEnabled = false;
    private boolean keepAspectRatio = false;
    private boolean isFlipHorizontal = false, isFlipVertical = false;

    private boolean surfaceCreated = false;

    private SurfaceListener surfaceListener;


    public OpenGlView(Context context) {
        super(context);
    }

    public OpenGlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.OpenGlView);
        try {
            keepAspectRatio = typedArray.getBoolean(R.styleable.OpenGlView_keepAspectRatio, false);
            AAEnabled = typedArray.getBoolean(R.styleable.OpenGlView_AAEnabled, false);
            ManagerRender.numFilters = typedArray.getInt(R.styleable.OpenGlView_numFilters, 1);
            isFlipHorizontal = typedArray.getBoolean(R.styleable.OpenGlView_isFlipHorizontal, false);
            isFlipVertical = typedArray.getBoolean(R.styleable.OpenGlView_isFlipVertical, false);
        } finally {
            typedArray.recycle();
        }
    }

    @Override
    public void init() {
        if (!initialized || managerRender==null) managerRender = new ManagerRender();
        managerRender.setCameraFlip(isFlipHorizontal, isFlipVertical);
        initialized = true;
    }

    public void setImageThumbnail(Bitmap bitmap) {
        if (managerRender != null) {
            managerRender.setImageThumbnail(bitmap);
        }
    }

    public void setSurfaceListener(SurfaceListener listener){
        this.surfaceListener = listener;
    }

    @Override
    public SurfaceTexture getSurfaceTexture() {
        return managerRender.getSurfaceTexture();
    }

    @Override
    public Surface getSurface() {
        return managerRender.getSurface();
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
        managerRender.setCameraRotation(rotation);
    }

    public boolean isKeepAspectRatio() {
        return keepAspectRatio;
    }

    public void setKeepAspectRatio(boolean keepAspectRatio) {
        this.keepAspectRatio = keepAspectRatio;
    }

    public void setCameraFlip(boolean isFlipHorizontal, boolean isFlipVertical) {
        managerRender.setCameraFlip(isFlipHorizontal, isFlipVertical);
    }

    public void setVideoEnable(boolean enable) {
        videoEnable = enable;
    }

    public boolean isVideoEnable() {
        return videoEnable;
    }

    @Override
    public boolean isAAEnabled() {
        return managerRender != null && managerRender.isAAEnabled();
    }

    @Override
    public void run() {
        releaseSurfaceManager();
        //truong custom
        if (getHolder() == null) {
            return;
        }
        if (getHolder().getSurface() == null) {
            return;
        }
        if (!surfaceCreated) {
            return;
        }

        surfaceManager = new SurfaceManager(getHolder().getSurface());
        surfaceManager.makeCurrent();
        managerRender.setStreamSize(encoderWidth, encoderHeight);
        managerRender.initGl(getContext());
        managerRender.getSurfaceTexture().setOnFrameAvailableListener(this);
        semaphore.release();
        FpsLimiter limiter = new FpsLimiter();
        try {
            while (running) {
                if (limiter.limitFPS(25)) {
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
                    managerRender.updateFrame();
                    managerRender.drawOffScreen();
                    managerRender.drawScreen(true, previewWidth, previewHeight, keepAspectRatio);
                    surfaceManager.swapBuffer();
                    if (takePhotoCallback != null) {
                        Bitmap bitmap = GlUtil.getBitmap(previewWidth, previewHeight, encoderWidth, encoderHeight);
                        takePhotoCallback.onTakePhoto(bitmap);
                        takePhotoCallback = null;
                    }
                }
                synchronized (sync) {
                    if (surfaceManagerEncoder != null) {
                        surfaceManagerEncoder.makeCurrent();

                        managerRender.drawScreen(videoEnable, encoderWidth, encoderHeight, false);
//                        long ts = managerRender.getSurfaceTexture().getTimestamp();
//                        surfaceManagerEncoder.setPresentationTime(ts);
                        surfaceManagerEncoder.swapBuffer();
                    }
                }
                if (!filterQueue.isEmpty()) {
                    Filter filter = filterQueue.take();
                    managerRender.setFilter(filter.getPosition(), filter.getBaseFilterRender());
                } else if (loadAA) {
                    managerRender.enableAA(AAEnabled);
                    loadAA = false;
                }
            }
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        } finally {
            managerRender.release();
            releaseSurfaceManager();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
        surfaceCreated = true;
        if (surfaceListener!=null)
            surfaceListener.onCreated();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        surfaceCreated = false;
        if (surfaceListener!=null)
            surfaceListener.onDestroyed();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        super.surfaceChanged(holder, format, width, height);
        if (surfaceListener!=null)
            surfaceListener.onChanged();
    }

    public interface SurfaceListener {
        void onCreated();

        void onDestroyed();

        void onChanged();

        void onError(Exception exception);

        void onSurfaceInvalid(Exception exception);
    }


}