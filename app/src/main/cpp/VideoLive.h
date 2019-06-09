#ifndef WLIVE_VIDEOLIVE_H
#define WLIVE_VIDEOLIVE_H

#include "librtmp/rtmp.h"
#include <pthread.h>
#include "x264.h"
#include "macro.h"
#include <inttypes.h>

class VideoLive {
    typedef void (*videoCallBack)(RTMPPacket* packet);
public:
    VideoLive();
    ~VideoLive();
    void setVideoCallBack(videoCallBack callBack1);
    void openVideoEncodec(int width, int height, int fps, int bitrate);
    void encodeData(int8_t *data,int width, int height, bool needRotate,
                    int degree);
private:
    pthread_mutex_t mutex;
    int mWidth;
    int mHeight;
    int mFps;
    int mBitrate;
    int ySize;
    int uvSize;
    int index = 0;
    x264_t *x264Codec = 0;//编码器
    x264_picture_t *pic_in = 0;
    videoCallBack callBack;
    void sendSpsPps(uint8_t *sps, uint8_t *pps, int len, int pps_len);
    void sendFrame(int type, uint8_t *payload, int i_payload);
};


#endif
