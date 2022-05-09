//
// Created by ZDH on 2022/4/3.
//

#ifndef MOOSEVIDEO_JAVACALLHELPER_H
#define MOOSEVIDEO_JAVACALLHELPER_H
#include <jni.h>
#define THREAD_MAIN 1
#define THREAD_CHILD 2

class JavaCallHelper {
public:
    JavaCallHelper(JavaVM *_jvm,JNIEnv *_env,jobject& _jobj);
    void postData(char* data,int len,int thread = THREAD_MAIN);
private:
    JavaVM* jvm;
    JNIEnv* env;
    jobject jobj;
    jmethodID jmid_post_data;
};


#endif //MOOSEVIDEO_JAVACALLHELPER_H
