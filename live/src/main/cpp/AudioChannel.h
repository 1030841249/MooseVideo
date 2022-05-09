//
// Created by ZDH on 2022/4/6.
//

#ifndef MOOSEVIDEO_AUDIOCHANNEL_H
#define MOOSEVIDEO_AUDIOCHANNEL_H
#include "librtmp/rtmp.h"
#include "faac.h"

class AudioChannel {
    typedef void(*Callback)(RTMPPacket *packet);
public:
    AudioChannel();

    void openCodec(int sampleRates,int channel);

    void setCallback(Callback callback){
        this->callback = callback;
    }

    int getInputByteNum(){
        return inputByteNum;
    }
    RTMPPacket * getAudioHeadConfig();
    void encodeData(int32_t *data, int len);

private:
    Callback callback;
    // 编码器
    faacEncHandle codec = 0;
    unsigned long maxOutputBytes;
    // 输出的数据
    unsigned char *outputBuffer = 0;
    // 输入容器的大小
    unsigned long inputByteNum;
};


#endif //MOOSEVIDEO_AUDIOCHANNEL_H
