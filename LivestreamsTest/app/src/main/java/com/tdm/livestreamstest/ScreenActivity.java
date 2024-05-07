package com.tdm.livestreamstest;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.pedro.rtplibrary.view.OpenGlView;
import com.sigma.FullLog;
import com.sigma.live.LiveListener;
import com.sigma.live.LiveManager;

public class ScreenActivity extends AppCompatActivity {
    ImageView imgPlay, imgMic, imgCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen);

        imgPlay = findViewById(R.id.img_live);
        imgMic = findViewById(R.id.img_mic);
        imgCamera = findViewById(R.id.img_camera);
        LiveManager.getInstance().setOrientation(0);
        findViewById(R.id.img_refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LiveManager.getInstance().reconnect();
            }
        });
        findViewById(R.id.img_call_dis).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LiveManager.getInstance().callDisconnect();
            }
        });

        imgPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FullLog.LogD("setOnClickListener");
                if (MainActivity.checkPermission(ScreenActivity.this)) {
                    if (LiveManager.getInstance().isRunning()) {
                        FullLog.LogD("setOnClickListener1");
                        LiveManager.getInstance().stop();
                        imgPlay.setImageResource(R.drawable.ic_play_arrow);
                    } else {
                        FullLog.LogD("setOnClickListener2");
//                        LiveManager.getInstance().start("rtmp://live.twitch.tv/app/live_926961558_dieFvvmelCjlO7ghTejMDm1WGXytCk");
                        LiveManager.getInstance().start("rtmp://txpush.rtmp.nimo.tv/live/su1639523573342r3b197b8551792127fde469b0116d75c2?guid=0a476dc4b1122b626d016a656b65ec9e&hyapp=81&hymuid=1639523573342&hyroom=84486504&psign=de7065700ae1cc5b1b461c16a36f674d&rtag=hIIe99AmLd&sru=9OU5DHQH1&txHost=txpush.rtmp.nimo.tv&ua=d2ViJjEuMC40Jm5pbW9UVg==&appid=81&room=84486504&muid=3279047133623&seq=1712128719662&streamcode=huya_inner_user");
//                        LiveManager.getInstance().start("rtmp://ingest-ottcore.vtccore.com:1935/origin/45ca370b366340fcbc09af1663917010");

                    }
                } else {
                    Toast.makeText(ScreenActivity.this, "Cần cấp quyền", Toast.LENGTH_LONG).show();
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
                    LiveManager.getInstance().setVideoEnable(false);
                    imgCamera.setImageResource(R.drawable.ic_videocam_off);
                } else {
                    LiveManager.getInstance().setVideoEnable(true);
                    imgCamera.setImageResource(R.drawable.ic_videocam);
                }
            }
        });

        try {
//            if (LiveManager.mVideoSource == null)
            LiveManager.getInstance().setInfoLive(500, 720, 10000, 30);
                LiveManager.getInstance().setupScreenStream(this, findViewById(R.id.view_screen),
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
                                Log.d("LiveListener=>", "onLiveError");
                                imgPlay.setImageResource(R.drawable.ic_play_arrow);
                            }

                            @Override
                            public void onLiveStopped() {
                                Log.d("LiveListener=>", "onLiveStopped");
                                imgPlay.setImageResource(R.drawable.ic_play_arrow);
                            }

                            @Override
                            public void onDisConnect() {
                                Log.d("LiveListener=>", "onDisConnect");
                            }

                            @Override
                            public void onConnectFailed(Exception err) {
                                Log.d("LiveListener=>", "onConnectFailed");
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
                        });
            Bitmap bitmap = ((BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.muoidiem)).getBitmap();
            LiveManager.getInstance().setWaitingImage(bitmap);

        } catch (Exception e) {
            Log.d("Exception", e.getMessage());
            throw new RuntimeException(e);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LiveManager.getInstance().onActivityResult(requestCode, resultCode, data);
        Bitmap bitmap = ((BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.muoidiem)).getBitmap();
        LiveManager.getInstance().setWaitingImage(bitmap);
    }

    @Override
    protected void onResume() {
        FullLog.LogD("checkResume");
        super.onResume();
    }
}