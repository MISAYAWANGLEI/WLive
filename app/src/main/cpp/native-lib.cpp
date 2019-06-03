#include <jni.h>
#include <string>
#include "librtmp/rtmp.h"
#include "include/x264.h"
#include <x264.h>
#include "SafeQueue.h"
#include "VideoLive.h"
#include <pthread.h>
#include "macro.h"
#include "AudioLive.h"

SafeQueue<RTMPPacket *> packets;//存储已经编码后的数据
VideoLive *videoLive = 0;
AudioLive *audioLive = 0;
int isStart = 0;
pthread_t pid_start;//从packets中取出stmp包发送

int readyPushing = 0;
uint32_t start_time;

void releasePacketCallBack(RTMPPacket** packet){
    if (packet){
        RTMPPacket_Free(*packet);
        *packet = 0;
    }
}

void callBack(RTMPPacket* packet){
    if (packet){
        packet->m_nTimeStamp = RTMP_GetTime() - start_time;
        packets.push(packet);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wanglei_wlive_LivePusher_native_1init(JNIEnv *env, jobject instance) {

    videoLive = new VideoLive;
    videoLive->setVideoCallBack(callBack);
    audioLive = new AudioLive;
    audioLive->setAudioCallBack(callBack);
    packets.setReleaseCallBack(releasePacketCallBack);
}
//释放内存
void release(RTMP *rtmp,char *url){
    if (rtmp){
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
    }
    if (url){
        delete url;
    }
}
void * start(void* url){
    char* path = static_cast<char *>(url);
    RTMP *rtmp = RTMP_Alloc();
    if (!rtmp){
        LOGE("rtmp创建失败");
        release(rtmp,path);
        return 0;
    }
    RTMP_Init(rtmp);
    rtmp->Link.timeout = 5;//5秒超时时间
    int ret = RTMP_SetupURL(rtmp,path);
    if(!ret){
        LOGE("rtmp设置地址失败:%s",path);
        release(rtmp,path);
        return 0;
    }
    RTMP_EnableWrite(rtmp);//开启写出数据模式，向服务器发送数据
    ret = RTMP_Connect(rtmp,0);//连接服务器
    if(!ret){
        LOGE("rtmp链接地址失败:%s",path);
        release(rtmp,path);
        return 0;
    }
    ret = RTMP_ConnectStream(rtmp,0);//创建一个链接流
    if(!ret){
        LOGE("rtmp链接流失败:%s",path);
        release(rtmp,path);
        return 0;
    }
    //正常链接服务器，可以推流了
    readyPushing = 1;
    start_time = RTMP_GetTime();//记录开始推流时间
    packets.setWork(1);
    RTMPPacket *packet;
    //第一个数据是发送aac解码数据包
    callBack(audioLive->getAudioTag());
    while (readyPushing){//不断从队列取出数据进行发送
        packets.pop(packet);
        if (!isStart){
            break;
        }
        if(!packet){
            continue;
        }
        //发送数据
        ret = RTMP_SendPacket(rtmp,packet,1);//1表示放入队列按照顺序发送
        releasePacketCallBack(&packet);//发送完及时释放内存
        if (!ret) {
            LOGE("发送数据失败");
            break;
        }
    }
    isStart = 0;
    readyPushing = 0;
    packets.setWork(0);
    packets.clear();
    releasePacketCallBack(&packet);
    release(rtmp,path);
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wanglei_wlive_LivePusher_native_1start(JNIEnv *env, jobject instance, jstring path_) {

    if (isStart){
        return;
    }
    isStart = 1;
    const char *path = env->GetStringUTFChars(path_, 0);
    char *url = new char[strlen(path)+1];
    strcpy(url,path);
    pthread_create(&pid_start,0,start,url);
    env->ReleaseStringUTFChars(path_, path);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wanglei_wlive_LivePusher_native_1stop(JNIEnv *env, jobject instance) {
    readyPushing = 0;
    packets.clear();
    packets.setWork(0);
    pthread_join(pid_start,0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wanglei_wlive_LivePusher_native_1setVideoEncoderInfo(JNIEnv *env, jobject instance,
                                                              jint width, jint height, jint fps,
                                                              jint bitrate) {
    if(videoLive){
        videoLive->openVideoEncodec(width,height,fps,bitrate);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wanglei_wlive_LivePusher_native_1pushVideo(JNIEnv *env, jobject instance,
                                                    jbyteArray data_) {
    if(!videoLive || !readyPushing){
        return;
    }
    jbyte *data = env->GetByteArrayElements(data_, NULL);
    videoLive->encodeData(data);
    env->ReleaseByteArrayElements(data_, data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wanglei_wlive_LivePusher_native_1pushAudio(JNIEnv *env, jobject instance,
                                                    jbyteArray data_) {
    if(!videoLive || !readyPushing){
        return;
    }
    jbyte *data = env->GetByteArrayElements(data_, NULL);
    audioLive->encodeData(data);
    env->ReleaseByteArrayElements(data_, data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wanglei_wlive_LivePusher_native_1release(JNIEnv *env, jobject instance) {

    DELETE(videoLive);
    DELETE(audioLive);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_wanglei_wlive_LivePusher_getInputSamples(JNIEnv *env, jobject instance) {

    if(audioLive){
        audioLive->getInputSamples();
    }
    return -1;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wanglei_wlive_LivePusher_native_1setAudioEncInfo(JNIEnv *env, jobject instance,
                                                          jint sampleRateInHz, jint channels) {

    if(audioLive){
        audioLive->setAudioEncInfo(sampleRateInHz,channels);
    }
}