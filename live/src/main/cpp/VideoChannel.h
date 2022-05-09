//
// Created by ZDH on 2022/4/1.
//

#ifndef MOOSEVIDEO_VIDEOCHANNEL_H
#define MOOSEVIDEO_VIDEOCHANNEL_H
#include <jni.h>
#include "x264/armeabi-v7a/include/x264.h"
#include "JavaCallHelper.h"
#include <inttypes.h>
#include <rtmp.h>

class VideoChannel {
    // 函数指针
    typedef void (*VideoCallback)(RTMPPacket *packet);
public:
    VideoChannel(JavaCallHelper *_javaCallHelper);
    ~VideoChannel();
    void setVideoEncodeInfo(int width,int height,int fps,int bitrate);
    void encodedData(int8_t *data);

    void sendSpsPps(uint8_t* sps,uint8_t* pps,int sps_len,int pps_len);
    void sendFrame(int type, int i_payload,uint8_t* p_payload);

    void setVideoCallback(VideoCallback callback);
private:
    int mWidth;
    int mHeight;
    int mBitrate;
    int mFps;
    // I420
    int ySize;
    int uvSize;
    // 编码器
    x264_t *videoCodec = 0;
    x264_picture_t *pic_in = 0;
    JavaCallHelper *javaCallHelper;
    // 回调
    VideoCallback  callback;
};


#endif //MOOSEVIDEO_VIDEOCHANNEL_H
