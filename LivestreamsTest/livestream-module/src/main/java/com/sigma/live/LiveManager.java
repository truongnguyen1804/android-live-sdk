package com.sigma.live;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.util.Base64;
import android.view.SurfaceHolder;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.pedro.encoder.input.video.CameraHelper;
import com.pedro.rtplibrary.rtmp.RtmpCamera1;
import com.pedro.rtplibrary.rtmp.RtmpDisplay;
import com.pedro.rtplibrary.view.OpenGlView;
import com.pedro.rtplibrary.view.TakePhotoCallback;
import com.sigma.Common;
import com.sigma.FullLog;
import com.views.MyViewGroup;

import net.ossrs.rtmp.BitrateControl;
import net.ossrs.rtmp.ConnectCheckerRtmp;
import net.ossrs.rtmp.SigmaMonitor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class LiveManager {
    private static LiveManager mInstance = new LiveManager();
    private final String[] CAMERA_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA
    };
    private final String[] SCREEN_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO
    };
    private List<Resolution> mResolutions = Arrays.asList(
            Resolution.SSD,
            Resolution.SD,
            Resolution.HD,
            Resolution.FULLHD
    );

    private String TAG = "live_manager";
    private Resolution mResolution = Resolution.HD;
    private Activity mActivity;
    private LiveListener mListener;
    private String mUrl;
    private LiveValidator mValidator;
    private int REQUEST_CODE_STREAM = 9889;
    private NotificationManager mNotificationManager;
    private int rotation = 0;
    private PreviewSizeListener previewSizeListener;

    private boolean isCallStop = false;
    private boolean isSurfaceCreated = false;

    private boolean isConnecting = false;

    private boolean canPushDisconnect = true;

    private static int STARTING = 1;
    private static int STARTED = 2;
    private static int STOPED = 3;
    private int stateLive = 0;
    Handler handlers = new Handler();

    public void setOrientation(int orientation) {
        this.rotation = orientation;
        CameraHelper.setCameraOrientation(null, rotation);
    }

    private ConnectCheckerRtmp mConnectCheckRtmp = new ConnectCheckerRtmp() {
        @Override
        public void onConnectingRtmp() {
            FullLog.LogD("ConnectCheckerRtmp: " + "onConnectingRtmp ");
            isConnecting = true;
            stateLive = STARTING;
        }

        @Override
        public void onConnectionSuccessRtmp() {
            FullLog.LogD("ConnectCheckerRtmp: " + "onConnectionSuccessRtmp ");
            isConnecting = false;
            stateLive = STARTED;

            if (mActivity != null)
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onLiveStarted();
                    }
                });
        }

        @Override
        public void onConnectionFailedRtmp(final String reason) {
            FullLog.LogE("ConnectCheckerRtmp: " + "onConnectionFailedRtmp " + reason);
            isConnecting = false;
            stateLive = STOPED;
            if (mActivity != null)
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onLiveError(new Exception(reason));
                        mListener.onConnectFailed(new Exception(reason));
                        if (isCallStop) {
                            FullLog.LogE("ConnectCheckerRtmp: " + "callStop");
                            stop();
                        } else {
                            if (canPushDisconnect) {
                                canPushDisconnect = false;
                                mListener.onDisConnect();
//                                reStartConnect(3000);
                            }
                        }
                    }
                });
//            if (isCallStop) {
//                stop();
//            }
        }

        @Override
        public void onConnectionStartedRtmp(String s) {
            FullLog.LogE("ConnectCheckerRtmp: " + "onConnectionStartedRtmp ");
            isConnecting = false;
            stateLive = STARTED;
            if (mActivity != null)
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onConnectionStarted();
                    }
                });
        }

        @Override
        public void onDisconnectRtmp() {
            FullLog.LogE("ConnectCheckerRtmp: " + "onDisconnectRtmp ");
            isConnecting = false;
            stateLive = STOPED;
            if (mActivity != null)
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isCallStop) {
                            mListener.onLiveStopped();
                        } else {
                            if (canPushDisconnect) {
                                canPushDisconnect = false;
                                mListener.onDisConnect();
//                                reStartConnect(3000);
                            }
                        }

                    }
                });
        }

        @Override
        public void onAuthErrorRtmp() {
            FullLog.LogE("ConnectCheckerRtmp: " + "onAuthErrorRtmp ");
            isConnecting = false;
            if (mActivity != null)
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onLiveError(new Exception("Can't authen"));
                    }
                });
        }

        @Override
        public void onAuthSuccessRtmp() {
            FullLog.LogE("ConnectCheckerRtmp: " + "onAuthSuccessRtmp ");
            isConnecting = false;

        }

        @Override
        public void onNewBitrateRtmp(long bitrate) {
            FullLog.LogE("ConnectCheckerRtmp: " + "onNewBitrateRtmp " + bitrate);
            if (mActivity != null)
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onNewBitrateReceived(bitrate);
                    }
                });
            try {
                if (mVideoSource != null)
                    mVideoSource.updateBitrate((int) bitrate);
                FullLog.LogD("SigmaLive", "Current bitrate: " + bitrate + "/" + SigmaMonitor.getInfo().mBitrate);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    };
    private int mFps = 30;
    private boolean isChangeResolutionWhenRunning = false;
    private int mWidth, mHeight;
    public static VideoSource mVideoSource;


    public void reStartConnect(int timeDelay) {

        if (mVideoSource instanceof ScreenSource) {

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handler.removeCallbacks(runnable);
                    handler.postDelayed(runnable, timeDelay);
                }
            });
        }
    }

    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            ((LiveManager.ScreenSource) LiveManager.mVideoSource).reconnect();
            reStartConnect(10000);
        }
    };

    OpenGlView.SurfaceListener surfaceListener;

    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            isSurfaceCreated = true;
            if (mActivity != null) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (surfaceListener != null) {
                            surfaceListener.onCreated();
                        }
                    }
                });
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            FullLog.LogD("surfaceChanged: " + width + " -- " + height);
            startPreview();
            if (mActivity != null) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (surfaceListener != null) {
                            surfaceListener.onChanged();
                        }
                    }
                });
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // Không gọi destroy khi background
            FullLog.LogD("SrsFlvMuxer: " + "surfaceDestroyed");
            if (mActivity != null) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (surfaceListener != null) {
                            surfaceListener.onDestroyed();
                        }
                    }
                });
            }
            isSurfaceCreated = false;
            stopPreview();
            stop();

        }
    };

    private LiveManager() {
    }

    public static LiveManager getInstance() {
        return mInstance;
    }

    public void setWaitingImage(Bitmap bitmap) {
        if (mVideoSource != null && bitmap != null) {
            mVideoSource.setImageWaiting(bitmap);
        }
    }


    private void setupInternal(Activity activity, ViewGroup container, int[] paddings, LiveListener listener, boolean isCamera, boolean isFrontCam, PreviewSizeListener previewSizeListener, OpenGlView.SurfaceListener surfaceListener) throws Exception {

        if (isConnecting) {
            if (surfaceListener != null) {
                surfaceListener.onError(new Exception("Can not call setupInternal when connecting Socket"));
            }
            return;
        }
//        AppCloseReceiver appCloseReceiver = new AppCloseReceiver();
//        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
//        intentFilter.addDataScheme("package");
//        activity.registerReceiver(appCloseReceiver, intentFilter);

        mValidator = new LiveValidator(activity.getPackageName());
        mActivity = activity;
        mNotificationManager = (NotificationManager) mActivity.getSystemService(Activity.NOTIFICATION_SERVICE);
        mListener = listener;

        if (mVideoSource != null && !(mVideoSource instanceof EmptySource)) {
            mVideoSource.stop();
            mVideoSource.stopPreview();
            mVideoSource = null;
        }
        isSurfaceCreated = false;
        if (isCamera) {
            this.surfaceListener = surfaceListener;
            mVideoSource = new CameraSource(isFrontCam);
            isCallStop = true;
        } else {
            mVideoSource = new ScreenSource();
            isCallStop = false;
        }

        mVideoSource.setupInternal(activity, container, paddings);
        container.setKeepScreenOn(true);
        refresh();
        if (previewSizeListener != null) {
            this.previewSizeListener = previewSizeListener;
            previewSizeListener.onPreviewSize(mWidth, mHeight);
        }

    }

    private void setupInternalFile(OpenGlView openGlView, Activity activity, ViewGroup container, LiveListener listener, PreviewSizeListener previewSizeListener) throws Exception {
        if (isConnecting) {
            return;
        }
        mValidator = new LiveValidator(activity.getPackageName());
        mActivity = activity;
        mNotificationManager = (NotificationManager) mActivity.getSystemService(Activity.NOTIFICATION_SERVICE);
        mListener = listener;
        if (mVideoSource != null && !(mVideoSource instanceof EmptySource)) {
            mVideoSource.stop();
            mVideoSource = null;
        }
        isSurfaceCreated = false;
        isCallStop = false;
        mVideoSource = new FileSource();
        mVideoSource.setupFileInternal(openGlView);
//        container.setKeepScreenOn(true);
        refresh();
        this.previewSizeListener = previewSizeListener;
        previewSizeListener.onPreviewSize(mWidth, mHeight);
    }

    public void setup(Activity activity, ViewGroup container, int[] paddings, LiveListener listener, boolean isFrontCam, PreviewSizeListener previewSizeListener, OpenGlView.SurfaceListener surfaceListener) throws Exception {
        if (checkPermission(activity, true)) {
            setupInternal(activity, container, paddings, listener, true, isFrontCam, previewSizeListener, surfaceListener);
        } else {
            listener.onPermissionDenied();
        }
    }

    public void setupScreenStream(Activity activity, ViewGroup container, LiveListener listener) throws Exception {
        if (checkPermission(activity, false)) {
            setupInternal(activity, container, null, listener, false, false, null, null);
        } else {
            listener.onPermissionDenied();
        }
    }

    public void setupStreamFile(OpenGlView openGlView, Activity activity, ViewGroup container, LiveListener listener) throws Exception {
        if (checkPermission(activity, false)) {
            setupInternalFile(openGlView, activity, container, listener, null);
        } else {
            listener.onPermissionDenied();
        }
    }

    public void setSurfaceViewParams(Common.TypePivot pivot, int widthPixel, int heightPixel, int startX, int startY, int durationMs) {
        if (mVideoSource != null) {
            mVideoSource.setSurfaceViewParams(pivot, widthPixel, heightPixel, startX, startY, durationMs);
        }

    }

    public void scaleSurfaceView(boolean requestScale) {
        if (mVideoSource != null) {
            mVideoSource.scaleViewOverlay(requestScale);
        }
    }


    public void setResolution(Resolution resolution) {
        mResolution = resolution;
        if (isRunning()) {
            isChangeResolutionWhenRunning = true;

            FullLog.LogD("SrsFlvMuxer: " + "setResolution");
            stop();
        }
        refresh();
        restart();
    }

    public void enableKaraokeEffect() {
        KaraokeManager.getInstance().enableEcho(0.6f, 0.3f, new int[]{200, 300, 500, 1000}, new float[]{0.5f, 0.3f, 0.2f, 0.1f});
    }

    public void disableKaraokeEffect() {
        KaraokeManager.getInstance().disableEcho();
    }

    public Resolution getResolution() {
        return mResolution;
    }

    public List<Resolution> getSupportedResolutions() {
        return new ArrayList<>(mResolutions);
    }

    public void switchCameraFace() {
        if (mVideoSource != null)
            mVideoSource.switchCameraFace();
    }

    public CameraFace getCameraFace() {
        if (mVideoSource != null)
            return mVideoSource.getCameraFace();
        return null;
    }

    public Camera getCamera() {
        if (mVideoSource != null)
            return mVideoSource.getCamera();
        return null;
    }

    public RtmpCamera1 getMCamera() {
        if (mVideoSource != null)
            return mVideoSource.getmCamera();
        return null;
    }

    public void setCameraFace(CameraFace face) {
        if (mVideoSource != null)
            mVideoSource.setCamera(face);
    }

    @TargetApi(23)
    private boolean checkPermission(Activity activity, boolean camera) throws Exception {
        if (Build.VERSION.SDK_INT < 23) return true;
        String[] needs = camera ? CAMERA_PERMISSIONS : SCREEN_PERMISSIONS;
        for (String permission : needs) {
            if (activity.checkSelfPermission(permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void restart() {
        FullLog.LogD(TAG + " restart");
        if (isRunning()) {
            FullLog.LogD("SrsFlvMuxer: " + "restart");
            stop();
            start();
        } else {
            stopPreview();
            startPreview();
            if (isChangeResolutionWhenRunning) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                start();
                            }
                        }, 5000);
                    }
                });
                isChangeResolutionWhenRunning = false;
            }
        }
    }

    public boolean setLedEnable(boolean enable) {
        if (mVideoSource != null)
            return mVideoSource.setLedEnable(enable);
        return false;
    }

    public boolean isLedEnable() {
        if (mVideoSource != null)
            return mVideoSource.isLedEnable();
        return false;
    }

    public void startPreview() {
        if (mVideoSource != null)
            mVideoSource.startPreview();
    }

    public boolean isFontCam() {
        if (mVideoSource != null) {
            return mVideoSource.isFrontCamera();
        }
        return false;
    }

    public void stopPreview() {
        if (mVideoSource != null)
            mVideoSource.stopPreview();
    }

    private void refresh() {
        if (mVideoSource != null) {
            mVideoSource.refresh();
            BitrateControl.getInstance().setMaxBitrate(mResolution.getVideoBitrate());
        }
    }

    public void start(String url) {
        mUrl = url;
        start();
    }

    private void start() {
        if (mVideoSource != null)
            mVideoSource.start();
    }

    public void pause(Boolean b) {
        if (mVideoSource != null)
            mVideoSource.onPause(b);
        return;
    }

    public void reconnect() {
        if (mVideoSource != null) {
            mVideoSource.reconnect();
        }
    }


    public void stop() {
        FullLog.LogD("SrsFlvMuxer: " + "stop");
        isCallStop = true;
        if (mVideoSource != null && mVideoSource.isRunning())
            mVideoSource.stop();
//        if (mListener != null) {
//            mListener.onLiveStopped();
//        }
    }

    public void callDisconnect() {
        FullLog.LogD("SrsFlvMuxer: " + "callDisconnect");

        isCallStop = false;
        if (mVideoSource != null)
            mVideoSource.callDisconnect();

    }


    public boolean isRunning() {
        if (mVideoSource != null)
            return mVideoSource.isRunning();
        return false;
    }

    public boolean isOnPreview() {
        if (mVideoSource != null)
            return mVideoSource.isOnPreview();
        return false;
    }

    public boolean isAudioEnable() {
        if (mVideoSource != null)
            return mVideoSource.isAudioEnable();
        return false;
    }

    public void setAudioEnable(boolean enable) {
        if (mVideoSource != null)
            mVideoSource.setAudioEnable(enable);
        return;
    }

    public boolean isVideoEnable() {
        if (mVideoSource != null)
            return mVideoSource.isVideoEnable();
        return false;
    }

    public void setVideoEnable(boolean enable) {
        if (mVideoSource != null)
            mVideoSource.setVideoEnable(enable);
        return;
    }

    public void captureImage(final ImageCaptureListener listener) {
        if (mVideoSource != null)
            mVideoSource.captureImage(listener);
    }

    public TrackInfo getTrackInfo() {
        return SigmaMonitor.getInfo();
    }

    class LiveValidator {
        String mSecret;

        void decode(String packageName) throws Exception {
            byte[] key = new byte[16];
            byte[] pkgBytes = packageName.getBytes();
            for (int i = 0; i < 16 && i < pkgBytes.length; i++) {
                key[i] = pkgBytes[i];
            }
            byte data[] = Base64.decode(BuildConfig.TOKEN, Base64.DEFAULT);
            String method = "AES";
            SecretKey secretKey = new SecretKeySpec(key, method);
            Cipher cipher = Cipher.getInstance(method);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decrypted = cipher.doFinal(data);
            mSecret = new String(decrypted);
        }

        LiveValidator(String packageName) throws Exception {
            try {
                decode("com.sigma.live.demo");
            } catch (Exception ex) {
                decode(packageName);
            }
        }

        boolean isValid(String url) {
            try {
                String streamId = url.substring(url.lastIndexOf('/') + 1);
                int markIndex = streamId.indexOf("?");
                if (markIndex > 0)
                    streamId = streamId.substring(0, markIndex);
                int index = streamId.indexOf('-');
                String publicId = streamId.substring(0, index);
                String target = streamId.substring(index + 1);
                String found = md5(publicId + ":" + mSecret);
                return found.substring(0, 10).equals(target);
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
        }

        private String md5(final String s) {
            final String MD5 = "MD5";
            try {
                MessageDigest digest = MessageDigest.getInstance(MD5);
                digest.update(s.getBytes());
                byte messageDigest[] = digest.digest();
                StringBuilder hexString = new StringBuilder();
                for (byte aMessageDigest : messageDigest) {
                    String h = Integer.toHexString(0xFF & aMessageDigest);
                    while (h.length() < 2)
                        h = "0" + h;
                    hexString.append(h);
                }
                return hexString.toString();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return "";
        }
    }

    private void showNotification() {
        Notification.Builder notificationBuilder =
                new Notification.Builder(mActivity).setSmallIcon(R.drawable.notification_anim)
                        .setContentTitle("Streaming")
                        .setContentText("Display mode stream")
                        .setTicker("Stream in progress");
        notificationBuilder.setAutoCancel(true);
        if (mNotificationManager != null)
            mNotificationManager.notify(12345, notificationBuilder.build());
    }

    private void hideNotification() {
        if (mNotificationManager != null) {
            mNotificationManager.cancel(12345);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_STREAM && resultCode == Activity.RESULT_OK && mVideoSource != null) {
            mVideoSource.onStart(resultCode, data);
        } else {
            Toast.makeText(mActivity, "No permissions available", Toast.LENGTH_SHORT).show();
        }
    }

    private void printConfig() {
        FullLog.LogD("SigmaLive", String.format("Started %dx%d@%d", mWidth, mHeight, mFps));
    }

    interface VideoSource {
        void updateBitrate(int bitrate);

        void switchCameraFace();

        void setImageWaiting(Bitmap bitmap);

        CameraFace getCameraFace();

        Camera getCamera();

        void setCamera(CameraFace face);

        boolean isFrontCamera();

        void setCameraFace(CameraFace face);


        void setupInternal(Activity activity, ViewGroup container, int[] paddings) throws Exception;

        void setupFileInternal(OpenGlView view);

        void start();

        void setSurfaceViewParams(Common.TypePivot pivot, int widthPixel, int heightPixel, int startX, int startY, int durationMs);

        void scaleViewOverlay(boolean requestScale);

        void onStart(int resultCode, Intent data);

        void onPause(Boolean p);

        void reconnect();

        void stop();

        void callDisconnect();

        boolean isRunning();

        boolean isAudioEnable();

        void setAudioEnable(boolean enable);

        boolean setLedEnable(boolean enable);

        boolean isLedEnable();

        boolean isAudioPrepared();

        boolean isVideoPrepared();

        boolean isOnPreview();

        void startPreview();

        void stopPreview();

        void refresh();

        boolean isVideoEnable();

        void setVideoEnable(boolean enable);

        void captureImage(final ImageCaptureListener listener);

        RtmpCamera1 getmCamera();
    }

    class CameraSource implements VideoSource {
        private CameraFace mFace = CameraFace.Back;
        private boolean isFontCam = false;
        boolean mLedEnable;
        RtmpCamera1 mCamera;
        ViewGroup viewParent;

        public CameraSource(boolean isFontCam) {
            this.isFontCam = isFontCam;
            if (isFontCam) {
                mFace = CameraFace.Front;
            } else {
                mFace = CameraFace.Back;
            }
        }

        public RtmpCamera1 getmCamera() {
            return mCamera;
        }

        private OpenGlView mOpenGlView;

        private int widthPixel;
        private Common.TypePivot pivot;
        private int heightPixel;
        private int startX;
        private int startY;
        private int durationMs;
        private MyViewGroup layoutParent;
        private int parrentPaddingTop = 0;
        private int parrentPaddingLeft = 0;
        private int parrentPaddingRight = 0;
        private int parrentPaddingBottom = 0;

        private int surfaceWidthOld = -2;
        private int surfaceHeightOld = -2;

        @Override
        public void updateBitrate(int bitrate) {
            if (mCamera != null)
                mCamera.setVideoBitrateOnFly(bitrate);
        }

        public void switchCameraFace() {
            FullLog.LogD(TAG + " switchCameraFace");
//            mFace = mFace == CameraFace.Back ? CameraFace.Front : CameraFace.Back;
            setCameraFace(mFace == CameraFace.Back ? CameraFace.Front : CameraFace.Back);
//            setCameraFace(mFace);
        }

        @Override
        public void setImageWaiting(Bitmap bitmap) {
            if (mActivity != null && mOpenGlView != null)
                Common.post(mActivity.getApplicationContext(), new Runnable() {
                    @Override
                    public void run() {
                        if (mOpenGlView != null) {
                            mOpenGlView.setImageThumbnail(bitmap);
                        }
                    }
                });

        }

        @Override
        public CameraFace getCameraFace() {
            return mFace;
        }

        @Override
        public Camera getCamera() {
            if (mCamera != null)
                return mCamera.getCamera();
            return null;
        }

        @Override
        public void setCamera(CameraFace face) {
            FullLog.LogD(TAG + " setCamera, face " + face);
            if (mCamera != null) {
                FullLog.LogD(TAG + " setCamera true");
//                if (face == mFace) return;
//                try {
//                    mCamera.switchCamera();
//                    mFace = face;
//                    isFontCam = mFace != CameraFace.Back;
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                    mFace = face;
//                    refresh();
//                    restart();
//                }
                try {
                    isFontCam = face == CameraFace.Front;
                    if (face == CameraFace.Back) {
                        mCamera.setCameraFace(Camera.CameraInfo.CAMERA_FACING_BACK);
                    } else {
                        mCamera.setCameraFace(Camera.CameraInfo.CAMERA_FACING_FRONT);
                    }
                    mFace = face;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    mFace = face;
                    refresh();
                    restart();
                }

            }
        }

        @Override
        public boolean isFrontCamera() {
            if (mCamera != null)
                return mCamera.isFrontCamera();
            return false;
        }

        @Override
        public boolean isOnPreview() {
            if (mCamera != null)
                return mCamera.isOnPreview();
            return false;
        }

        @Override
        public void setCameraFace(CameraFace face) {
            FullLog.LogD(TAG + " setCameraFace");

            if (mCamera != null) {
                FullLog.LogD(TAG + " setCameraFace true");
                if (face == mFace) return;
                try {
                    mCamera.switchCamera();
                    mFace = face;
                    isFontCam = mFace != CameraFace.Back;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    mFace = face;
                    refresh();
                    restart();
                }
            }
        }

        @Override
        public void setupInternal(Activity activity, ViewGroup container, int[] paddings) throws Exception {
            FullLog.LogD(TAG + " setupInternal, cameraIsFront " + isFontCam);
            layoutParent = new MyViewGroup(activity);
            ViewGroup.LayoutParams layoutParamsParent = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParent.setPadding((paddings != null && paddings.length > 0) ? paddings[0] : 0,
                    (paddings != null && paddings.length > 1) ? paddings[1] : 0,
                    (paddings != null && paddings.length > 2) ? paddings[2] : 0,
                    (paddings != null && paddings.length > 3) ? paddings[3] : 0);
            layoutParent.setLayoutParams(layoutParamsParent);
            for (int i = 0; i < container.getChildCount(); i++) {
                if (container.getChildAt(i) instanceof MyViewGroup) {
                    container.removeViewAt(i);
                }
            }
            container.addView(layoutParent, 0);
            mOpenGlView = new OpenGlView(activity);

            for (int i = 0; i < layoutParent.getChildCount(); i++) {
                if (layoutParent.getChildAt(i) instanceof OpenGlView) {
                    layoutParent.removeViewAt(i);
                }
            }
            this.viewParent = container;

            layoutParent.addView(mOpenGlView, 0);
            //mOpenGlView.setFilter(new GreyScaleFilterRender());
            mOpenGlView.setKeepAspectRatio(true);
            mOpenGlView.getHolder().addCallback(mSurfaceCallback);
            mCamera = new RtmpCamera1(mOpenGlView, mConnectCheckRtmp);
            mCamera.setCameraFace(isFontCam ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK);
            mOpenGlView.post(new Runnable() {
                @Override
                public void run() {
                    surfaceWidthOld = mOpenGlView.getWidth();
                    surfaceHeightOld = mOpenGlView.getHeight();
                    FullLog.LogD("mOpenGlView size: " + surfaceHeightOld + " -- " + surfaceWidthOld);
                }
            });

        }

        @Override
        public void setupFileInternal(OpenGlView view) {

        }

        @Override
        public void start() {
            FullLog.LogD(TAG + " start, isFrontCam " + isFontCam);
            if (mListener == null)
                return;
            if (stateLive == STARTING || stateLive == STARTED) {
                mListener.onLiveError(new Exception("Can not start a stream when it's STARTING or STARTED"));
            } else {
                if (mCamera != null && isSurfaceCreated) {
                    try {
                        if (mCamera.prepareAudio(mResolution.getAudioBitrate(), 44100, false,
                                true, true, true)
                                && mCamera.prepareVideo(mWidth, mHeight, mCamera.getFps(), mResolution.getVideoBitrate(),
                                false, CameraHelper.getCameraOrientation(mActivity)
                                /*CameraHelper.chooseCameraOrientation(mActivity, 0)*/)) {
                            FullLog.LogD("checkUrlLive:" + mUrl + " -- " + mWidth + " -- " + mHeight);
                            mCamera.startStream(mUrl);
                            mListener.onLiveStarting();
                            stateLive = STARTING;
                            printConfig();
                            if (mLedEnable)
                                setLedEnable(true);
                            return;
                        }
                    } catch (Exception ex) {
                        mListener.onLiveError(ex);
                        ex.printStackTrace();
                        stop();
                    }
                } else {
                    if (surfaceListener != null) {
                        surfaceListener.onSurfaceInvalid(new Exception("Can not start a stream camera when surface invalid"));

                    }
                }
            }


        }

        @Override
        public void setSurfaceViewParams(Common.TypePivot pivot, int widthPixel, int heightPixel, int startX, int startY, int durationMs) {
            this.widthPixel = widthPixel;
            this.heightPixel = heightPixel;
            this.startX = startX;
            this.startY = startY;
            this.pivot = pivot;
            this.durationMs = durationMs;
        }

        @Override
        public void onStart(int resultCode, Intent data) {

        }

        @Override
        public void onPause(Boolean p) {
            FullLog.LogD(TAG + " onPause");
            LiveManager.this.setAudioEnable(p);
            LiveManager.this.setVideoEnable(p);
        }

        @Override
        public void reconnect() {

        }

        @Override
        public void stop() {
            FullLog.LogD(TAG + " stop, isFrontCam " + isFontCam);
            if (mCamera != null && mCamera.isStreaming()) {
                FullLog.LogD(TAG + " stop true");
                mCamera.stopStream();
                boolean isLedOn = isLedEnable();
                if (isLedOn) {
                    setLedEnable(false);
                    mLedEnable = isLedOn;
                }
            }
            if (mOpenGlView != null) {
                mOpenGlView.getHolder().removeCallback(mSurfaceCallback);
            }
        }

        @Override
        public void callDisconnect() {

        }

        @Override
        public boolean isRunning() {
            if (mCamera != null) {
                return mCamera.isStreaming();
            } else
                return false;

        }

        @Override
        public boolean isAudioEnable() {
            if (mCamera != null)
                return !mCamera.isAudioMuted();
            return false;
        }

        @Override
        public void setAudioEnable(boolean enable) {
            if (mCamera != null) {
                if (enable)
                    mCamera.enableAudio();
                else mCamera.disableAudio();
            }
        }

        @Override
        public boolean setLedEnable(boolean enable) {
            if (mCamera != null) {
                try {
                    if (enable)
                        mCamera.enableLantern();
                    else mCamera.disableLantern();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return mLedEnable = isLedEnable();
            }
            return false;

        }

        @Override
        public boolean isLedEnable() {
            if (mCamera != null) {
                return mCamera.isLanternEnabled();
            } else {
                return false;
            }

        }

        @Override
        public boolean isAudioPrepared() {
            if (mCamera != null)
                return mCamera.prepareAudio();
            return false;
        }

        @Override
        public boolean isVideoPrepared() {
            if (mCamera != null)
                return mCamera.prepareVideo();
            return false;
        }

        @Override
        public void startPreview() {
            FullLog.LogD(TAG + " startPreview");
            if (mCamera != null) {
                FullLog.LogD(TAG + " startPreview true, isFrontCam " + isFontCam);
                FullLog.LogD("startPreviewww: " + isFontCam);
                mCamera.startPreview((isFontCam ? CameraFace.Front : CameraFace.Back).getValue()/*mFace.getValue()*/, mWidth, mHeight, mFps);
            }
        }

        @Override
        public void stopPreview() {
            FullLog.LogD(TAG + " stopPreview, isFrontCam " + isFontCam);
            if (mCamera != null)
                mCamera.stopPreview();
        }

        @Override
        public void refresh() {
            FullLog.LogD("CameraSourceRefresh=>", "123");
            List<Camera.Size> sizes = mFace == CameraFace.Back ? mCamera.getResolutionsBack() : mCamera.getResolutionsFront();
            Camera.Size found = sizes.get(0);
            int width = mResolution.getWidth();
            int height = mResolution.getHeight();
            int dxf = Math.abs(found.width - width);
            int dyf = Math.abs(found.height - height);
            for (int i = 0; i < sizes.size(); i++) {
                Camera.Size size = sizes.get(i);
                int dx = Math.abs(size.width - width);
                int dy = Math.abs(size.height - height);
                if (dy < dyf || dy == dyf && dx < dxf) {
                    dxf = dx;
                    dyf = dy;
                    found = size;
                    FullLog.LogD("camerasize=>", found.width + "_" + found.height + "_" + width + "_" + height);
                }
            }
            FullLog.LogD("camerasize=>final", found.width + "_" + found.height + "_" + width + "_" + height);
            mWidth = found.width;
            mHeight = found.height;
//            mWidth = width;
//            mHeight = height;
        }

        public boolean isVideoEnable() {
            if (mOpenGlView != null)
                return mOpenGlView.isVideoEnable();
            return false;
        }

        public void setVideoEnable(boolean enable) {
            if (mOpenGlView != null)
                mOpenGlView.setVideoEnable(enable);

        }

        public void captureImage(final ImageCaptureListener listener) {
            if (mOpenGlView != null)
                mOpenGlView.takePhoto(new TakePhotoCallback() {
                    @Override
                    public void onTakePhoto(final Bitmap bitmap) {
                        try {
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onImageCaptured(bitmap);
                                }
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
        }

        @Override
        public void scaleViewOverlay(boolean requestScale) {

            if (mOpenGlView == null || surfaceWidthOld == -2 || surfaceHeightOld == -2)
                return;

            float pivotX = 0, pivotY = 0;

            float scale = Math.min(widthPixel / (float) surfaceWidthOld, heightPixel / (float) surfaceHeightOld) * 100;
            widthPixel = (int) (scale * surfaceWidthOld) / 100;
            heightPixel = (int) (scale * surfaceHeightOld) / 100;

            float left = (float) startX;
            float right = (float) startX + widthPixel;
            float top = (float) startY;
            float bottom = (float) startY + heightPixel;

            switch (pivot) {
                case TOP:
                    pivotX = 0.5f;
                    pivotY = top / (float) surfaceHeightOld;
                    break;
                case LEFT:
                    pivotX = left / (float) surfaceWidthOld;
                    pivotY = 0.5f;
                    break;
                case RIGHT:
                    pivotX = right / (float) surfaceWidthOld;
                    pivotY = 0.5f;
                    break;
                case BOTTOM:
                    pivotX = 0.5f;
                    pivotY = bottom / (float) surfaceHeightOld;
                    break;
            }

            ScaleAnimation anim;// Needed to keep the result of the animation
            if (requestScale) {
                anim = new ScaleAnimation(1f, scale / 100, 1f,
                        scale / 100,
                        Animation.RELATIVE_TO_PARENT,
                        pivotX,
                        Animation.RELATIVE_TO_PARENT,
                        pivotY);

            } else {
                anim = new ScaleAnimation(scale / 100, 1f, scale / 100,
                        1f,
                        Animation.RELATIVE_TO_PARENT,
                        pivotX,
                        Animation.RELATIVE_TO_PARENT,
                        pivotY);
            }
            anim.setFillAfter(true); // Needed to keep the result of the animation
            anim.setDuration(durationMs);
            mOpenGlView.startAnimation(anim);
        }
    }

    class ScreenSource implements VideoSource {
        public RtmpDisplay mDisplay;

        @Override
        public void updateBitrate(int bitrate) {
            if (mDisplay != null)
                mDisplay.setVideoBitrateOnFly(bitrate);
        }

        public void switchCameraFace() {

        }

        @Override
        public void setImageWaiting(Bitmap bitmap) {
            if (mDisplay != null)
                Common.post(mActivity.getApplicationContext(), new Runnable() {
                    @Override
                    public void run() {
                        if (mDisplay.getOpenGl() != null)
                            mDisplay.getOpenGl().setImageThumbnail(bitmap);
                    }
                });
        }

        @Override
        public CameraFace getCameraFace() {
            return CameraFace.None;
        }

        @Override
        public Camera getCamera() {
            return null;
        }

        @Override
        public void setCamera(CameraFace face) {

        }

        @Override
        public boolean isFrontCamera() {
            return false;
        }

        @Override
        public void setCameraFace(CameraFace face) {

        }

        @Override
        public void setupInternal(Activity activity, ViewGroup container, int[] paddings) throws Exception {
            mDisplay = new RtmpDisplay(activity, true, mConnectCheckRtmp);
        }

        @Override
        public void setupFileInternal(OpenGlView view) {

        }

        @Override
        public void start() {
            if (mActivity != null && mDisplay != null)
                mActivity.startActivityForResult(mDisplay.sendIntent(), REQUEST_CODE_STREAM);
        }

        @Override
        public void setSurfaceViewParams(Common.TypePivot pivot, int widthPixel, int heightPixel, int startX, int startY, int durationMs) {

        }

        @Override
        public void scaleViewOverlay(boolean requestScale) {

        }

        @Override
        public void onStart(int resultCode, Intent data) {
            FullLog.LogD(TAG + " onStart " + resultCode);
            if (mListener == null) {
                return;
            }
            if (stateLive == STARTING || stateLive == STARTED) {
                mListener.onLiveError(new Exception("Can not start a stream when it's STARTING or STARTED"));
            } else {
                try {
                    stateLive = STARTING;
                    mDisplay.setIntentResult(resultCode, data);
                    isCallStop = false;
                    handlers.removeCallbacks(runnablePrepare);
                    handlers.postDelayed(runnablePrepare, 2000);

                } catch (Exception ex) {
                    mListener.onLiveError(ex);
                    ex.printStackTrace();
                    stop();
                }
            }

        }

        @Override
        public void onPause(Boolean p) {
            if (mDisplay != null)
                if (p) {
                    mDisplay.enableAudio();
                } else {
                    mDisplay.disableAudio();
                }
        }

        public void startLive() {
            if (mDisplay != null && mListener != null) {
                if (mDisplay.getData() != null) {
                    canPushDisconnect = true;
                    stateLive = STARTING;
                    mDisplay.startStream(mUrl);
                    mListener.onLiveStarting();
                    showNotification();
                    printConfig();
                } else {
                    Intent intent = new Intent(mActivity, SigmaService.class);
                    mActivity.stopService(intent);
                    mListener.onPermissionDenied();
                }

            }
        }

        @Override
        public void reconnect() {
            isCallStop = false;
            if (mListener == null) {
                return;
            }
            FullLog.LogD(TAG + " reconnect ");
            if (stateLive == STARTING || stateLive == STARTED) {
                mListener.onLiveError(new Exception("Can not start a stream when it's STARTING or STARTED"));
                return;
            }
            if (mDisplay != null && mActivity != null) {
                try {
                    mDisplay.restartStream();
                    Intent intent = new Intent(mActivity, SigmaService.class);
                    mActivity.stopService(intent);

                    stateLive = STARTING;
                    handlers.removeCallbacks(runnablePrepare);
                    handlers.postDelayed(runnablePrepare, 2000);


                } catch (Exception ex) {
                    mListener.onLiveError(ex);
                    ex.printStackTrace();
                }
            } else {
                mListener.onLiveError(new Exception("Cannot reconnect when mDisplay or mActivity is null"));
            }
        }

        Runnable runnablePrepare = new Runnable() {
            @Override
            public void run() {
                if (mDisplay == null || mResolution == null || mActivity == null || mListener == null) {
                    return;
                }
                if (mDisplay.prepareAudio(mResolution.getAudioBitrate(), 48000, false,
                        true, true, true)
                        && mDisplay.prepareVideo(mWidth, mHeight, mFps, mResolution.getVideoBitrate(), false, 0, 320)) {

                    if (isMyServiceRunning(SigmaService.class, mActivity)) {
                        Intent intent = new Intent(mActivity, SigmaService.class);
                        mActivity.stopService(intent);
                    }

                    Intent intent = new Intent(mActivity, SigmaService.class);
                    mActivity.startService(intent);
                    handlers.removeCallbacks(runnableStartLive);
                    handlers.postDelayed(runnableStartLive, 1000);
                } else {
                    mListener.onLiveError(new Exception("prepareAudio is error!"));
                }
            }
        };

        Runnable runnableStartLive = new Runnable() {
            @Override
            public void run() {
                startLive();
            }
        };

        @Override
        public void stop() {
            FullLog.LogD(TAG + " stop ");
            if (mDisplay != null)
                try {
                    FullLog.LogD(TAG + " SrsFlvMuxer: stop screen");
                    hideNotification();
                    isCallStop = true;
                    mDisplay.stopStream();
                    Intent intent = new Intent(mActivity, SigmaService.class);
                    mActivity.stopService(intent);

                } catch (Exception ex) {
                    mListener.onLiveError(new Exception("Stop stream error"));
                    ex.printStackTrace();
                }
        }

        @Override
        public void callDisconnect() {
            FullLog.LogD(TAG + " callDisconnect ");
            if (mDisplay != null && mActivity != null) {
                try {
                    mDisplay.restartStream();
                    Intent intent = new Intent(mActivity, SigmaService.class);
                    mActivity.stopService(intent);

                } catch (Exception ignored) {
                    mListener.onLiveError(new Exception("Can not reconnect " + ignored.getMessage()));
                }
            }
        }

        @Override
        public boolean isRunning() {
            if (mDisplay != null)
                return mDisplay.isStreaming();
            return false;
        }

        @Override
        public boolean isAudioEnable() {
            if (mDisplay != null)
                return !mDisplay.isAudioMuted();
            return false;
        }

        @Override
        public void setAudioEnable(boolean enable) {
            if (mDisplay != null)
                if (enable)
                    mDisplay.enableAudio();
                else mDisplay.disableAudio();
        }

        @Override
        public boolean setLedEnable(boolean enable) {
            return false;
        }

        @Override
        public boolean isLedEnable() {
            return false;
        }

        @Override
        public boolean isAudioPrepared() {
            return false;
        }

        @Override
        public boolean isVideoPrepared() {
            return false;
        }

        @Override
        public boolean isOnPreview() {
            return false;
        }

        @Override
        public void startPreview() {

        }

        @Override
        public void stopPreview() {

        }

        @Override
        public void refresh() {
            mWidth = mResolution.getWidth();
            mHeight = mResolution.getHeight();
        }

        public boolean isVideoEnable() {
            if (mDisplay != null)
                return mDisplay.getOpenGl().isVideoEnable();
            return false;
        }

        public void setVideoEnable(boolean enable) {
            if (mDisplay != null)
                mDisplay.getOpenGl().setVideoEnable(enable);
        }

        public void captureImage(final ImageCaptureListener listener) {
            if (mDisplay != null)
                mDisplay.getOpenGl().takePhoto(new TakePhotoCallback() {
                    @Override
                    public void onTakePhoto(final Bitmap bitmap) {
                        try {
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onImageCaptured(bitmap);
                                }
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
        }

        @Override
        public RtmpCamera1 getmCamera() {
            return null;
        }
    }

    class EmptySource implements VideoSource {
        @Override
        public void updateBitrate(int bitrate) {
        }

        @Override
        public void switchCameraFace() {

        }

        @Override
        public void setImageWaiting(Bitmap bitmap) {

        }

        @Override
        public CameraFace getCameraFace() {
            return null;
        }

        @Override
        public Camera getCamera() {
            return null;
        }

        @Override
        public void setCamera(CameraFace face) {

        }

        @Override
        public boolean isFrontCamera() {
            return false;
        }

        @Override
        public void setCameraFace(CameraFace face) {

        }


        @Override
        public void setupInternal(Activity activity, ViewGroup container, int[] paddings) throws Exception {
//            RtmpFromFile rtmp = new RtmpFromFile(activity,)
        }

        @Override
        public void setupFileInternal(OpenGlView view) {

        }

        @Override
        public void start() {

        }

        @Override
        public void setSurfaceViewParams(Common.TypePivot pivot, int widthPixel, int heightPixel, int startX, int startY, int durationMs) {

        }

        @Override
        public void scaleViewOverlay(boolean requestScale) {

        }

        @Override
        public void onStart(int resultCode, Intent data) {

        }

        @Override
        public void onPause(Boolean p) {

        }

        @Override
        public void reconnect() {

        }

        @Override
        public void stop() {

        }

        @Override
        public void callDisconnect() {

        }

        @Override
        public boolean isRunning() {
            return false;
        }

        @Override
        public boolean isAudioEnable() {
            return false;
        }

        @Override
        public void setAudioEnable(boolean enable) {

        }

        @Override
        public boolean setLedEnable(boolean enable) {
            return false;
        }

        @Override
        public boolean isLedEnable() {
            return false;
        }

        @Override
        public boolean isAudioPrepared() {
            return false;
        }

        @Override
        public boolean isVideoPrepared() {
            return false;
        }

        @Override
        public boolean isOnPreview() {
            return false;
        }

        @Override
        public void startPreview() {

        }

        @Override
        public void stopPreview() {

        }

        @Override
        public void refresh() {

        }

        @Override
        public boolean isVideoEnable() {
            return false;
        }

        @Override
        public void setVideoEnable(boolean enable) {

        }

        @Override
        public void captureImage(ImageCaptureListener listener) {

        }

        @Override
        public RtmpCamera1 getmCamera() {
            return null;
        }
    }

    class FileSource implements VideoSource {
        CameraFace mFace = CameraFace.Back;
        boolean mLedEnable;
        RtmpCamera1 mCamera;
        ViewGroup viewParent;

        public RtmpCamera1 getmCamera() {
            return mCamera;
        }

        private OpenGlView mOpenGlView;

        @Override
        public void updateBitrate(int bitrate) {
            if (mCamera != null)
                mCamera.setVideoBitrateOnFly(bitrate);
        }

        public void switchCameraFace() {
            setCameraFace(mFace == CameraFace.Back ? CameraFace.Front : CameraFace.Back);
        }

        @Override
        public void setImageWaiting(Bitmap bitmap) {
            if (mActivity != null && mOpenGlView != null)
                Common.post(mActivity.getApplicationContext(), new Runnable() {
                    @Override
                    public void run() {
                        if (mOpenGlView != null) {
                            mOpenGlView.setImageThumbnail(bitmap);
                        }
                    }
                });

        }

        @Override
        public CameraFace getCameraFace() {
            return mFace;
        }

        @Override
        public Camera getCamera() {
            if (mCamera != null)
                return mCamera.getCamera();
            return null;
        }

        @Override
        public void setCamera(CameraFace face) {

        }

        @Override
        public boolean isFrontCamera() {
            return false;
        }

        @Override
        public boolean isOnPreview() {
            if (mCamera != null)
                return mCamera.isOnPreview();
            return false;
        }

        @Override
        public void setCameraFace(CameraFace face) {
            if (mCamera != null) {
                if (face == mFace) return;
                try {
                    mCamera.switchCamera();
                    mFace = face;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    mFace = face;
                    refresh();
                    restart();
                }
            }
        }


        @Override
        public void setupInternal(Activity activity, ViewGroup container, int[] paddings) throws Exception {

        }


        public void setupFileInternal(OpenGlView openGlView) {

            mOpenGlView = openGlView;
            mOpenGlView.setKeepAspectRatio(true);
            mOpenGlView.getHolder().addCallback(mSurfaceCallback);
            mCamera = new RtmpCamera1(mOpenGlView, mConnectCheckRtmp);
        }


        @Override
        public void start() {
            if (mCamera != null)
                try {
                    if (mCamera.prepareAudio(mResolution.getAudioBitrate(), 44100, false, true, true, true) && mCamera.prepareVideo(mWidth, mHeight, mCamera.getFps(), mResolution.getVideoBitrate(), false, CameraHelper.getCameraOrientation(mActivity) /*CameraHelper.chooseCameraOrientation(mActivity, 0)*/)) {
                        FullLog.LogD("checkUrlLive:" + mUrl + " -- " + mWidth + " -- " + mHeight);
                        mCamera.startStream(mUrl);
                        mListener.onLiveStarting();
                        printConfig();
                        if (mLedEnable)
                            setLedEnable(true);
                    }
                } catch (Exception ex) {
                    mListener.onLiveError(ex);
                    ex.printStackTrace();
                    stop();
                }
        }

        @Override
        public void setSurfaceViewParams(Common.TypePivot pivot, int widthPixel, int heightPixel, int startX, int startY, int durationMs) {

        }

        @Override
        public void scaleViewOverlay(boolean requestScale) {

        }

        @Override
        public void onStart(int resultCode, Intent data) {

        }

        @Override
        public void onPause(Boolean p) {
            LiveManager.this.setAudioEnable(p);
            LiveManager.this.setVideoEnable(p);
        }

        @Override
        public void reconnect() {

        }

        @Override
        public void stop() {
            if (mCamera != null) {
                mCamera.stopStream();
                boolean isLedOn = isLedEnable();
                if (isLedOn) {
                    setLedEnable(false);
                    mLedEnable = isLedOn;
                }
            }
            if (mOpenGlView != null) {
                mOpenGlView.getHolder().removeCallback(mSurfaceCallback);
            }
        }

        @Override
        public void callDisconnect() {

        }

        @Override
        public boolean isRunning() {
            if (mCamera != null) {
                return mCamera.isStreaming();
            } else
                return false;

        }

        @Override
        public boolean isAudioEnable() {
            if (mCamera != null)
                return !mCamera.isAudioMuted();
            return false;
        }

        @Override
        public void setAudioEnable(boolean enable) {
            if (mCamera != null) {
                if (enable)
                    mCamera.enableAudio();
                else mCamera.disableAudio();
            }
        }

        @Override
        public boolean setLedEnable(boolean enable) {
            if (mCamera != null) {
                try {
                    if (enable)
                        mCamera.enableLantern();
                    else mCamera.disableLantern();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return mLedEnable = isLedEnable();
            }
            return false;

        }

        @Override
        public boolean isLedEnable() {
            if (mCamera != null) {
                return mCamera.isLanternEnabled();
            } else {
                return false;
            }

        }

        @Override
        public boolean isAudioPrepared() {
            if (mCamera != null)
                return mCamera.prepareAudio();
            return false;
        }

        @Override
        public boolean isVideoPrepared() {
            if (mCamera != null)
                return mCamera.prepareVideo();
            return false;
        }

        @Override
        public void startPreview() {
            if (mCamera != null)
                mCamera.startPreview(mFace.getValue(), mWidth, mHeight, mFps);
        }

        @Override
        public void stopPreview() {
            if (mCamera != null)
                mCamera.stopPreview();
        }

        @Override
        public void refresh() {
            FullLog.LogD("CameraSourceRefresh=>", "123");
            List<Camera.Size> sizes = mFace == CameraFace.Back ? mCamera.getResolutionsBack() : mCamera.getResolutionsFront();
            Camera.Size found = sizes.get(0);
            int width = mResolution.getWidth();
            int height = mResolution.getHeight();
            int dxf = Math.abs(found.width - width);
            int dyf = Math.abs(found.height - height);
            for (int i = 0; i < sizes.size(); i++) {
                Camera.Size size = sizes.get(i);
                int dx = Math.abs(size.width - width);
                int dy = Math.abs(size.height - height);
                if (dy < dyf || dy == dyf && dx < dxf) {
                    dxf = dx;
                    dyf = dy;
                    found = size;
                    FullLog.LogD("camerasize=>", found.width + "_" + found.height + "_" + width + "_" + height);
                }
            }
            FullLog.LogD("camerasize=>final", found.width + "_" + found.height + "_" + width + "_" + height);
            mWidth = found.width;
            mHeight = found.height;
//            mWidth = width;
//            mHeight = height;
        }

        public boolean isVideoEnable() {
            if (mOpenGlView != null)
                return mOpenGlView.isVideoEnable();
            return false;
        }

        public void setVideoEnable(boolean enable) {
            if (mOpenGlView != null)
                mOpenGlView.setVideoEnable(enable);

        }

        public void captureImage(final ImageCaptureListener listener) {
            if (mOpenGlView != null)
                mOpenGlView.takePhoto(new TakePhotoCallback() {
                    @Override
                    public void onTakePhoto(final Bitmap bitmap) {
                        try {
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onImageCaptured(bitmap);
                                }
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
        }
    }


    public boolean checkCameraAvailable(Context context) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIds = cameraManager.getCameraIdList();

            for (String cameraId : cameraIds) {
                boolean isCameraAvailable = cameraManager.getCameraCharacteristics(cameraId)
                        .get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                        != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
                if (!isCameraAvailable) {
                    return false;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return isCameraAvailable();
    }

    private boolean isCameraAvailable() {
        try {
            Camera camera = Camera.open();
            camera.release();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public interface CameraAvailableListener {
        void onCameraState(boolean isCameraAvailable);
    }

    public interface PreviewSizeListener {
        void onPreviewSize(int width, int height);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


}
