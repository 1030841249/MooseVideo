package com.zdh.mediacodec;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import com.zdh.live.RTMPPackage;
import com.zdh.live.ScreenLive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * author: ZDH
 * Date: 2022/3/31
 * Description:
 */
public class AudioEncoder implements Runnable{
    public static final int RTMP_TYPE_AUDIO_HEADER = 1;
    public static final int RTMP_TYPE_AUDIO = 2;
    MediaCodec mEncoder;
    MediaFormat audioFormat;
    AudioRecord audioRecord;
    int minBufferSize;
    long startTimes = 0;
    boolean isRecording = true;
    ScreenLive screenLive;
    OnAudioEncodedOneFrameListener onAudioEncodedOneFrameListener;
    public interface OnAudioEncodedOneFrameListener{
        void onEncodedOneFreame(int type,ByteBuffer byteBuffer, MediaCodec.BufferInfo info);
    }

    public AudioEncoder(ScreenLive screenLive, OnAudioEncodedOneFrameListener listener) {
        this.screenLive = screenLive;
        onAudioEncodedOneFrameListener = listener;
        initCodec();
        initAudioRecord();
    }

    private void initCodec() {
        audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 1);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 64_000);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        try {
            mEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            mEncoder.configure(audioFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initAudioRecord() {
        minBufferSize = AudioRecord.getMinBufferSize(44100,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,minBufferSize);
    }

    public void start() {
        new Thread(this).start();
    }
    @Override
    public void run() {
        mEncoder.start();
        audioRecord.startRecording();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        byte[] audioBuffer = new byte[minBufferSize];
        if (screenLive != null) {
            byte[] data = new byte[]{0x12,0x08};
            screenLive.sendData(new RTMPPackage(RTMP_TYPE_AUDIO_HEADER, data, data.length, 0));
//            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length);
//            byteBuffer.put(data);
//            byteBuffer.position(0);
//            bufferInfo.size = data.length;
//            bufferInfo.presentationTimeUs = 0;
//            onAudioEncodedOneFrameListener.onEncodedOneFreame(RTMP_TYPE_AUDIO_HEADER,byteBuffer,bufferInfo);
//            byteBuffer.clear();
//            byteBuffer = null;
        }
        while(isRecording) {
            int index = 0;
                int size = audioRecord.read(audioBuffer, 0, minBufferSize);
                if(size <= 0) continue;
                index = mEncoder.dequeueInputBuffer(100);
                if (index >= 0) {
                    ByteBuffer byteBuffer = mEncoder.getInputBuffer(index);
                    byteBuffer.clear();
                    byteBuffer.put(audioBuffer, 0,size);
                    mEncoder.queueInputBuffer(index, 0, size,
                            System.nanoTime()/1000, 0);
                }
                index = mEncoder.dequeueOutputBuffer(bufferInfo, 100);
                while (index >= 0) {
                    if (startTimes == 0) {
                        startTimes = bufferInfo.presentationTimeUs / 1000;
                    }
                    bufferInfo.presentationTimeUs = (bufferInfo.presentationTimeUs/1000) - startTimes;
                    ByteBuffer byteBuffer = mEncoder.getOutputBuffer(index);
                    byte[] buffer = new byte[bufferInfo.size];
                    byteBuffer.get(buffer);
                    if (screenLive != null) {
                        screenLive.sendData(new RTMPPackage(RTMP_TYPE_AUDIO, buffer, bufferInfo.size, bufferInfo.presentationTimeUs));
                    }
//                    if (onAudioEncodedOneFrameListener != null) {
//                        onAudioEncodedOneFrameListener.onEncodedOneFreame(RTMP_TYPE_AUDIO,byteBuffer,bufferInfo);
//                    }
                    mEncoder.releaseOutputBuffer(index, false);
                    index = mEncoder.dequeueOutputBuffer(bufferInfo, 0);
                }
            }

        mEncoder.release();
        audioRecord.release();
        }


}
