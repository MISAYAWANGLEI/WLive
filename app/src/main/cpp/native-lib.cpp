#include <jni.h>
#include <string>
#include "librtmp/rtmp.h"
#include "include/x264.h"
#include <x264.h>


extern "C" JNIEXPORT jstring JNICALL
Java_com_wanglei_wlive_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    RTMP_Alloc();
    x264_picture_t *p = new x264_picture_t;
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
