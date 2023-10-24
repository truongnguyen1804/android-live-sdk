package com.sigma.live;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sigma.FullLog;

public class AppCloseReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        FullLog.LogD("AppCloseReceiver " );
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
            String packageName = intent.getData().getEncodedSchemeSpecificPart();
            // Kiểm tra xem ứng dụng nào đã bị xóa
            FullLog.LogD("AppCloseReceiver " + packageName);
            if (packageName != null) {
                // Xử lý sự kiện ở đây
            }
        }
    }
}