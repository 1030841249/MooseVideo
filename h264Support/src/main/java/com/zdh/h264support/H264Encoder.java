package com.zdh.h264support;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Bundle;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * author: ZDH
 * Date: 2022/3/24
 * Description:
 */
public class H264Encoder implements Runnable{

    MediaCodec mEncoder;
    int width = 720;
    int height = 1280;
    MediaProjection mediaProjection;
    VirtualDisplay virtualDisplay;
    MediaFormat mediaFormat;
    OnEncodeFrameListener onEncodeFrameListener;

    long startTimes = 0;
    long timeStamps = 0;
    int I_INTERVAL = 2;

    public interface OnEncodeFrameListener {
        void decodeOneFrame(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo);
    }

    public void init(MediaProjection mediaProjection,OnEncodeFrameListener listener) {
        this.mediaProjection = mediaProjection;
        onEncodeFrameListener = listener;
        initFormat();
        initCodec();
        initMediaProjection();
        new Thread(this).start();
    }

    private void initFormat() {
        mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_INTERVAL);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
    }

    private void initCodec() {
        try {
            mEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mEncoder.configure(mediaFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initMediaProjection() {
        virtualDisplay = mediaProjection.createVirtualDisplay("screen-h264", width, height, 1,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mEncoder.createInputSurface(), null, null);
    }

    @Override
    public void run() {
        mEncoder.start();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        timeStamps = System.currentTimeMillis();
        while(true) {
            // 请求 I 帧
            if (System.currentTimeMillis() - timeStamps >= I_INTERVAL * 1000) {
                Bundle bundle = new Bundle();
                bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                mEncoder.setParameters(bundle);
                timeStamps = System.currentTimeMillis();
            }
            int index = mEncoder.dequeueOutputBuffer(bufferInfo, 10000);
            if (index >= 0) {
                // 很重要，时间戳需要重新计算
                if (startTimes == 0) {
                    startTimes = bufferInfo.presentationTimeUs / 1000;
                }
                ByteBuffer byteBuffer = mEncoder.getOutputBuffer(index);
                bufferInfo.presentationTimeUs = (bufferInfo.presentationTimeUs/1000)-startTimes;
                if (onEncodeFrameListener != null) {
                    onEncodeFrameListener.decodeOneFrame(byteBuffer,bufferInfo);
                }
                mEncoder.releaseOutputBuffer(index,false);
            }
        }
    }
}
