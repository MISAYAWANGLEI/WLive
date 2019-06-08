
#include <cstring>
#include "AudioLive.h"
#include "macro.h"

AudioLive::AudioLive() {

}

AudioLive::~AudioLive() {
    DELETE(outPutBuffer);
    //释放编码器
    if (audioCodec) {
        faacEncClose(audioCodec);
        audioCodec = 0;
    }
}

void AudioLive::setAudioEncInfo(int samplesInHZ, int channels) {
    LOGE("打开faac编码器");
    mChannels = channels;
    //inputSamples、一次最大能输入编码器的样本数量 也编码的数据的个数 (一个样本是16位 2字节)
    //maxOutputBytes、最大可能的输出数据  编码后的最大字节数
    //打开编码器
    audioCodec = faacEncOpen(samplesInHZ,channels,&inputSamples,&maxOutputBytes);
    //设置编码器参数
    faacEncConfigurationPtr config = faacEncGetCurrentConfiguration(audioCodec);
    //指定为 mpeg4 标准
    config->mpegVersion = MPEG4;
    //lc 标准
    config->aacObjectType = LOW;
    //16位
    config->inputFormat = FAAC_INPUT_16BIT;
    // 编码出原始数据 既不是adts也不是adif
    config->outputFormat = 0;
    faacEncSetConfiguration(audioCodec, config);

    //输出缓冲区 编码后的数据
    outPutBuffer = new u_char[maxOutputBytes];
    LOGE("打开faac编码器结束");
}

unsigned long AudioLive::getInputSamples() {
    return inputSamples;
}

void AudioLive::setAudioCallBack(AudioLive::AudioCallBack callBack) {
    this->callBack = callBack;
}

RTMPPacket* AudioLive::getAudioTag() {
    u_char *buf;
    u_long len;
    faacEncGetDecoderSpecificInfo(audioCodec, &buf, &len);
    int bodySize = 2 + len;
    RTMPPacket *packet = new RTMPPacket;
    RTMPPacket_Alloc(packet, bodySize);
    //双声道
    packet->m_body[0] = 0xAF;
    if (mChannels == 1) {
        packet->m_body[0] = 0xAE;
    }
    packet->m_body[1] = 0x00;
    //图片数据
    memcpy(&packet->m_body[2], buf, len);

    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = bodySize;
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nChannel = 0x11;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    return packet;
}

void AudioLive::encodeData(int8_t *data) {
    LOGE("音频开始编码");
    //返回编码后数据字节的长度
    int bytelen = faacEncEncode(audioCodec, reinterpret_cast<int32_t *>(data), inputSamples, outPutBuffer,
                                maxOutputBytes);
    if (bytelen > 0) {

        int bodySize = 2 + bytelen;
        RTMPPacket *packet = new RTMPPacket;
        RTMPPacket_Alloc(packet, bodySize);
        //双声道
        packet->m_body[0] = 0xAF;
        if (mChannels == 1) {
            packet->m_body[0] = 0xAE;
        }
        //编码出的声音 都是 0x01
        packet->m_body[1] = 0x01;

        memcpy(&packet->m_body[2], outPutBuffer, bytelen);

        packet->m_hasAbsTimestamp = 0;
        packet->m_nBodySize = bodySize;
        packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
        packet->m_nChannel = 0x11;
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
        callBack(packet);
        LOGE("音频结束编码");
    }

}
