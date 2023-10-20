package net.ossrs.rtmp;

/**
 * Created by pedro on 25/01/17.
 */

public interface ConnectCheckerRtmp {

    void onConnectingRtmp();
    void onConnectionSuccessRtmp();

    void onConnectionFailedRtmp(String reason);

    void onConnectionStartedRtmp(String s);

    void onDisconnectRtmp();

    void onAuthErrorRtmp();

    void onAuthSuccessRtmp();

    void onNewBitrateRtmp(long bitrate);
}
