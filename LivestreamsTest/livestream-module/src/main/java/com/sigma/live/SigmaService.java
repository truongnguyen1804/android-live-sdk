package com.sigma.live;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import com.sigma.FullLog;

public class SigmaService extends Service {
    private String channelId = "hwmChannel";
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        FullLog.LogD("onTaskRemoved onCreate");
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        keepAliveTrick();
    }

    @Override
    public void onDestroy() {
        FullLog.LogD("onTaskRemoved onDestroy");
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
        FullLog.LogD("onTaskRemoved onBind");
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
        FullLog.LogD("onTaskRemoved stopService");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.stopForeground(Service.STOP_FOREGROUND_REMOVE);
        }
        return super.stopService(name);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
//        System.out.println("onTaskRemoved called");
        FullLog.LogD("onTaskRemoved called");
        LiveManager.getInstance().stop();
        super.onTaskRemoved(rootIntent);
        this.stopSelf();
    }

    @Override
    public void onTrimMemory(int level) {
        FullLog.LogD("onTrimMemory called");
        super.onTrimMemory(level);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        FullLog.LogD("onUnbind called");
        return super.onUnbind(intent);
    }

    @Override
    public void onLowMemory() {
        FullLog.LogD("onLowMemory called");
        super.onLowMemory();
    }

    @Override
    public void onRebind(Intent intent) {
        FullLog.LogD("onRebind called");
        super.onRebind(intent);
    }

}

