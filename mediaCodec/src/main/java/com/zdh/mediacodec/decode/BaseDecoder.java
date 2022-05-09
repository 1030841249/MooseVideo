package com.zdh.mediacodec.decode;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.view.Surface;

import java.io.IOException;

/**
 * author: ZDH
 * Date: 2022/3/22
 * Description:
 */
public abstract class BaseDecoder implements Runnable{

    protected MediaCodec mDecoder;
    private boolean isAsync = false;

    BaseDecoder(Context context,String path) {
        try {
            mDecoder = MediaCodec.createDecoderByType(getDecodeType());
            isAsync = getCallback() != null;
            if (isAsync) {
                mDecoder.setCallback(getCallback());
            }
            mDecoder.configure(getMediaFormat(),getSurface(),null,0);
            mDecoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

    }

    abstract String getDecodeType();
    abstract MediaFormat getMediaFormat();
    abstract Surface getSurface();
//    abstract MediaCrypto getMediaCrypto();
    abstract MediaCodec.Callback getCallback();

}
