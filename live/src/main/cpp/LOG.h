//
// Created by ZDH on 2022/4/7.
//

#ifndef MOOSEVIDEO_LOG_H
#define MOOSEVIDEO_LOG_H
#include "android/log.h"
#define LOG_TAG "native_live"
#define LOG(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

#endif //MOOSEVIDEO_LOG_H
