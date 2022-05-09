package com.zdh.mediacodec.decode;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

/**
 * author: ZDH
 * Date: 2022/3/21
 * Description:
 */
class MediaDecoder extends BaseDecoder{
    MediaCodec mDecoder;
    MediaFormat mFormat;
    MediaCodec.Callback mCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
            ByteBuffer buffer = codec.getInputBuffer(index);

        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {

        }

        @Override
        public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

        }
    };

    MediaDecoder(Context context, String path) {
        super(context, path);
        init();
    }

    public void init() {

    }

    @Override
    String getDecodeType() {
        return "video/avc";
    }

    @Override
    MediaFormat getMediaFormat() {
        mFormat = MediaFormat.createVideoFormat("video/avc", 100, 100);
        return mFormat;
    }

    @Override
    Surface getSurface() {
        return null;
    }

    @Override
    MediaCodec.Callback getCallback() {
        return mCallback;
    }
}
