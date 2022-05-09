//
// Created by ZDH on 2022/4/1.
//

#include "VideoChannel.h"
#include <cstring>
#include <android/log.h>
#include <cstdlib>

#define LOG_TAG "native_live"
#define LOG(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

VideoChannel::VideoChannel(JavaCallHelper *_javaCallHelper):javaCallHelper(_javaCallHelper) {
    LOG("INIT VideoChannel!");
}

void VideoChannel::setVideoEncodeInfo(int width, int height, int fps, int bitrate) {
    mWidth = width;
    mHeight = height;
    mBitrate = bitrate;
    mFps = fps;

    ySize = width * height;
    uvSize = ySize / 4;
    // 之前有使用，则关闭
    if (videoCodec) {
        x264_encoder_close(videoCodec);
        videoCodec = 0;
    }
//    定义参数
    x264_param_t param;
//    参数赋值   x264  麻烦  编码器 速度   直播  越快 1  越慢2
    x264_param_default_preset(&param, "ultrafast", "zerolatency");
//编码等级
    param.i_level_idc = 32;
//    选取显示格式
    param.i_csp = X264_CSP_I420;
    param.i_width = width;
    param.i_height = height;
//    B帧
    param.i_bframe = 0;
//折中    cpu   突发情况   ABR 平均
    param.rc.i_rc_method = X264_RC_ABR;
//k为单位
    param.rc.i_bitrate = bitrate / 1024;
    LOG("Bitrate %d",bitrate);
//帧率   1s/25帧     40ms  视频 编码      帧时间 ms存储  us   s
    param.i_fps_num = fps;
//    帧率 时间  分子  分母
    param.i_fps_den = 1;
//    分母
    param.i_timebase_den = param.i_fps_num;
//    分子
    param.i_timebase_num = param.i_fps_den;

//单位 分子/分母    发热  --
    //用fps而不是时间戳来计算帧间距离
    param.b_vfr_input = 0;
//I帧间隔     2s  15*2
    param.i_keyint_max = fps * 2;

    // 是否复制sps和pps放在每个关键帧的前面 该参数设置是让每个关键帧(I帧)都附带sps/pps。
    param.b_repeat_headers = 1;
//    sps  pps  赋值及裙楼
    //多线程
    param.i_threads = 1;
    x264_param_apply_profile(&param, "baseline");
//    打开编码器
    videoCodec = x264_encoder_open(&param);
    LOG("INIT VideoCodec %p",videoCodec);
//容器
    pic_in = new x264_picture_t;
//设置初始化大小  容器大小就确定的
    x264_picture_alloc(pic_in, X264_CSP_I420, width, height);
}

void VideoChannel::encodedData(int8_t *data) {
    // 为每个平面填充yuv数据，这里使用的nv21数据
//    memcpy(pic_in->img.plane[0], data, ySize);
//    // nv21, vuvu 排序
//    for(int i = 0; i < uvSize; i++) {
//        *(pic_in->img.plane[1] + i) = *(data + ySize + i * 2 + 1);
//        *(pic_in->img.plane[2] + i) = *(data + ySize + i * 2);
//    }
    memcpy(pic_in->img.plane[0], data, ySize);

    for (int i = 0; i < uvSize; ++i) {
        //v数据
        *(pic_in->img.plane[2] + i) = *(data + ySize + i * 2);
        //间隔1个字节取一个数据
        //u数据
        *(pic_in->img.plane[1] + i) = *(data + ySize + i * 2 + 1);
    }
    // 编码出几个nalu
    int pi_nal;
    // 编码出的h264
    x264_nal_t  *pp_nal;
    x264_picture_t  pic_out;
    // 关键
    int ret = x264_encoder_encode(videoCodec, &pp_nal, &pi_nal, pic_in, &pic_out);
    LOG("x264 encode result %d",ret);
    // 可能编码出多个帧，如sps、pps、I帧会一起编译出来
    LOG("NALU SIZE:%d",pi_nal);
//    if (pi_nal > 0) {
//        for (int i = 0; i < pi_nal; ++i) {
//            LOG("帧类型 %d",pp_nal[i].i_type);
//            javaCallHelper->postData(reinterpret_cast<char *>(pp_nal[i].p_payload), pp_nal[i].i_payload);
//        }
//    }
    uint8_t sps[100];
    uint8_t pps[100];
    int sps_len,pps_len;
    if (pi_nal > 0) {
        for(int i = 0; i < pi_nal; i++) {
            int type = pp_nal[i].i_type;
            switch (type) {
                case NAL_SPS:
                    sps_len = pp_nal[i].i_payload - 4;
                    memcpy(sps, pp_nal[i].p_payload + 4, sps_len);
                    break;
                case NAL_PPS:
                    pps_len = pp_nal[i].i_payload - 4;
                    memcpy(pps, pp_nal[i].p_payload + 4, sps_len);
                    sendSpsPps(sps,pps,sps_len,pps_len);
                    break;
                case NAL_SLICE_IDR:
                    sendFrame(pp_nal[i].i_type,pp_nal[i].i_payload,pp_nal[i].p_payload);
                    break;
            }
        }
    }

}

void VideoChannel::sendSpsPps(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len) {
    // 16 字节的 Tag Header AVC
    int size = 16 + sps_len + pps_len;
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
    packet->m_body[i++] = sps[1];   // AVCProfileIndication
    packet->m_body[i++] = sps[2];   // profile_compatibility
    packet->m_body[i++] = sps[3];   // AVCLevelIndication
    packet->m_body[i++] = 0xff;           // Reserved（占6位） 111111
    // + lengthSizeMinusOne（占2位,表示nalu长度表示字节数，一般总为 3 ） 11
    /*sps*/
    packet->m_body[i++] = 0xe1;   // reserved(占3位） 111 + numOfSPS（占5位）总为 1
    packet->m_body[i++] = (sps_len >> 8) & 0xff;
    packet->m_body[i++] = sps_len & 0xff;
    memcpy(&packet->m_body[i], sps, sps_len);
    i += sps_len;
    /*pps*/
    packet->m_body[i++] = 0x01;   // numOfPPS
    packet->m_body[i++] = (pps_len >> 8) & 0xff;    // ppsLength 长 2 字节
    packet->m_body[i++] = (pps_len) & 0xff;
    memcpy(&packet->m_body[i], pps, pps_len);     // pps

    // RTMP Header
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = size;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nChannel = 0x04;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    callback(packet);
}

void VideoChannel::sendFrame(int type, int i_payload, uint8_t *p_payload) {
    if (p_payload[2] == 0x01) {
        p_payload+=3;
        i_payload-=3;
    } else if(p_payload[3] == 0x01) {
        p_payload+=4;
        i_payload-=4;
    }
    int size = 9 + i_payload;
    RTMPPacket *packet = new RTMPPacket;
    RTMPPacket_Alloc(packet, size);
    RTMPPacket_Reset(packet);
    packet->m_body[0] = 0x27;
    if (type == NAL_SLICE_IDR) {
        LOG("关键帧");
        packet->m_body[0] = 0x17;
    }
    // AVC NALU
    packet->m_body[1] = 0x01;
    // cts
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;
    // NALU size
    packet->m_body[5] = (i_payload >> 24) & 0xff;
    packet->m_body[6] = (i_payload >> 16) & 0xff;
    packet->m_body[7] = (i_payload >> 8) & 0xff;
    packet->m_body[8] = (i_payload) & 0xff;
    // NALU data
    memcpy(&packet->m_body[9], p_payload, i_payload);
    // header
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = size;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nChannel = 0x04; // csid
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    callback(packet);
}

void VideoChannel::setVideoCallback(VideoChannel::VideoCallback callback) {
    this->callback = callback;
}
