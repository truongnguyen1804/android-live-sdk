package com.tdm.livestreamstest;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSourceFactory;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SimpleExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.hls.playlist.DefaultHlsPlaylistParserFactory;
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.exoplayer.video.VideoFrameMetadataListener;
import androidx.media3.ui.PlayerView;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.pedro.rtplibrary.view.OpenGlView;
import com.sigma.FullLog;
import com.sigma.live.LiveListener;
import com.sigma.live.LiveManager;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;


@UnstableApi
public class SourceActivity extends AppCompatActivity {
    PlayerView playerView;

    ExoPlayer player;

    SimpleExoPlayer simpleExoPlayer;

    private static final String TAG = "ExoPlayerActivity";
    private static final int FRAME_WIDTH = 1280;
    private static final int FRAME_HEIGHT = 720;

    private ImageView imageView;

    private ImageReader imageReader;
    private Handler imageReaderHandler;

    private SurfaceView surfaceView;
    private OpenGlView openGlView;

    ImageView imgPlay, imgMic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_source);
        imageView = findViewById(R.id.imageview_pre);
        surfaceView = findViewById(R.id.sfv_player);
        openGlView = findViewById(R.id.op_glv);

        imgPlay = findViewById(R.id.img_live);
        imgMic = findViewById(R.id.img_mic);

        // Khởi tạo PlayerView
        playerView = findViewById(R.id.player);
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this);

        // Khởi tạo SimpleExoPlayer
        player = new ExoPlayer.Builder(this).build();


        HandlerThread imageReaderThread = new HandlerThread("ImageReaderThread");
        imageReaderThread.start();
        imageReaderHandler = new Handler(imageReaderThread.getLooper());
        imageReader = ImageReader.newInstance(FRAME_WIDTH, FRAME_HEIGHT, ImageFormat.YUV_420_888, 2);
        imageReader.setOnImageAvailableListener(onImageAvailableListener, imageReaderHandler);
        player.setVideoFrameMetadataListener(new VideoFrameMetadataListener() {
            @Override
            public void onVideoFrameAboutToBeRendered(long presentationTimeUs, long releaseTimeNs, Format format, @Nullable MediaFormat mediaFormat) {

            }
        });

        player.setVideoSurfaceView(openGlView);


        Uri mediaUri = Uri.parse("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8");

        String userAgent = Util.getUserAgent(this, "ExoPlayerDemo");
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this, userAgent);

        HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mediaUri));

        player.prepare(hlsMediaSource);
        player.setPlayWhenReady(true);

/*        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {

                    FullLog.LogD("onFrameAvailable:   " + surfaceTexture.toString());

            }
        });*/



        try {
            LiveManager.getInstance().setupStreamFile(openGlView, this, null,
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
                    });


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        imgPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MainActivity.checkPermission(SourceActivity.this)) {
                    if (LiveManager.getInstance().isRunning()) {
                        LiveManager.getInstance().stop();
                        imgPlay.setImageResource(R.drawable.ic_play_arrow);
                    } else {
                        LiveManager.getInstance().start("rtmp://live.twitch.tv/app/live_926961558_dieFvvmelCjlO7ghTejMDm1WGXytCk");
                    }
                } else {
                    Toast.makeText(SourceActivity.this, "Cần cấp quyền", Toast.LENGTH_LONG).show();
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

    }

    private VideoFrameMetadataListener videoFrameMetadataListener = (presentationTimeUs, releaseTimeNs, format, mediaFormat) -> {
        FullLog.LogD("videoFrameMetadataListener: " + presentationTimeUs);
        Image image = imageReader.acquireLatestImage();

        if (image != null) {
            FullLog.LogD("videoFrameMetadataListener: " + image.getHeight());
            Bitmap bitmap = imageToBitmap(image);
            imageView.setImageBitmap(bitmap);
            image.close();
        }
    };

    private Bitmap imageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        int width = image.getWidth();
        int height = image.getHeight();
        int yStride = planes[0].getRowStride();
        int uvStride = planes[1].getRowStride();
        int uvPixelStride = planes[1].getPixelStride();

        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uvBuffer = planes[1].getBuffer();

        byte[] data = new byte[width * height * 3 / 2];
        int yPos = 0;
        int uvPos = 0;
        for (int row = 0; row < height; row++) {
            yBuffer.position(yPos);
            yBuffer.get(data, yPos, width);
            yPos += yStride;

            if (row % 2 == 0) {
                uvBuffer.position(uvPos);
                uvBuffer.get(data, yPos, width / 2);
                uvPos += uvStride;
            }
            yPos += width;
        }

        // Tạo Bitmap từ dữ liệu ảnh YUV
        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, outputStream);
        byte[] jpegArray = outputStream.toByteArray();
        return BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.length);
    }

    // Lắng nghe sự kiện khi có hình ảnh mới sẵn sàng từ ImageReader
    private ImageReader.OnImageAvailableListener onImageAvailableListener = reader -> {
        Image image = reader.acquireLatestImage();
        if (image != null) {
            FullLog.LogD("videoFrameMetadataListener: " + image.getHeight());
            Bitmap bitmap = imageToBitmap(image);
            imageView.setImageBitmap(bitmap);
            image.close();
        }
    };

}