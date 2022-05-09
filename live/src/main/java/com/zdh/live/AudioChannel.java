package com.zdh.live;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

/**
 * author: ZDH
 * Date: 2022/4/6
 * Description:
 */
public class AudioChannel {

    int sampleRate;
    int channelConfig;
    private int minBufferSize;
    private byte[] buffer;
    private Handler handler;
    private HandlerThread handlerThread;
    private AudioRecord audioRecord;

    public AudioChannel(int sampleRate, int channels) {
        this.sampleRate = sampleRate;
        channelConfig = channels == 2 ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO;
        minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);

        int inputByteNum = initAudioEncInfo(sampleRate, channels);
        buffer = new byte[inputByteNum];
        minBufferSize = inputByteNum > minBufferSize ? inputByteNum : minBufferSize;

        handlerThread = new HandlerThread("Audio-Record");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    public void start() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        sampleRate, channelConfig,
                        AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
                audioRecord.startRecording();
                while (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
//                    len实际长度len 打印下这个值  录音不成功
                    int len = audioRecord.read(buffer, 0, buffer.length);
                    Log.i("rtmp", "len: "+len);
                    if (len > 0) {
                        sendAudioData(buffer, len);
                    }
                }
            }
        });
    }

    private native int initAudioEncInfo(int sampleRates,int channel);

    public native void sendAudioData(byte[] data,int len);
}
