package com.github.faucamp.simplertmp;

import android.util.Log;

import com.github.faucamp.simplertmp.io.RtmpConnection;
import com.sigma.FullLog;

import net.ossrs.rtmp.ConnectCheckerRtmp;

/**
 * Srs implementation of an RTMP publisher
 *
 * @author francois, leoma, pedro
 */
public class DefaultRtmpPublisher implements RtmpPublisher {

    private RtmpConnection rtmpConnection;

    public DefaultRtmpPublisher(ConnectCheckerRtmp connectCheckerRtmp) {
        rtmpConnection = new RtmpConnection(connectCheckerRtmp);
    }

    @Override
    public boolean connect(String url) {
        return rtmpConnection.connect(url);
    }

    @Override
    public boolean publish(String publishType) {
        return rtmpConnection.publish(publishType);
    }

    @Override
    public void close() {
        rtmpConnection.close();
    }

    @Override
    public void publishVideoData(byte[] data, int size, int dts) {
        FullLog.LogD("SrsFlvMuxer", "publishVideoData: 3 " + size + " -- " + dts);
        rtmpConnection.publishVideoData(data, size, dts);
    }

    @Override
    public void publishAudioData(byte[] data, int size, int dts) {
        rtmpConnection.publishAudioData(data, size, dts);
    }

    @Override
    public void setVideoResolution(int width, int height) {
        rtmpConnection.setVideoResolution(width, height);
    }

    public void setFps(int fps) {
        rtmpConnection.setFps(fps);
    }

    @Override
    public void setAuthorization(String user, String password) {
        rtmpConnection.setAuthorization(user, password);
    }
}
