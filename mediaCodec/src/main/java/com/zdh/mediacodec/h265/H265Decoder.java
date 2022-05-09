package com.zdh.mediacodec.h265;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * author: ZDH
 * Date: 2022/3/24
 * Description:
 */
class H265Decoder {
    int width = 720;
    int height = 1280;
    MediaCodec mDecoder;
    MediaFormat mediaFormat;
    public H265Decoder() {
        try {
            mDecoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init(Surface surface) {
        mDecoder.configure(mediaFormat,surface,null,0);
    }

    public void decode(byte[] data) {
        int index = mDecoder.dequeueInputBuffer(10000);
        if (index >= 0) {
            ByteBuffer byteBuffer = mDecoder.getInputBuffer(index);
            byteBuffer.clear();
            byteBuffer.put(data, 0, data.length);
            mDecoder.queueInputBuffer(index,0,byteBuffer.remaining(),0,0);
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        index = mDecoder.dequeueOutputBuffer(bufferInfo, 1);
        while(true) {
            mDecoder.releaseOutputBuffer(index,true);
            index = mDecoder.dequeueOutputBuffer(bufferInfo, 1);
        }
    }
}
