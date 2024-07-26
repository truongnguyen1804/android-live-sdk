package com.tdm.livestreamstest;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.pedro.rtplibrary.view.OpenGlView;
import com.sigma.Common;
import com.sigma.FullLog;
import com.sigma.live.CameraFace;
import com.sigma.live.LiveListener;
import com.sigma.live.LiveManager;
import com.sigma.live.Resolution;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class CameraActivity extends AppCompatActivity {

    ImageView imgPlay, imgMic, imgCamera, imgSwitchCam;
    boolean scaleX = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        imgPlay = findViewById(R.id.img_live);
        imgMic = findViewById(R.id.img_mic);
        imgCamera = findViewById(R.id.img_camera);
        imgSwitchCam = findViewById(R.id.img_change_cam);


        LiveManager.getInstance().setOrientation(90);
        // ngang bên trai
        imgSwitchCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LiveManager.getInstance().switchCameraFace();
                FullLog.LogD("CheckCamFont :" + LiveManager.getInstance().isFontCam());
            }
        });

        imgPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MainActivity.checkPermission(CameraActivity.this)) {
                    if (LiveManager.getInstance().isRunning()) {
//                        LiveManager.getInstance().stop();
//                        imgPlay.setImageResource(R.drawable.ic_play_arrow);
//                        LiveManager.getInstance().setInfoLive(500, 10000, 10000, 30);
                    } else {
//                        LiveManager.getInstance().setInfoLive(500, 600, 10000, 5);

//                        LiveManager.getInstance().setResolution(Resolution.HD15);
//                        LiveManager.getInstance().start("rtmp://live.twitch.tv/app/live_926961558_dieFvvmelCjlO7ghTejMDm1WGXytCk");
                        LiveManager.getInstance().start("rtmp://live.twitch.tv/app/live_926961558_dieFvvmelCjlO7ghTejMDm1WGXytCk");
//                        LiveManager.getInstance().start("rtmp://live.ori2.vtc.vn:1935/origin2/f7d3966bc1244ad8ba658395bf1313b6");
//                        LiveManager.getInstance().start("rtmp://live.twitch.tv/app/live_162311279_a2TQ6agKAohE6VyCuwFO6zq5xC74FJ");
//                        LiveManager.getInstance().start("rtmp://ingest-ottcore.vtccore.com:1935/origin/b8097ccc699b46a3bd05a1690f95977d");

                    }
                } else {
                    Toast.makeText(CameraActivity.this, "Cần cấp quyền", Toast.LENGTH_LONG).show();
                }
            }
        });
        imgMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (LiveManager.getInstance().isAudioEnable()) {
                    LiveManager.getInstance().setAudioEnable(false);
                    imgMic.setImageResource(R.drawable.ic_mic_off);
                } else {
                    LiveManager.getInstance().setAudioEnable(true);
                    imgMic.setImageResource(R.drawable.ic_mic);
                }
            }
        });
        imgCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (LiveManager.getInstance().isVideoEnable()) {
//                    LiveManager.getInstance().setVideoEnable(false);
                    LiveManager.getInstance().pause(false);
                    imgCamera.setImageResource(R.drawable.ic_videocam_off);
                } else {
//                    LiveManager.getInstance().setVideoEnable(true);
                    imgCamera.setImageResource(R.drawable.ic_videocam);
                    LiveManager.getInstance().pause(true);
                }
            }
        });


        findViewById(R.id.img_library).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent intent = new Intent(Intent.ACTION_PICK);
//                intent.setType("image/*");
//                startActivityForResult(intent, 1);
                scaleX = !scaleX;
                LiveManager.getInstance().scaleSurfaceView(scaleX);

            }
        });


    }

    @Override
    protected void onResume() {
        FullLog.LogD("checkResume");
        super.onResume();
        try {
            LiveManager.getInstance().setInfoLive(1280, 768, 10000, 60);
            LiveManager.getInstance().setup(this, findViewById(R.id.view_screen), test,
                    new LiveListener() {
                        @Override
                        public void onLiveStarting() {
                            Log.d("LiveListener=>", "onLiveStarting");
                        }

                        @Override
                        public void onLiveStarted() {
                            Log.d("LiveListener=>", "onLiveStarted");
                            imgPlay.setImageResource(R.drawable.ic_pause);

                        }

                        @Override
                        public void onLiveError(Exception ex) {
                            Log.d("LiveListener=>", "onLiveError " + ex);
                            imgPlay.setImageResource(R.drawable.ic_play_arrow);
                        }

                        @Override
                        public void onLiveStopped() {
                            Log.d("LiveListener=>", "onLiveStopped");
                            imgPlay.setImageResource(R.drawable.ic_play_arrow);
                        }

                        @Override
                        public void onDisConnect() {

                        }

                        @Override
                        public void onConnectFailed(Exception err) {

                        }

                        @Override
                        public void onConnectionStarted() {
                            Log.d("LiveListener=>", "onConnectionStarted");
                        }

                        @Override
                        public void onNewBitrateReceived(long b) {
                        }

                        @Override
                        public void onPermissionDenied() {

                        }

                        @Override
                        public void onPrepareError(Exception ex) {

                        }
                    }, true,
                    new LiveManager.PreviewSizeListener() {
                        @Override
                        public void onPreviewSize(int width, int height) {
                            FullLog.LogD("onPreviewSize width: " + width + " -- " + "heigh: " + height);

                        }
                    },
                    new OpenGlView.SurfaceListener() {
                        @Override
                        public void onCreated() {

                        }

                        @Override
                        public void onDestroyed() {

                        }

                        @Override
                        public void onChanged() {

                        }

                        @Override
                        public void onError(Exception e) {
                            FullLog.LogD("onRtmpConnecting");
                        }

                        @Override
                        public void onSurfaceInvalid(Exception e) {

                        }
                    });


            LiveManager.getInstance().setSurfaceViewParams(Common.TypePivot.LEFT, 1080, 1000, 0, 0, 2000);

//            Bitmap bitmap = ((BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.okokoo)).getBitmap();
//            LiveManager.getInstance().setWaitingImage(bitmap);
//            Bitmap bitmap = getBitmapFromURL("https://cdn.pixabay.com/photo/2023/06/10/15/24/insect-8054262_1280.jpg");
            /*         Glide.with(this).asBitmap().load(*//*"https://cdn.pixabay.com/photo/2023/06/10/15/24/insect-8054262_1280.jpg"*//*"https://imgtr.ee/images/2023/07/28/66ee28f55d70fe0167574c9205dfb2ca.jpeg").into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    Bitmap bitmap = resource;
                    LiveManager.getInstance().setWaitingImage(bitmap);
                }
            });*/

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
//        LiveManager.getInstance().start("rtmp://live.twitch.tv/app/live_162311279_a2TQ6agKAohE6VyCuwFO6zq5xC74FJ");
//
//        for (int i = 0; i<20; i++){
//            loopSetup.run();
//        }

    }

    Runnable loopSetup = new Runnable() {
        @Override
        public void run() {
            LiveManager.getInstance().stopPreview();
            LiveManager.getInstance().stop();

            try {
                LiveManager.getInstance().setup(CameraActivity.this, findViewById(R.id.view_screen), test,
                        new LiveListener() {
                            @Override
                            public void onLiveStarting() {
                                Log.d("LiveListener=>", "onLiveStarting");
                            }

                            @Override
                            public void onLiveStarted() {
                                Log.d("LiveListener=>", "onLiveStarted");
                                imgPlay.setImageResource(R.drawable.ic_pause);

                            }

                            @Override
                            public void onLiveError(Exception ex) {
                                Log.d("LiveListener=>", "onLiveError " + ex);
                                imgPlay.setImageResource(R.drawable.ic_play_arrow);
                            }

                            @Override
                            public void onLiveStopped() {
                                Log.d("LiveListener=>", "onLiveStopped");
                                imgPlay.setImageResource(R.drawable.ic_play_arrow);
                            }

                            @Override
                            public void onDisConnect() {

                            }

                            @Override
                            public void onConnectFailed(Exception err) {

                            }

                            @Override
                            public void onConnectionStarted() {
                                Log.d("LiveListener=>", "onConnectionStarted");
                            }

                            @Override
                            public void onNewBitrateReceived(long b) {
                            }

                            @Override
                            public void onPermissionDenied() {

                            }

                            @Override
                            public void onPrepareError(Exception ex) {

                            }
                        }, true, new LiveManager.PreviewSizeListener() {
                            @Override
                            public void onPreviewSize(int width, int height) {
                                FullLog.LogD("onPreviewSize width: " + width + " -- " + "heigh: " + height);

                            }
                        }, new OpenGlView.SurfaceListener() {
                            @Override
                            public void onCreated() {

                            }

                            @Override
                            public void onDestroyed() {

                            }

                            @Override
                            public void onChanged() {

                            }

                            @Override
                            public void onError(Exception e) {
                                FullLog.LogD("onRtmpConnecting");
                            }

                            @Override
                            public void onSurfaceInvalid(Exception e) {

                            }
                        });


//
                LiveManager.getInstance().setSurfaceViewParams(Common.TypePivot.LEFT, 1080, 1000, 0, 0, 2000);

//            Bitmap bitmap = ((BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.okokoo)).getBitmap();
//            LiveManager.getInstance().setWaitingImage(bitmap);
//            Bitmap bitmap = getBitmapFromURL("https://cdn.pixabay.com/photo/2023/06/10/15/24/insect-8054262_1280.jpg");
                /*         Glide.with(this).asBitmap().load(*//*"https://cdn.pixabay.com/photo/2023/06/10/15/24/insect-8054262_1280.jpg"*//*"https://imgtr.ee/images/2023/07/28/66ee28f55d70fe0167574c9205dfb2ca.jpeg").into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    Bitmap bitmap = resource;
                    LiveManager.getInstance().setWaitingImage(bitmap);
                }
            });*/

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    LiveManager.getInstance().start("rtmp://live.twitch.tv/app/live_162311279_a2TQ6agKAohE6VyCuwFO6zq5xC74FJ");
                }
            }, 2000);
        }
    };


    @Override
    public int getRequestedOrientation() {
        return super.getRequestedOrientation();
    }

    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            // Log exception
            return null;
        }
    }

    int[] test = {0, 0, 0, 0};

    int t = 0;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        FullLog.LogD("checkResume onActivityResult");
        if (requestCode == 1 && data != null) {
//            FullLog.LogD("onActivityResult: "+ resultCode+" -- "+data.getDataString());

            Uri imageUri = data.getData();

            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap selectedBitmap = BitmapFactory.decodeStream(inputStream);
                Bitmap rotatedBitmap = rotateBitmapIfNeeded(selectedBitmap, imageUri);
                LiveManager.getInstance().setWaitingImage(rotatedBitmap);
//                ((ImageView)findViewById(R.id.img_tets)).setImageBitmap(rotatedBitmap);

                // Bạn có thể sử dụng rotatedBitmap ở đây để hiển thị hoặc xử lý tiếp theo.
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
//            Bitmap selectedBitmap = getBitmapFromUri(imageUri);

        }
    }


    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            return BitmapFactory.decodeStream(inputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap rotateBitmapIfNeeded(Bitmap bitmap, Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);

            // Đọc thông tin Exif của ảnh
            ExifInterface exifInterface = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                exifInterface = new ExifInterface(inputStream);
            }
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
            );

            int rotationAngle = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotationAngle = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotationAngle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotationAngle = 270;
                    break;
                default:
                    // Không xoay nếu không xác định được góc xoay
                    return bitmap;
            }

            // Xoay ảnh sử dụng Matrix
            Matrix matrix = new Matrix();
            matrix.postRotate(rotationAngle);

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (IOException e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    @Override
    public void onBackPressed() {
//        LiveManager.getInstance().stop();
//        LiveManager.getInstance().stopPreview();
        super.onBackPressed();
    }

    @Override
    protected void onStop() {
//        LiveManager.getInstance().stopPreview();
        LiveManager.getInstance().stopPreview();
        LiveManager.getInstance().stop();
        super.onStop();
    }
}