package com.sigma.live;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sigma.FullLog;

public class NetworkChangeReceiver  extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {

        int status = NetworkUtil.getConnectivityStatusString(context);
        Log.e("Sulod sa network", "Sulod sa network reciever");
        if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
            if (status == NetworkUtil.NETWORK_STATUS_NOT_CONNECTED) {
                FullLog.LogD("onReceive: "+ "NETWORK_STATUS_NOT_CONNECTED");
            } else {
                LiveManager.getInstance().reconnect();
            }
        }
    }
}