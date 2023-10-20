package com.sigma.live;

public interface LiveListener {

    void onLiveStarting();

    void onLiveStarted();

    void onLiveError(Exception ex);

    void onLiveStopped();

    void onDisConnect();

    void onConnectFailed(Exception err);
    void onConnectionStarted();
    void onNewBitrateReceived(long b);

    void onPermissionDenied();
}