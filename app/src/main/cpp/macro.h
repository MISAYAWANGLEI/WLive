//
// Created by wanglei55 on 2019/5/30.
//

#ifndef WLIVE_MACRO_H
#define WLIVE_MACRO_H

#include <android/log.h>

//标记线程 因为子线程需要attach
#define THREAD_MAIN 1
#define THREAD_CHILD 2

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,"FFMPEG",__VA_ARGS__)

//宏函数
#define DELETE(obj) if(obj){ delete obj; obj = 0; }

#endif //WLIVE_MACRO_H
