//
// Created by ZDH on 2022/4/3.
//

#include "JavaCallHelper.h"
JavaCallHelper::JavaCallHelper(JavaVM *_jvm, JNIEnv *_env, jobject& _jobj):jvm(_jvm),env(_env) {
    jobj = env->NewGlobalRef(_jobj);
    jclass clazz = env->GetObjectClass(jobj);
    jmid_post_data = env->GetMethodID(clazz,"postData","([B)V");
}

void JavaCallHelper::postData(char *data, int len, int thread) {
    jbyteArray array = env->NewByteArray(len);
    env->SetByteArrayRegion(array, 0, len, reinterpret_cast<const jbyte *>(data));
    if (thread == THREAD_MAIN) {
        env->CallVoidMethod(jobj, jmid_post_data,array);
    } else {
        // 通过jvm 连接到对应的线程
        if (jvm->AttachCurrentThread(&env, 0) != JNI_OK) {
            return;
        }
        env->CallVoidMethod(jobj, jmid_post_data,array);
        jvm->DetachCurrentThread();
    }
}