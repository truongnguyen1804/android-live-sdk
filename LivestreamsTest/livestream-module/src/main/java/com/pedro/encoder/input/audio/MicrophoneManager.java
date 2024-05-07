package com.pedro.encoder.input.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioRouting;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import com.github.faucamp.simplertmp.Util;
import com.pedro.encoder.audio.DataTaken;
import com.sigma.FullLog;
import com.sigma.live.KaraokeManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by pedro on 19/01/17.
 */

public class MicrophoneManager {

    private static final int BUFFER_SIZE = 2048;
    private final String TAG = "MicrophoneManager";
    private AudioRecord audioRecord;
    private GetMicrophoneData getMicrophoneData;
    private byte[] pcmBuffer = new byte[BUFFER_SIZE];
    private byte[] pcmBufferMuted = new byte[BUFFER_SIZE];
    private boolean running = false;
    private boolean created = false;

    //default parameters for microphone
    private int sampleRate = 32000; //hz
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int channel = AudioFormat.CHANNEL_IN_STEREO;
    private boolean muted = false;
    private AudioPostProcessEffect audioPostProcessEffect;
    private Thread thread;

    public MicrophoneManager(GetMicrophoneData getMicrophoneData) {
        this.getMicrophoneData = getMicrophoneData;
    }

    /**
     * Create audio record
     */
    public void createMicrophone() {
        createMicrophone(sampleRate, true, false, false, false);
        Log.i(TAG, "Microphone created, " + sampleRate + "hz, Stereo");
    }

    /**
     * Create audio record with params
     */

    int[] sources = new int[]{MediaRecorder.AudioSource.MIC, MediaRecorder.AudioSource.DEFAULT, MediaRecorder.AudioSource.CAMCORDER, MediaRecorder.AudioSource.VOICE_COMMUNICATION, MediaRecorder.AudioSource.VOICE_RECOGNITION};

    public void createMicrophone(int sampleRate, boolean isStereo, boolean echoCanceler,
                                 boolean noiseSuppressor, boolean autoGainControl) {
        this.sampleRate = sampleRate;
        if (!isStereo) channel = AudioFormat.CHANNEL_IN_MONO;
//        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channel, audioFormat,
//                getPcmBufferSize() * 4);
        for (int source : sources) {
            try {
                AudioRecord audioRecord2 = new AudioRecord(source, sampleRate, channel, audioFormat, getPcmBufferSize() * 4);
                if (audioRecord2.getState() != AudioRecord.STATE_INITIALIZED) {
                    audioRecord2 = null;
                }
                audioRecord = audioRecord2;
                FullLog.LogD("checkSourceAudio " + source);
            } catch (Exception exception) {
                audioRecord = null;
            }
            if (audioRecord != null) {
                break;
            }

        }

//        audioPostProcessEffect = new AudioPostProcessEffect(audioRecord.getAudioSessionId());
//        /*if (echoCanceler)*/ audioPostProcessEffect.enableEchoCanceler();
//       /* if (noiseSuppressor)*/ audioPostProcessEffect.enableNoiseSuppressor();
//        /*if (autoGainControl)*/ audioPostProcessEffect.enableAutoGainControl();
        String chl = (isStereo) ? "Stereo" : "Mono";
        Log.i(TAG, "Microphone created, " + sampleRate + "hz, " + chl);
        created = true;
    }

    /**
     * Start record and get data
     */
    public void start() {
        init();
        KaraokeManager.getInstance().start(channel == AudioFormat.CHANNEL_IN_STEREO ? 2 : 1, sampleRate, BUFFER_SIZE);
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running && !Thread.interrupted()) {
                    DataTaken dataTaken = read();
                    if (dataTaken != null) {
                        getMicrophoneData.inputPCMData(dataTaken.getPcmBuffer(), dataTaken.getSize());
                    } else {
                        running = false;
                    }
                }
            }
        });
        thread.start();
    }

    private void init() {
        if (audioRecord != null) {
            audioRecord.startRecording();
            running = true;
            Log.i(TAG, "Microphone started");
        } else {
            Log.e(TAG, "Error starting, microphone was stopped or not created, "
                    + "use createMicrophone() before start()");
        }
    }

    public void mute() {
        muted = true;
    }

    public void unMute() {
        muted = false;
    }

    public boolean isMuted() {
        return muted;
    }

    /**
     * @return Object with size and PCM buffer data
     */
    private DataTaken read() {

        int size = audioRecord.read(pcmBuffer, 0, pcmBuffer.length);
        if (size <= 0) {
            return null;
        }
        float volume = 1f;
        String manufacturer = android.os.Build.MANUFACTURER;
        if (manufacturer.equals("samsung")) {
            volume = 1.5f;
        } else {
            volume = 1.5f;
        }

        pcmBuffer = setVolume(pcmBuffer, volume);
        byte res[] = muted ? pcmBufferMuted : pcmBuffer;
        //res = KaraokeManager.getInstance().apply(res, size);

//        FullLog.LogD("bytelenght: "+ res.length);
//        for (byte value: res){
//            FullLog.LogD("bytevalue: "+value);
//
//        }

        return new DataTaken(res, size);
    }

    private static final int USHORT_MASK = (1 << 16) - 1;

    private byte[] setVolume(byte[] audioSamples, float volume) {

//        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
//            FullLog.LogD("checkByteODer " + "BIG_ENDIAN");
//        } else if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
//            FullLog.LogD("checkByteODer " + "LITTLE_ENDIAN");
//        }
//        FullLog.LogD("CheckByteOderAudio " + (ByteBuffer.wrap(audioSamples).order() == ByteOrder.BIG_ENDIAN ? " BIG_ENDIAN" : "LITTLE_ENDIAN"));
//        System.out.printf("\n \n \n");
//        for (int i = 0; i < audioSamples.length; i++) {
//            System.out.printf("%02x "+"\n", audioSamples[i] & 0xFF );
//        }


//        ByteBuffer inputBuffer = ByteBuffer.wrap(audioSamples).order(ByteOrder.BIG_ENDIAN);

//        ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
//
//        if (audioSamples.length < 2) {
//            return null;
//        }
//        if (audioSamples[0] > audioSamples[1]) {
//            byteOrder = ByteOrder.BIG_ENDIAN;
//            FullLog.LogD("checkByteODer " + "BIG_ENDIAN");
//        } else {
//            byteOrder = ByteOrder.LITTLE_ENDIAN;
//            FullLog.LogD("checkByteODer " + "LITTLE_ENDIAN");
//        }

        ByteBuffer inputBuffer = ByteBuffer.wrap(audioSamples).order(ByteOrder.LITTLE_ENDIAN);
        FullLog.LogD("checkByteODer " + inputBuffer.order());
        ByteBuffer outputBuffer = ByteBuffer.allocate(audioSamples.length).order(ByteOrder.LITTLE_ENDIAN);

        int sample;

        while (inputBuffer.hasRemaining()) {
            sample = (int) inputBuffer.getShort();
            if (Math.abs(sample) < (32767 / volume)) {
                sample *= volume;
                if (sample > 32767) {
                    sample = 32767;
                } else if (sample < -32768) {
                    sample = -32768;
                }
            }
            outputBuffer.putShort((short) sample);
        }
        return outputBuffer.array();
    }

    /**
     * Stop and release microphone
     */
    public void stop() {
        running = false;
        created = false;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(100);
            } catch (InterruptedException e) {
                thread.interrupt();
            }
        }
        thread = null;
        if (audioRecord != null) {
            audioRecord.setRecordPositionUpdateListener(null);
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
//        if (audioPostProcessEffect != null) {
//            audioPostProcessEffect.releaseEchoCanceler();
//            audioPostProcessEffect.releaseNoiseSuppressor();
//            audioPostProcessEffect.releaseAutoGainControl();
//        }
        Log.i(TAG, "Microphone stopped");
    }

    /**
     * Get PCM buffer size
     */
    private int getPcmBufferSize() {
        int pcmBufSize =
                AudioRecord.getMinBufferSize(sampleRate, channel, AudioFormat.ENCODING_PCM_16BIT) + 8191;
        return pcmBufSize - (pcmBufSize % 8192);
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getAudioFormat() {
        return audioFormat;
    }

    public int getChannel() {
        return channel;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isCreated() {
        return created;
    }
}