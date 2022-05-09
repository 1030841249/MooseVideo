//
// Created by ZDH on 2022/4/6.
//

#include <cstdlib>
#include <cstring>
#include "AudioChannel.h"
#include "LOG.h"

AudioChannel::AudioChannel() {

}

void AudioChannel::openCodec(int sampleRates, int channel) {
    // 编码一帧AAC所需要的字节数
    unsigned long inputSamples;
    // maxOutputBytes 得到每次调用编码时生成的AAC数据的最大长度
    codec = faacEncOpen(sampleRates, channel, &inputSamples, &maxOutputBytes);
    inputByteNum = inputSamples * 2;
    outputBuffer = static_cast<unsigned char *>(malloc(maxOutputBytes));
    // 配置信息
    faacEncConfigurationPtr configPtr = faacEncGetCurrentConfiguration(codec);
    LOG("初始化-----------》%d  inputByteNum %d  maxOutputBytes:%d ",codec,inputByteNum,maxOutputBytes);
    //编码  MPEG AAC
    configPtr->mpegVersion = MPEG4;
//    编码等级
    configPtr->aacObjectType = LOW;
    // 流输出格式
    configPtr->outputFormat = 0;
    // 采样位数
    configPtr->inputFormat = FAAC_INPUT_16BIT;
    // 生效配置
    faacEncSetConfiguration(codec, configPtr);
}


RTMPPacket *createAudioPacket(int8_t *data, int len) {
    int size = 2 + len;
    RTMPPacket *packet = new RTMPPacket();
    RTMPPacket_Alloc(packet, size);
    // 音频采样信息
    packet->m_body[0] = 0xAF;
    // 音频数据
    packet->m_body[1] = 0x01;
    memcpy(&packet->m_body[2], data, len);
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = size;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nChannel = 0x05;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    return packet;
}

void AudioChannel::encodeData(int32_t *data,int len) {
    int size = faacEncEncode(codec, data, len, outputBuffer, maxOutputBytes);
    LOG("encode Audio Data Size : %d",size);
    if (size > 0) {
        callback(createAudioPacket(reinterpret_cast<int8_t *>(outputBuffer), size));
//        callback(packet);
    }
}

RTMPPacket *AudioChannel::getAudioHeadConfig() {
    u_char *buffer;
    u_long len;
    // 音频头 0x12 0x08
    faacEncGetDecoderSpecificInfo(codec,&buffer,&len);
    RTMPPacket *packet = new RTMPPacket();
    RTMPPacket_Alloc(packet, len + 2);
    packet->m_body[0] = 0XAF;
    packet->m_body[1] = 0X00;
    memcpy(&packet->m_body[2],buffer,len);
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = len + 2;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nChannel = 0x05;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    return packet;
}

