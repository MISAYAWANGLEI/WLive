//
// Created by wanglei55 on 2019/6/3.
//

#ifndef WLIVE_AUDIOLIVE_H
#define WLIVE_AUDIOLIVE_H
#include "librtmp/rtmp.h"
#include "faac.h"
#include <sys/types.h>
#include <inttypes.h>

class AudioLive {
    typedef void (*AudioCallBack)(RTMPPacket* packet);
public:
    AudioLive();
    ~AudioLive();
    void setAudioCallBack(AudioCallBack callBack);
    u_long getInputSamples();
    RTMPPacket* getAudioTag();
    void encodeData(int8_t *data);
    void setAudioEncInfo(int samplesInHZ, int channels);
private:
    unsigned long inputSamples;
    unsigned long maxOutputBytes;
    int mChannels;
    AudioCallBack callBack;
    faacEncHandle audioCodec = 0;
    u_char *outPutBuffer = 0;//编码后的数据存放buffer
};


#endif //WLIVE_AUDIOLIVE_H
