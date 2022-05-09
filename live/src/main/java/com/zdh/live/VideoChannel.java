package com.zdh.live;

import java.io.File;

/**
 * author: ZDH
 * Date: 2022/4/1
 * Description:
 */
public class VideoChannel {
    int width;
    int height;
    int fps;
    int bitrate;
    public VideoChannel(int width, int height, int fps, int bitrate) {
        this.width =width;
        this.height = height;
        this.fps = fps;
        this.bitrate = bitrate;
        native_init();
    }

    public void setVideoEncInfo(int width, int height) {
        this.width = width;
        this.height = height;
        native_setVideoEncInfo(width,height,fps,bitrate);
    }

    public void start(String path) {
        native_start(path);
    }

    public void pushVideoToNative(byte[] data) {
        pushVideo(data);
    }

    // native 回调
    private void postData(byte[] data) {
        FileWriter.writeBytes(data);
    }

    private native void native_init();

    private native void native_start(String _path);

    private native void native_setVideoEncInfo(int width, int height, int fps, int bitrate);

    private native void pushVideo(byte[] data);
}
