
#include "AudioLive.h"

AudioLive::AudioLive() {

}

AudioLive::~AudioLive() {

}

void AudioLive::setAudioEncInfo(int samplesInHZ, int channels) {
    mChannels = channels;

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
}

unsigned long AudioLive::getInputSamples() {
    return inputSamples;
}

void AudioLive::encodeData(int8_t *data) {

}
