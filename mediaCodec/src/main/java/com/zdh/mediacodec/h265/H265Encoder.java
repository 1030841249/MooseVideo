package com.zdh.mediacodec.h265;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * author: ZDH
 * Date: 2022/3/24
 * Description:
 */
public class H265Encoder implements Runnable{

    public static final int NAL_I = 19;
    public static final int NAL_VPS = 32;
    private byte[] vps_sps_pps_buf;

    MediaCodec mEncoder;
    int width = 720;
    int height = 1280;
    MediaProjection mediaProjection;
    VirtualDisplay virtualDisplay;
    MediaFormat mediaFormat;
    SocketPushLive socketPushLive;
    public void init(MediaProjection mediaProjection,SocketPushLive socketPushLive) {
        this.mediaProjection = mediaProjection;
        this.socketPushLive = socketPushLive;
        initFormat();
        initCodec();
        initMediaProjection();
        new Thread(this).start();
    }

    private void initMediaProjection() {
        virtualDisplay = mediaProjection.createVirtualDisplay("screen-h265", width, height, 1,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mEncoder.createInputSurface(), null, null);
    }

    private void initFormat() {
        mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
    }

    private void initCodec() {
        try {
            mEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
//            mEncoder.setCallback();
            mEncoder.configure(mediaFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        mEncoder.start();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while(true) {
            int index = mEncoder.dequeueOutputBuffer(bufferInfo, 10000);
            if (index >= 0) {
                ByteBuffer byteBuffer = mEncoder.getOutputBuffer(index);
                dealBuffer(byteBuffer,bufferInfo);
                mEncoder.releaseOutputBuffer(index,false);
            }
        }
    }

    private void dealBuffer(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        int offset = 4;
        if (byteBuffer.get(2) == 0x01) {
            offset = 3;
        }
        int type = (byteBuffer.get(offset) & 0x7E) >> 1;
        if (type == NAL_VPS) {
            vps_sps_pps_buf = new byte[bufferInfo.size];
            byteBuffer.get(vps_sps_pps_buf);
        } else if (type == NAL_I) {
            byte[] buffer = new byte[bufferInfo.size];
            byteBuffer.get(buffer);
            byte[] data = new byte[vps_sps_pps_buf.length + bufferInfo.size];
            System.arraycopy(vps_sps_pps_buf, 0, data, 0,vps_sps_pps_buf.length);
            System.arraycopy(buffer, 0, data, vps_sps_pps_buf.length,buffer.length);
            socketPushLive.sendData(data);
        } else {
            byte[] data = new byte[bufferInfo.size];
            byteBuffer.get(data);
            socketPushLive.sendData(data);
        }
    }
}
