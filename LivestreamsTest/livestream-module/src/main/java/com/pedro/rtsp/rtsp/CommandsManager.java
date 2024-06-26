package com.pedro.rtsp.rtsp;

import android.util.Base64;
import android.util.Log;

import com.pedro.rtsp.utils.AuthUtil;
import com.pedro.rtsp.utils.ConnectCheckerRtsp;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by pedro on 12/02/19.
 * <p>
 * Class to create request to server and parse response from server.
 */

public class CommandsManager {

    private static final String TAG = "CommandsManager";
    private static String authorization = null;
    //For only audio
    private final String defaultSps = "Z0KAHtoHgUZA";
    private final String defaultPps = "aM4NiA==";
    private String host;
    private int port;
    private String path;
    private byte[] sps;
    private byte[] pps;
    private int cSeq = 0;
    private String sessionId;
    private long timeStamp;
    private int sampleRate = 32000;
    private boolean isStereo = true;
    private int trackAudio = 0;
    private int trackVideo = 1;
    private Protocol protocol;
    //For udp
    private int[] audioPorts = new int[]{5000, 5001};
    private int[] videoPorts = new int[]{5002, 5003};
    private byte[] vps; //For H265
    //For auth
    private String user;
    private String password;

    public CommandsManager() {
        protocol = Protocol.TCP;
        long uptime = System.currentTimeMillis();
        timeStamp = (uptime / 1000) << 32 & (((uptime - ((uptime / 1000) * 1000)) >> 32)
                / 1000); // NTP timestamp
    }

    public static String createPause() {
        return "";
    }

    public static String createPlay() {
        return "";
    }

    public static String createGetParameter() {
        return "";
    }

    public static String createSetParameter() {
        return "";
    }

    public static String createRedirect() {
        return "";
    }

    private byte[] getData(ByteBuffer byteBuffer) {
        if (byteBuffer != null) {
            byte[] bytes = new byte[byteBuffer.capacity() - 4];
            byteBuffer.position(4);
            byteBuffer.get(bytes, 0, bytes.length);
            return bytes;
        } else {
            return null;
        }
    }

    private String encodeToString(byte[] bytes) {
        return Base64.encodeToString(bytes, 0, bytes.length, Base64.NO_WRAP);
    }

    public void setVideoInfo(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps) {
        this.sps = getData(sps);
        this.pps = getData(pps);
        this.vps = getData(vps);  //H264 has no vps so if not null assume H265
    }

    public void setIsStereo(boolean isStereo) {
        this.isStereo = isStereo;
    }

    public void setAuth(String user, String password) {
        this.user = user;
        this.password = password;
    }

    public void setUrl(String host, int port, String path) {
        this.host = host;
        this.port = port;
        this.path = path;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public byte[] getSps() {
        return sps;
    }

    public byte[] getPps() {
        return pps;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public boolean isStereo() {
        return isStereo;
    }

    public int getTrackAudio() {
        return trackAudio;
    }

    public int getTrackVideo() {
        return trackVideo;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public byte[] getVps() {
        return vps;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public int[] getAudioPorts() {
        return audioPorts;
    }

    public int[] getVideoPorts() {
        return videoPorts;
    }

    public void clear() {
        cSeq = 0;
        sps = null;
        pps = null;
        sessionId = null;
    }

    private String getSpsString() {
        return sps != null ? encodeToString(sps) : defaultSps;
    }

    //Commands

    private String getPpsString() {
        return pps != null ? encodeToString(pps) : defaultPps;
    }

    private String getVpsString() {
        return encodeToString(vps);
    }

    private String addHeaders() {
        return "CSeq: " + (++cSeq) + "\r\n" + (sessionId != null ? "Session: " + sessionId + "\r\n"
                : "") + (authorization != null ? "Authorization: " + authorization + "\r\n" : "") + "\r\n";
    }

    private String createBody() {
        String videoBody = vps == null ? Body.createH264Body(trackVideo, getSpsString(), getPpsString())
                : Body.createH265Body(trackVideo, getSpsString(), getPpsString(), getVpsString());
        return "v=0\r\n"
                + "o=- "
                + timeStamp
                + " "
                + timeStamp
                + " IN IP4 "
                + "127.0.0.1"
                + "\r\n"
                + "s=Unnamed\r\n"
                + "i=N/A\r\n"
                + "c=IN IP4 "
                + host
                + "\r\n"
                + "t=0 0\r\n"
                + "a=recvonly\r\n"
                + videoBody
                + Body.createAacBody(trackAudio, sampleRate, isStereo);
    }

    private String createAuth(String authResponse) {
        Pattern authPattern =
                Pattern.compile("realm=\"(.+)\",\\s+nonce=\"(\\w+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = authPattern.matcher(authResponse);
        //digest auth
        if (matcher.find()) {
            Log.i(TAG, "using digest auth");
            String realm = matcher.group(1);
            String nonce = matcher.group(2);
            String hash1 = AuthUtil.getMd5Hash(user + ":" + realm + ":" + password);
            String hash2 = AuthUtil.getMd5Hash("ANNOUNCE:rtsp://" + host + ":" + port + path);
            String hash3 = AuthUtil.getMd5Hash(hash1 + ":" + nonce + ":" + hash2);
            return "Digest username=\""
                    + user
                    + "\",realm=\""
                    + realm
                    + "\",nonce=\""
                    + nonce
                    + "\",uri=\"rtsp://"
                    + host
                    + ":"
                    + port
                    + path
                    + "\",response=\""
                    + hash3
                    + "\"";
            //basic auth
        } else {
            Log.i(TAG, "using basic auth");
            String data = user + ":" + password;
            String base64Data = Base64.encodeToString(data.getBytes(), Base64.DEFAULT);
            return "Basic " + base64Data;
        }
    }

    public String createOptions() {
        String options = "OPTIONS rtsp://" + host + ":" + port + path + " RTSP/1.0\r\n" + addHeaders();
        Log.i(TAG, options);
        return options;
    }

    //Unused commands

    public String createSetup(int track) {
        String params =
                (protocol == Protocol.UDP) ? ("UDP;unicast;client_port=" + (5000 + 2 * track) + "-" + (5000
                        + 2 * track
                        + 1) + ";mode=record")
                        : ("TCP;interleaved=" + 2 * track + "-" + (2 * track + 1) + ";mode=record");
        String setup = "SETUP rtsp://"
                + host
                + ":"
                + port
                + path
                + "/trackID="
                + track
                + " RTSP/1.0\r\n"
                + "Transport: RTP/AVP/"
                + params
                + "\r\n"
                + addHeaders();
        Log.i(TAG, setup);
        return setup;
    }

    public String createRecord() {
        String record = "RECORD rtsp://"
                + host
                + ":"
                + port
                + path
                + " RTSP/1.0\r\n"
                + "Range: npt=0.000-\r\n"
                + addHeaders();
        Log.i(TAG, record);
        return record;
    }

    public String createAnnounce() {
        String body = createBody();
        String announce = "ANNOUNCE rtsp://"
                + host
                + ":"
                + port
                + path
                + " RTSP/1.0\r\n"
                + "CSeq: "
                + (++cSeq)
                + "\r\n"
                + "Content-Length: "
                + body.length()
                + "\r\n"
                + (authorization == null ? "" : "Authorization: " + authorization + "\r\n")
                + "Content-Type: application/sdp\r\n\r\n"
                + body;
        Log.i(TAG, announce);
        return announce;
    }

    public String createAnnounceWithAuth(String authResponse) {
        authorization = createAuth(authResponse);
        Log.i("Auth", authorization);
        String body = createBody();
        String announceAuth = "ANNOUNCE rtsp://"
                + host
                + ":"
                + port
                + path
                + " RTSP/1.0\r\n"
                + "CSeq: "
                + (++cSeq)
                + "\r\n"
                + "Content-Length: "
                + body.length()
                + "\r\n"
                + "Authorization: "
                + authorization
                + "\r\n"
                + "Content-Type: application/sdp\r\n\r\n"
                + body;
        Log.i(TAG, announceAuth);
        return announceAuth;
    }

    public String createTeardown() {
        String teardown =
                "TEARDOWN rtsp://" + host + ":" + port + path + " RTSP/1.0\r\n" + addHeaders();
        Log.i(TAG, teardown);
        return teardown;
    }

    //Response parser

    public String getResponse(BufferedReader reader, ConnectCheckerRtsp connectCheckerRtsp,
                              boolean isAudio, boolean checkStatus) {
        try {
            String response = "";
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("Session")) {
                    Pattern rtspPattern = Pattern.compile("Session: (\\w+)");
                    Matcher matcher = rtspPattern.matcher(line);
                    if (matcher.find()) {
                        sessionId = matcher.group(1);
                    }
                    sessionId = line.split(";")[0].split(":")[1].trim();
                }
                if (line.contains("server_port")) {
                    Pattern rtspPattern = Pattern.compile("server_port=([0-9]+)-([0-9]+)");
                    Matcher matcher = rtspPattern.matcher(line);
                    if (matcher.find()) {
                        if (isAudio) {
                            audioPorts[0] = Integer.parseInt(matcher.group(1));
                            audioPorts[1] = Integer.parseInt(matcher.group(2));
                        } else {
                            videoPorts[0] = Integer.parseInt(matcher.group(1));
                            videoPorts[1] = Integer.parseInt(matcher.group(2));
                        }
                    }
                }
                response += line + "\n";
                //end of response
                if (line.length() < 3) break;
            }
            if (checkStatus && getResponseStatus(response) != 200) {
                connectCheckerRtsp.onConnectionFailedRtsp("Error configure stream, " + response);
            }
            Log.i(TAG, response);
            return response;
        } catch (IOException e) {
            Log.e(TAG, "read error", e);
            return null;
        }
    }

    public int getResponseStatus(String response) {
        Matcher matcher =
                Pattern.compile("RTSP/\\d.\\d (\\d+) (\\w+)", Pattern.CASE_INSENSITIVE).matcher(response);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        } else {
            return -1;
        }
    }
}
