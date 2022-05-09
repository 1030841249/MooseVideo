package com.zdh.mediacodec;

import android.graphics.YuvImage;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * author: ZDH
 * Date: 2022/3/21
 * Description: mediacodec 解码
 */
public class MediaHelper implements Runnable {
    public static String TAG = "MediaHelper";
    MediaCodec mDecoder;
    String path;

    public void init(SurfaceView surfaceView) {
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                try {
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "out.h264");
                    init(surfaceHolder.getSurface(),file.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

            }
        });
    }

    public void init(Surface surface,String path) throws IOException {
        this.path = path;
        mDecoder = MediaCodec.createDecoderByType("video/avc");
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", 368, 384);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
        mDecoder.configure(mediaFormat,surface,null,0);
    }

    public byte[] readData(String filePath) throws IOException {
        File file = new File(filePath);
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] data = new byte[1024];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while( fileInputStream.read(data,0,1024) != -1) {
            bos.write(data, 0, data.length);
        }
        return bos.toByteArray();
    }

    @Override
    public void run() {
        mDecoder.start();
        decodeH264();
    }

    private void decodeH264() {
        try {
            byte[] data = readData(path);
            int startIndex = 0;
            int nextFrameIndex = 0;
            int totalSize = data.length;
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            while (true) {
                if (totalSize == 0 || startIndex >= totalSize) {
                    break;
                }
                nextFrameIndex = findNextFrame(data, startIndex+1, totalSize);
                Log.e(TAG, "decodeH264: nextFrameIndex  " + nextFrameIndex );
                int index = mDecoder.dequeueInputBuffer(10000);
                Log.e(TAG, "decodeH264: index  " + index );
                if (index >= 0) {
                    ByteBuffer byteBuffer = mDecoder.getInputBuffer(index);
                    byteBuffer.clear();
                    byteBuffer.put(data, startIndex, nextFrameIndex - startIndex);
                    mDecoder.queueInputBuffer(index, 0, nextFrameIndex - startIndex, 0, 0);
                    startIndex = nextFrameIndex;
                } else {
                    continue;
                }
                index = mDecoder.dequeueOutputBuffer(bufferInfo, 10000);
                Log.e(TAG, "decodeH264: outIndex  " + index );
                if (index >= 0) {
                    mDecoder.releaseOutputBuffer(index, true);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private int findNextFrame(byte[] data, int startIndex, int totalSize) {
        for(int i = startIndex; i < totalSize-4; i++) {
            if ((data[i] == 0x00 && data[i + 1] == 0x00 && data[i + 2] == 0x00 && data[i + 3] == 0x01)
                    || (data[i] == 0x00 && data[i + 1] == 0x00 && data[i + 2] == 0x01)) {
                return i;
            }
        }
        return -1;
    }
}
