#include <jni.h>
#include <malloc.h>
#include <string.h>
#include <android/log.h>

#define LOG_TAG "native_live"
#define LOG(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

extern "C" {
#include <rtmp.h>
}

typedef struct  {
    RTMP *rtmp;
    int16_t sps_len;
    int8_t *sps;
    int16_t pps_len;
    int8_t *pps;
} Live;

Live *live;

void splitSpsAndPps(int8_t *data, int len);

RTMPPacket *createSpsPPsPacket();

RTMPPacket *createH264Packet(int8_t *, int, long);

int sendPacket(RTMPPacket *pPacket);

extern "C"
JNIEXPORT jint JNICALL
Java_com_zdh_live_ScreenLive_connect(JNIEnv *env, jobject thiz, jstring _url) {
    const char *url = env->GetStringUTFChars(_url, 0);
    // 初始化 RTMP
    live = static_cast<Live *>(malloc(sizeof(Live)));
    memset(live, 0, sizeof(Live));
    live->rtmp = RTMP_Alloc();
    RTMP_Init(live->rtmp);
    int ret = 0;
    do {
        // 设置链接地址
        LOG("设置链接");
        if (!(ret = RTMP_SetupURL(live->rtmp, (char *) url))) {
            break;
        }
        RTMP_EnableWrite(live->rtmp);
        LOG("连接");
        if (!(ret = RTMP_Connect(live->rtmp, 0))) {
            break;
        }
        LOG("创建流");
        if (!(ret = RTMP_ConnectStream(live->rtmp, 0))) {
            break;
        }
        LOG("初始化完成");
    } while (!ret);
    LOG("RET %d",ret);
    if (!ret) {
        free(live);
        live = nullptr;
    }
    env->ReleaseStringUTFChars(_url, url);

    return 1;
}

void sendVideo(int8_t *data, int len, long tms) {
    // 默认偏移 4 字节，起始码
    int offset = 4;
    // 起始码 00 00 01 偏移 3 字节
    if (data[2] == 0x01) {
        offset = 3;
    }
    int type = data[offset] & 0x1f;
    int ret = 0;
    // sps&&pps
    if (type == 7) {
        if (live && (!live->pps || !live->sps)) {
            splitSpsAndPps(data, len);
            ret = sendPacket(createSpsPPsPacket());
            LOG("发送SPSPPS结果：%d",ret);
        }
    } else {
        // I 帧,在直播中需要在每个I帧前，发送sps和pps信息，提供给观众解码
        if (type == 5) {
            ret = sendPacket(createSpsPPsPacket());
            LOG("发送SPSPPS结果：%d",ret);
        }
        RTMPPacket *packet = createH264Packet(data, len, tms);
        ret = sendPacket(packet);
        LOG("发送视频结果：%d",ret);
    }
//    LOG("发送结果：&b", !ret);
}

RTMPPacket *createH264Packet(int8_t *data, int len, long tms) {
    if (data[2] == 0x01) {
        data+=3;
//        len-=3;
    } else if(data[3] == 0x01) {
        data+=4;
//        len-=4;
    }
    int size = 9 + len;
    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, size);

    packet->m_body[0] = 0x27;
    if ((data[0] & 0x1f) == 5) {
        packet->m_body[0] = 0x17;
    }
    // AVC NALU
    packet->m_body[1] = 0x01;
    // cts
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;
    // NALU size
    packet->m_body[5] = (len >> 24) & 0xff;
    packet->m_body[6] = (len >> 16) & 0xff;
    packet->m_body[7] = (len >> 8) & 0xff;
    packet->m_body[8] = (len) & 0xff;
    // NALU data
    memcpy(&packet->m_body[9], data, len);
    // header
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = size;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nTimeStamp = tms;
    packet->m_nChannel = 0x04; // csid
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = live->rtmp->m_stream_id;
    return packet;
}

RTMPPacket *createSpsPPsPacket() {
    // 16 字节的 Tag Header AVC
    int size = 16 + live->sps_len + live->pps_len;
    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, size);
    int i = 0;
    // RTMP/FLV 视频数据格式
    packet->m_body[i++] = 0x17;
    // AVCVideoPacket
    // 帧类型：0，sequence header
    packet->m_body[i++] = 0x00;
    // cts
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    // avc sequence header
    /*AVCDecoderConfigurationRecord*/
    packet->m_body[i++] = 0x01;           // configurationVersion 固定版本 1
    packet->m_body[i++] = live->sps[1];   // AVCProfileIndication
    packet->m_body[i++] = live->sps[2];   // profile_compatibility
    packet->m_body[i++] = live->sps[3];   // AVCLevelIndication
    packet->m_body[i++] = 0xff;           // Reserved（占6位） 111111
    // + lengthSizeMinusOne（占2位,表示nalu长度表示字节数，一般总为 3 ） 11
    /*sps*/
    packet->m_body[i++] = 0xe1;   // reserved(占3位） 111 + numOfSPS（占5位）总为 1
    packet->m_body[i++] = (live->sps_len >> 8) & 0xff;
    packet->m_body[i++] = live->sps_len & 0xff;
    memcpy(&packet->m_body[i], live->sps, live->sps_len);
    i += live->sps_len;
    /*pps*/
    packet->m_body[i++] = 0x01;   // numOfPPS
    packet->m_body[i++] = (live->pps_len >> 8) & 0xff;    // ppsLength 长 2 字节
    packet->m_body[i++] = (live->pps_len) & 0xff;
    memcpy(&packet->m_body[i], live->pps, live->pps_len);     // pps

    // RTMP Header
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = size;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nChannel = 0x04;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = live->rtmp->m_stream_id;
    LOG("创建sps");
    return packet;
}

int sendPacket(RTMPPacket *packet) {
    int ret = 0;
//    if (live && RTMP_IsConnected(live->rtmp)) {
//    }
    ret = RTMP_SendPacket(live->rtmp, packet, 1);
    RTMPPacket_Free(packet);
    packet = nullptr;
    return ret;
}

/**
 * 分割 sps和pps 数据
 * @param data
 * @param len
 */
void splitSpsAndPps(int8_t *data, int len) {
    for (int i = 0; i < len; i++) {
        if (i + 4 >= len) break;
        if (data[i] == 0x00 && data[i + 1] == 0x00
            && data[i + 2] == 0x00 && data[i + 3] == 0x01) {
            // sps 和 pps在一起，通过寻找起始码分割
            if (data[i + 4] == 0x68) {
                live->sps_len = i - 4;
                live->sps = static_cast<int8_t *>(malloc(live->sps_len));
                memcpy(live->sps, data + 4, live->sps_len);
                live->pps_len = len - live->sps_len - 4 - 4;
                live->pps = static_cast<int8_t *>(malloc(live->pps_len));
                memcpy(live->pps, data + live->sps_len + 4 + 4, live->pps_len);
                LOG("SPS&PPS 解析完毕： SPS 长 %d  PPS 长 %d", live->sps_len, live->pps_len);
                break;
            }
        }
    }
}

RTMPPacket *createAudioPacket(int8_t *data, int len,long tms,int type) {
    int size = 2 + len;
    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, size);
    // 音频采样信息
    packet->m_body[0] = 0xAF;
    if(type == 1) {
        // 音频头
        packet->m_body[1] = 0x00;
    } else {
        // 音频数据
        packet->m_body[1] = 0x01;
    }
    memcpy(&packet->m_body[2],data,len);
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = size;
    packet->m_nTimeStamp = tms;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nChannel = 0x05;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = live->rtmp->m_stream_id;
    return packet;
}

void sendAudio(int8_t *data, int len,long tms,int type) {
    int ret = sendPacket(createAudioPacket(data, len, tms, type));
    LOG("发送音频结果：%d",ret);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_zdh_live_ScreenLive_send(JNIEnv *env, jobject thiz, jbyteArray _data, jint _len,
                                  jlong _tms,jint _type) {

    if(live && !live->rtmp) {
        LOG("连接到期");
        return;
    }
    jbyte *data = env->GetByteArrayElements(_data, 0);
    switch(_type) {
        case 0:
            sendVideo(data, _len, _tms);
            break;
        default:
            sendAudio(data,_len,_tms,_type);
            break;
    }
    env->ReleaseByteArrayElements(_data, data, 0);
}