package com.pedro.encoder.input.video;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by pedro on 4/03/17.
 * <p>
 * <p>
 * Class for use surfaceEncoder to buffer com.sigma.live.encoder.
 * Advantage = you can use all resolutions.
 * Disadvantages = you cant control fps of the stream, because you cant know when the inputSurface
 * was renderer.
 * <p>
 * Note: you can use opengl for surfaceEncoder to buffer com.sigma.live.encoder on devices 21 < API > 16:
 * https://github.com/google/grafika
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2ApiManager extends CameraDevice.StateCallback {

    private final String TAG = "Camera2ApiManager";

    private CameraDevice cameraDevice;
    private SurfaceView surfaceView;
    private TextureView textureView;
    private Surface surfaceEncoder; //input surfaceEncoder from videoEncoder
    private CameraManager cameraManager;
    private Handler cameraHandler;
    private CameraCaptureSession cameraCaptureSession;
    private boolean prepared = false;
    private int cameraId = -1;
    private Surface preview;
    private boolean isOpenGl = false;
    private boolean isFrontCamera = false;
    private CameraCharacteristics cameraCharacteristics;
    private CaptureRequest.Builder builderPreview;
    private CaptureRequest.Builder builderInputSurface;
    private float fingerSpacing = 0;
    private float zoomLevel = 1.0f;
    private boolean lanternEnable = false;
    private FaceDetectorCallback faceDetectorCallback;
    private final CameraCaptureSession.CaptureCallback cb =
            new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
                    if (faceDetectorCallback != null) {
                        faceDetectorCallback.onGetFaces(faces);
                    }
                }
            };
    private boolean faceDetectionEnabled = false;
    private int faceDetectionMode;

    public Camera2ApiManager(Context context) {
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    public void prepareCamera(SurfaceView surfaceView, Surface surface) {
        this.surfaceView = surfaceView;
        this.surfaceEncoder = surface;
        prepared = true;
        isOpenGl = false;
    }

    public void prepareCamera(TextureView textureView, Surface surface) {
        this.textureView = textureView;
        this.surfaceEncoder = surface;
        prepared = true;
        isOpenGl = false;
    }

    public void prepareCamera(Surface surface) {
        this.surfaceEncoder = surface;
        prepared = true;
        isOpenGl = false;
    }

    public void prepareCamera(SurfaceTexture surfaceTexture, int width, int height) {
        surfaceTexture.setDefaultBufferSize(width, height);
        this.surfaceEncoder = new Surface(surfaceTexture);
        prepared = true;
        isOpenGl = true;
    }

    public boolean isPrepared() {
        return prepared;
    }

    private void startPreview(CameraDevice cameraDevice) {
        try {
            List<Surface> listSurfaces = new ArrayList<>();
            preview = addPreviewSurface();
            if (preview != null) {
                listSurfaces.add(preview);
            }
            if (surfaceEncoder != null) {
                listSurfaces.add(surfaceEncoder);
            }
            cameraDevice.createCaptureSession(listSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Camera2ApiManager.this.cameraCaptureSession = cameraCaptureSession;
                    try {
                        if (surfaceView != null || textureView != null) {
                            cameraCaptureSession.setRepeatingBurst(
                                    Arrays.asList(drawSurface(preview), drawSurface(surfaceEncoder)),
                                    faceDetectionEnabled ? cb : null, cameraHandler);
                        } else {
                            cameraCaptureSession.setRepeatingBurst(
                                    Collections.singletonList(drawSurface(surfaceEncoder)),
                                    faceDetectionEnabled ? cb : null, cameraHandler);
                        }
                        Log.i(TAG, "Camera configured");
                    } catch (CameraAccessException | NullPointerException e) {
                        Log.e(TAG, "Error", e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    cameraCaptureSession.close();
                    Log.e(TAG, "Configuration failed");
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error", e);
        }
    }

    private Surface addPreviewSurface() {
        Surface surface = null;
        if (surfaceView != null) {
            surface = surfaceView.getHolder().getSurface();
        } else if (textureView != null) {
            final SurfaceTexture texture = textureView.getSurfaceTexture();
            surface = new Surface(texture);
        }
        return surface;
    }

    private CaptureRequest drawSurface(Surface surface) {
        try {
            builderInputSurface = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builderInputSurface.addTarget(surface);
            return builderInputSurface.build();
        } catch (CameraAccessException | IllegalStateException e) {
            Log.e(TAG, "Error", e);
            return null;
        }
    }

    public int getLevelSupported() {
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics("0");
            return cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        } catch (CameraAccessException | IllegalStateException e) {
            Log.e(TAG, "Error", e);
            return -1;
        }
    }

    public void openCamera() {
        openCameraBack();
    }

    public void openCameraBack() {
        openCameraFacing(CameraHelper.Facing.BACK);
    }

    public void openCameraFront() {
        openCameraFacing(CameraHelper.Facing.FRONT);
    }

    public void openLastCamera() {
        if (cameraId == -1) {
            openCameraBack();
        } else {
            openCameraId(cameraId);
        }
    }

    public Size[] getCameraResolutionsBack() {
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics("0");
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                    != CameraCharacteristics.LENS_FACING_BACK) {
                cameraCharacteristics = cameraManager.getCameraCharacteristics("1");
            }
            StreamConfigurationMap streamConfigurationMap =
                    cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            return streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error", e);
            return new Size[0];
        }
    }

    public Size[] getCameraResolutionsFront() {
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics("0");
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                    != CameraCharacteristics.LENS_FACING_FRONT) {
                cameraCharacteristics = cameraManager.getCameraCharacteristics("1");
            }
            StreamConfigurationMap streamConfigurationMap =
                    cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            return streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error", e);
            return new Size[0];
        }
    }

    public CameraCharacteristics getCameraCharacteristics() {
        return cameraCharacteristics;
    }

    /**
     * Select camera facing
     *
     * @param cameraFacing - CameraCharacteristics.LENS_FACING_FRONT, CameraCharacteristics.LENS_FACING_BACK,
     *                     CameraCharacteristics.LENS_FACING_EXTERNAL
     */
    public void openCameraFacing(CameraHelper.Facing cameraFacing) {
        int facing = cameraFacing == CameraHelper.Facing.BACK ? CameraMetadata.LENS_FACING_BACK
                : CameraMetadata.LENS_FACING_FRONT;
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics("0");
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == facing) {
                openCameraId(0);
            } else {
                openCameraId(cameraManager.getCameraIdList().length - 1);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error", e);
        }
    }

    public boolean isLanternSupported() {
        return (cameraCharacteristics != null ? cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) : false);
    }

    public boolean isLanternEnabled() {
        return lanternEnable;
    }

    /**
     * @required: <uses-permission android:name="android.permission.FLASHLIGHT"/>
     */
    public void enableLantern() throws Exception {
        if ((cameraCharacteristics != null) && cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
            if (builderInputSurface != null) {
                try {
                    builderInputSurface.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                    cameraCaptureSession.setRepeatingRequest(builderInputSurface.build(), faceDetectionEnabled ? cb : null, null);
                    lanternEnable = true;
                } catch (CameraAccessException | IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Log.e(TAG, "Lantern unsupported");
            throw new Exception("Lantern unsupported");
        }
    }

    /**
     * @required: <uses-permission android:name="android.permission.FLASHLIGHT"/>
     */
    public void disableLantern() {
        if ((cameraCharacteristics != null) && cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
            if (builderInputSurface != null) {
                try {
                    builderInputSurface.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                    cameraCaptureSession.setRepeatingRequest(builderInputSurface.build(), faceDetectionEnabled ? cb : null, null);
                    lanternEnable = false;
                } catch (CameraAccessException | IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void enableFaceDetection(FaceDetectorCallback faceDetectorCallback) {
        int[] fd = cameraCharacteristics.get(
                CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
        int maxFD = cameraCharacteristics.get(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT);
        if (fd.length > 0) {
            List<Integer> fdList = new ArrayList<>();
            for (int FaceD : fd) {
                fdList.add(FaceD);
            }
            if (maxFD > 0) {
                this.faceDetectorCallback = faceDetectorCallback;
                faceDetectionEnabled = true;
                faceDetectionMode = Collections.max(fdList);
                if (builderPreview != null) setFaceDetect(builderPreview, faceDetectionMode);
                setFaceDetect(builderInputSurface, faceDetectionMode);
                prepareFaceDetectionCallback();
            } else {
                Log.e(TAG, "No face detection");
            }
        } else {
            Log.e(TAG, "No face detection");
        }
    }

    public void disableFaceDetection() {
        if (faceDetectionEnabled) {
            faceDetectorCallback = null;
            faceDetectionEnabled = false;
            faceDetectionMode = 0;
            prepareFaceDetectionCallback();
        }
    }

    public boolean isFaceDetectionEnabled() {
        return faceDetectorCallback != null;
    }

    private void setFaceDetect(CaptureRequest.Builder requestBuilder, int faceDetectMode) {
        if (faceDetectionEnabled) {
            requestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, faceDetectMode);
        }
    }

    private void prepareFaceDetectionCallback() {
        try {
            cameraCaptureSession.stopRepeating();
            if (builderPreview != null) {
                cameraCaptureSession.setRepeatingRequest(builderPreview.build(),
                        faceDetectionEnabled ? cb : null, null);
            }
            cameraCaptureSession.setRepeatingRequest(builderInputSurface.build(),
                    faceDetectionEnabled ? cb : null, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error", e);
        }
    }

    @SuppressLint("MissingPermission")
    public void openCameraId(Integer cameraId) {
        this.cameraId = cameraId;
        if (prepared) {
            HandlerThread cameraHandlerThread = new HandlerThread(TAG + " Id = " + cameraId);
            cameraHandlerThread.start();
            cameraHandler = new Handler(cameraHandlerThread.getLooper());
            try {
                cameraManager.openCamera(cameraId.toString(), this, cameraHandler);
                cameraCharacteristics = cameraManager.getCameraCharacteristics(Integer.toString(cameraId));
                isFrontCamera =
                        (LENS_FACING_FRONT == cameraCharacteristics.get(CameraCharacteristics.LENS_FACING));
            } catch (CameraAccessException | SecurityException e) {
                Log.e(TAG, "Error", e);
            }
        } else {
            Log.e(TAG, "Camera2ApiManager need be prepared, Camera2ApiManager not enabled");
        }
    }

    public void switchCamera() {
        if (cameraDevice != null) {
            int cameraId = Integer.parseInt(cameraDevice.getId()) == 1 ? 0 : 1;
            closeCamera(false);
            prepared = true;
            openCameraId(cameraId);
        }
    }

    public float getMaxZoom() {
        return (cameraCharacteristics != null ? cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) : 1);
    }

    public Float getZoom() {
        return zoomLevel;
    }

    public void setZoom(MotionEvent event) {
        float currentFingerSpacing;

        if (event.getPointerCount() > 1) {
            // Multi touch logic
            currentFingerSpacing = CameraHelper.getFingerSpacing(event);
            if (fingerSpacing != 0) {
                if (currentFingerSpacing > fingerSpacing && getMaxZoom() > zoomLevel) {
                    zoomLevel += 0.1f;
                } else if (currentFingerSpacing < fingerSpacing && zoomLevel > 1) {
                    zoomLevel -= 0.1f;
                }
                setZoom(zoomLevel);
            }
            fingerSpacing = currentFingerSpacing;
        }
    }

    public void setZoom(Float level) {
        try {
            float maxZoom = getMaxZoom();
            Rect m = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

            if ((level <= maxZoom) && (level >= 1)) {
                zoomLevel = level;
                int minW = (int) (m.width() / (maxZoom * 10));
                int minH = (int) (m.height() / (maxZoom * 10));
                int difW = m.width() - minW;
                int difH = m.height() - minH;
                int cropW = (int) (difW / 10 * level);
                int cropH = (int) (difH / 10 * level);
                cropW -= cropW & 3;
                cropH -= cropH & 3;
                Rect zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);
                if (builderPreview != null)
                    builderPreview.set(CaptureRequest.SCALER_CROP_REGION, zoom);
                builderInputSurface.set(CaptureRequest.SCALER_CROP_REGION, zoom);

                if (builderPreview != null) {
                    cameraCaptureSession.setRepeatingRequest(builderPreview.build(),
                            faceDetectionEnabled ? cb : null, null);
                }
                cameraCaptureSession.setRepeatingRequest(builderInputSurface.build(),
                        faceDetectionEnabled ? cb : null, null);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error", e);
        }
    }

    public boolean isFrontCamera() {
        return isFrontCamera;
    }

    private void resetCameraValues() {
        lanternEnable = false;
        zoomLevel = 1.0f;
    }

    public void closeCamera(boolean reOpen) {
        if (reOpen) {
            try {
                cameraCaptureSession.stopRepeating();
                if (surfaceView != null || textureView != null) {
                    cameraCaptureSession.setRepeatingBurst(Collections.singletonList(drawSurface(preview)),
                            null, cameraHandler);
                } else if (surfaceEncoder != null && isOpenGl) {
                    cameraCaptureSession.setRepeatingBurst(
                            Collections.singletonList(drawSurface(surfaceEncoder)), null, cameraHandler);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error", e);
            }
        } else {
            resetCameraValues();

            cameraCharacteristics = null;
            if (cameraCaptureSession != null) {
                cameraCaptureSession.close();
                cameraCaptureSession = null;
            }
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (cameraHandler != null) {
                cameraHandler.getLooper().quitSafely();
                cameraHandler = null;
            }
            prepared = false;
            builderPreview = null;
            builderInputSurface = null;
        }
    }

    @Override
    public void onOpened(@NonNull CameraDevice cameraDevice) {
        this.cameraDevice = cameraDevice;
        startPreview(cameraDevice);
        Log.i(TAG, "Camera opened");
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice cameraDevice) {
        cameraDevice.close();
        Log.i(TAG, "Camera disconnected");
    }

    @Override
    public void onError(@NonNull CameraDevice cameraDevice, int i) {
        cameraDevice.close();
        Log.e(TAG, "Open failed");
    }

    //Face detector
    public interface FaceDetectorCallback {
        void onGetFaces(Face[] faces);
    }
}