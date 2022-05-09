package com.zdh.live;

/**
 * author: ZDH
 * Date: 2022/3/30
 * Description:
 */
public class RTMPPackage {

    public static final int RTMP_TYPE_VIDEO = 0;
    public static final int RTMP_TYPE_AUDIO_HEADER = 1;
    public static final int RTMP_TYPE_AUDIO = 2;

    byte[] data;
    int len;
    long tms;
    int type;

    public RTMPPackage(int type,byte[] data, int len, long tms) {
        this.type = type;
        this.data = data;
        this.len = len;
        this.tms = tms;
    }
}
