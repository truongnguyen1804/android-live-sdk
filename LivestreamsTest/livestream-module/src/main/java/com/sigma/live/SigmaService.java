package com.sigma.live;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class SigmaService extends Service {
    private String channelId = "hwmChannel";
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        keepAliveTrick();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void keepAliveTrick() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            Notification notification = new Notification.Builder(this, channelId)
                    .setOngoing(true)
                    .setContentTitle("")
                    .setContentText("").build();
            startForeground(1, notification);
        } else {
            startForeground(1, new Notification());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        if ((LiveManager.ScreenSource) LiveManager.mVideoSource != null) {
//            ((LiveManager.ScreenSource) LiveManager.mVideoSource).startLive();
//        }
//        keepAliveTrick();
        return START_STICKY;
    }

    @Override
    public boolean stopService(Intent name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.stopForeground(Service.STOP_FOREGROUND_REMOVE);
        }
        return super.stopService(name);
    }

}

