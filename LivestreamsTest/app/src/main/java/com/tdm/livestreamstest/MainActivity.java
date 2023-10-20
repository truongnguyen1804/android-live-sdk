package com.tdm.livestreamstest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.pedro.rtplibrary.rtmp.RtmpCamera1;
import com.pedro.rtplibrary.view.OpenGlView;
import com.sigma.FullLog;
import com.sigma.live.LiveListener;
import com.sigma.live.LiveManager;
import com.sigma.live.SigmaService;

import net.ossrs.rtmp.ConnectCheckerRtmp;

public class MainActivity extends AppCompatActivity {
    TextView tvScreen, tvCamera, tvSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvCamera = findViewById(R.id.txt_camera);
        tvSource = findViewById(R.id.txt_source);
        tvScreen = findViewById(R.id.txt_screen);

//        Glide.with(this).load("https://cdn.pixabay.com/photo/2023/06/10/15/24/insect-8054262_1280.jpg").into((ImageView) findViewById(R.id.img_bg));

        tvCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermission(MainActivity.this)) {
                    startActivity(new Intent(MainActivity.this, CameraActivity.class));
                }
            }
        });
        tvSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermission(MainActivity.this)) {
                    startActivity(new Intent(MainActivity.this, SourceActivity.class));
                }
            }
        });
//        this.findViewById(android.R.id.content).addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
//            @Override
//            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
//                FullLog.LogD("onLayoutChange: " + view.getWidth() + " -- " + view.getHeight() +" -- "+ view.get+" -- "+ view.getPaddingRight()+" -- "+ view.getPaddingTop()+" -- "+ view.getPaddingBottom());
//            }
//        });
        tvScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermission(MainActivity.this)) {
                    startActivity(new Intent(MainActivity.this, ScreenActivity.class));
                }
            }
        });
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 100);
//        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
//        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
//        } else {
//            try {
//                LiveManager.getInstance().setupScreenStream(this, findViewById(R.id.view_screen), new LiveListener() {
//                    @Override
//                    public void onLiveStarting() {
//                        Log.d("LiveListener=>", "onLiveStarting");
//                    }
//
//                    @Override
//                    public void onLiveStarted() {
//                        Log.d("LiveListener=>", "onLiveStarted");
//                    }
//
//                    @Override
//                    public void onLiveError(Exception ex) {
//                        Log.d("LiveListener=>", "onLiveError");
//                    }
//
//                    @Override
//                    public void onLiveStopped() {
//                        Log.d("LiveListener=>", "onLiveStopped");
//                    }
//
//                    @Override
//                    public void onConnectionStarted() {
//                        Log.d("LiveListener=>", "onConnectionStarted");
//                    }
//
//                    @Override
//                    public void onNewBitrateReceived(long b) {
//                    }
//                });
//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        LiveManager.getInstance().start("rtmp://live.twitch.tv/app/live_926961558_dieFvvmelCjlO7ghTejMDm1WGXytCk");
//                    }
//                }, 3000);
//
//            } catch (Exception e) {
//                Log.d("Exception", e.getMessage());
//                throw new RuntimeException(e);
//            }
//
//        }
    }

    public static boolean checkPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return checkPermission33(activity);
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, 100);
        } else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, 100);
        } else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        } else {
            return true;
        }
        return false;
    }

    public static boolean checkPermission33(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, 100);
        } else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, 100);
        } /* else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE}, 100);
        }*/ else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_MEDIA_AUDIO}, 100);
        } else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 100);
        } else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_MEDIA_VIDEO}, 100);
        } else {
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("onActivityResult", "requestCode: " + requestCode + " -- " + "resultCode: " + resultCode);
//        Intent intent = new Intent(this, SigmaService.class);
//        intent.putExtra("requestCode",requestCode);
//        intent.putExtra("resultCode",resultCode);
//        intent.putExtra("data",data.getData());
//        startService(new Intent(this, SigmaService.class));
        LiveManager.getInstance().onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {

            FullLog.LogD("okoko " + grantResults[0]);
            // Kiểm tra xem người dùng đã cho phép quyền ghi vào bộ nhớ hay không
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                FullLog.LogD("okoko");
            } else {
                FullLog.LogD("not ok");
            }
        }
    }
}