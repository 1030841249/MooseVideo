//
// Created by ZDH on 2022/4/1.
//
#include "VideoChannel.h"
#include <jni.h>
#include <cstdio>
#include <cstring>
#include <pthread.h>
#include "safe_queue.h"
#include "AudioChannel.h"
#include <android/log.h>

#define LOG_TAG "native_live"
#define LOG(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
extern "C" {
#include "rtmp.h"
}

VideoChannel *videoChannel = 0;
AudioChannel *audioChannel = 0;
pthread_t pid;
bool isStart = 0;
uint32_t startTimes;
//推流标志位
int readyPushing = 0;
//阻塞式队列
SafeQueue<RTMPPacket *> packets;
JavaCallHelper *javaCallHelper;
//虚拟机的引用
JavaVM *javaVM = 0;

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    return JNI_VERSION_1_6;
}

void callback(RTMPPacket *packet) {
    if (packet) {
        if (packets.size() > 200) {
            packets.clear();
        }
        packet->m_nTimeStamp = RTMP_GetTime() - startTimes;
        packets.push(packet);
    }
}

void releasePacket(RTMPPacket *packet) {
    if(packet) {
        RTMPPacket_Free(packet);
        delete packet;
        packet = nullptr;
    }
}

void *start(void *args) {
    char *url = static_cast<char *>(args);
    RTMP *rtmp;
    do {
        rtmp = RTMP_Alloc();
        RTMP_Init(rtmp);
        LOG("RTMP_SetupURL");
        if (!RTMP_SetupURL(rtmp, url)) {
            break;
        }
        RTMP_EnableWrite(rtmp);
        LOG("RTMP_Connect");
        if (!RTMP_Connect(rtmp, 0)) {
            break;
        }
        LOG("RTMP_ConnectStream");
        if (!RTMP_ConnectStream(rtmp, 0)) {
            break;
        }
        LOG("readyPushing");
        readyPushing = 1;
        startTimes = RTMP_GetTime();
        packets.setWork(1);
        RTMPPacket *packet;

        if(audioChannel) {
            LOG("发送audiochannel——header");
            RTMPPacket *audioHeader = audioChannel->getAudioHeadConfig();
            callback(audioHeader);
        }
        while (isStart) {
            packets.pop(packet);
            if(!isStart) break;
            if (!packet) {
                continue;
            }
            packet->m_nInfoField2 = rtmp->m_stream_id;
            int ret = RTMP_SendPacket(rtmp, packet, 1);
//            LOG("RTMP 数据发送情况 %d",ret);
            releasePacket(packet);
        }
        releasePacket(packet);
    }while(0);
    if(rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
    }
    delete url;
    return 0;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_zdh_live_VideoChannel_native_1init(JNIEnv *env, jobject thiz) {
    javaCallHelper = new JavaCallHelper(javaVM,env,thiz);
    videoChannel = new VideoChannel(javaCallHelper);
    videoChannel->setVideoCallback(callback);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_zdh_live_VideoChannel_native_1start(JNIEnv *env, jobject thiz, jstring _path) {
    if (isStart) {
        return;
    }
    const char *path = env->GetStringUTFChars(_path, 0);
    char *url = new char[strlen(path) + 1];
    strcpy(url,path);
    LOG("url %s",url);
    isStart = 1;
    pthread_create(&pid,0,start,url);
    env->ReleaseStringUTFChars(_path,path);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_zdh_live_VideoChannel_native_1setVideoEncInfo(JNIEnv *env, jobject thiz, jint width,
                                                       jint height, jint fps, jint bitrate) {
    if(videoChannel) {
        videoChannel->setVideoEncodeInfo(width, height, fps, bitrate);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_zdh_live_VideoChannel_pushVideo(JNIEnv *env, jobject thiz, jbyteArray data) {
    if (!videoChannel || !readyPushing) {
        return;
    }
    jbyte *buffer = env->GetByteArrayElements(data,0);
    videoChannel->encodedData(buffer);
    env->ReleaseByteArrayElements(data,buffer,0);
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_zdh_live_AudioChannel_initAudioEncInfo(JNIEnv *env, jobject thiz, jint sample_rates,
                                                jint channel) {
    audioChannel = new AudioChannel();
    audioChannel->openCodec(sample_rates, channel);
    audioChannel->setCallback(callback);
    return audioChannel->getInputByteNum();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_zdh_live_AudioChannel_sendAudioData(JNIEnv *env, jobject thiz, jbyteArray _data, jint len) {
    if (!audioChannel || !readyPushing) {
        return;
    }
    jbyte *data = env->GetByteArrayElements(_data, 0);
    audioChannel->encodeData(reinterpret_cast<int32_t *>(data), len/2);
    env->ReleaseByteArrayElements(_data, data, 0);
}