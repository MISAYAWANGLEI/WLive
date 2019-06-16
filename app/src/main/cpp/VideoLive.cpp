//
// Created by wanglei55 on 2019/5/30.
//

#include <malloc.h>
#include "VideoLive.h"
#include "WYuvUtils.h"


VideoLive::VideoLive() {
    pthread_mutex_init(&mutex,0);
}

VideoLive::~VideoLive() {
    pthread_mutex_destroy(&mutex);
    if(!x264Codec){
        x264_encoder_close(x264Codec);
        x264Codec = 0;
    }
    if (!pic_in){
        x264_picture_clean(pic_in);
        delete pic_in;
        pic_in = 0;
    }
}

/**
 * 这里同步是为了用户切换摄像头考虑
 * @param width
 * @param height
 * @param fps
 * @param bitrate
 */
//打开X264编码器
void VideoLive::openVideoEncodec(int width, int height, int fps, int bitrate) {

    pthread_mutex_lock(&mutex);
    mWidth = width;
    mHeight = height;
    mFps = fps;
    mBitrate = bitrate;
    ySize = width * height;
    uvSize = (width >> 1) * (height >> 1);
    if (x264Codec) {
        x264_encoder_close(x264Codec);
        x264Codec = 0;
    }
    if (pic_in) {
        x264_picture_clean(pic_in);
        DELETE(pic_in);
    }


    //打开x264编码器
    //x264编码器的属性
    x264_param_t param;
    //2： 最快
    //3:  无延迟编码
    x264_param_default_preset(&param, "ultrafast", "zerolatency");
    //base_line 3.2 编码规格
    param.i_level_idc = 32;
    //输入数据格式
    param.i_csp = X264_CSP_I420;
    param.i_width = width;
    param.i_height = height;
    //无b帧
    param.i_bframe = 0;
    //参数i_rc_method表示码率控制，CQP(恒定质量)，CRF(恒定码率)，ABR(平均码率)
    param.rc.i_rc_method = X264_RC_ABR;
    //码率(比特率,单位Kbps)
    param.rc.i_bitrate = bitrate / 1000;
    //瞬时最大码率
    param.rc.i_vbv_max_bitrate = bitrate / 1000 * 1.2;
    //设置了i_vbv_max_bitrate必须设置此参数，码率控制区大小,单位kbps
    param.rc.i_vbv_buffer_size = bitrate / 1000;

    //帧率
    param.i_fps_num = fps;
    param.i_fps_den = 1;
    param.i_timebase_den = param.i_fps_num;
    param.i_timebase_num = param.i_fps_den;
//    param.pf_log = x264_log_default2;
    //用fps而不是时间戳来计算帧间距离
    param.b_vfr_input = 0;
    //帧距离(关键帧)  2s一个关键帧
    param.i_keyint_max = fps * 2;
    // 是否复制sps和pps放在每个关键帧的前面 该参数设置是让每个关键帧(I帧)都附带sps/pps。
    param.b_repeat_headers = 1;
    //多线程
    param.i_threads = 1;

    x264_param_apply_profile(&param, "baseline");
    //打开编码器
    x264Codec = x264_encoder_open(&param);
    pic_in = new x264_picture_t;
    x264_picture_alloc(pic_in, X264_CSP_I420, width, height);
    pthread_mutex_unlock(&mutex);
}

/**
 * data是摄像头拍摄的图像数据为NV21格式，
 * X264需要i420格式，需要转换
 * @param data 相机原样采集的nv21格式数据，未经任何处理
 */
void VideoLive::encodeData(int8_t *data,int src_length,int width, int height,
        bool needRotate,int degree) {
    //
    pthread_mutex_lock(&mutex);
    LOGE("视频开始编码");
    int8_t *dst_i420_data = (int8_t *) malloc(sizeof(int8_t) * width * height * 3 / 2);
    int8_t *dst_i420_data_rotate = (int8_t *) malloc(sizeof(int8_t) * width * height * 3 / 2);

    if (!dst_i420_data || !dst_i420_data_rotate){
        pthread_mutex_unlock(&mutex);
        return;
    }

    memset(dst_i420_data,0,sizeof(int8_t) * width * height * 3 / 2);
    memset(dst_i420_data_rotate,0,sizeof(int8_t) * width * height * 3 / 2);
    //NV21(I420SP)->I420P
    WYuvUtils::nv21ToI420(data,width,height,dst_i420_data);

    //needRotate = false;
    if(needRotate){
        WYuvUtils::rotateI420(dst_i420_data,width,height,dst_i420_data_rotate,degree);
    }
    //
    if(needRotate){
        memcpy(pic_in->img.plane[0],dst_i420_data_rotate,ySize);//Y
        memcpy(pic_in->img.plane[1],dst_i420_data_rotate+ySize,uvSize);//U
        memcpy(pic_in->img.plane[2],dst_i420_data_rotate+ySize+uvSize,uvSize);//V
    } else{
        memcpy(pic_in->img.plane[0],dst_i420_data,ySize);//Y
        memcpy(pic_in->img.plane[1],dst_i420_data+ySize,uvSize);//U
        memcpy(pic_in->img.plane[2],dst_i420_data+ySize+uvSize,uvSize);//V
    }
    //释放内存
    free(dst_i420_data);
    free(dst_i420_data_rotate);
//    memcpy(pic_in->img.plane[0], data, ySize);
//    for (int i = 0; i <uvSize ; ++i) {
//        *(pic_in->img.plane[1]+i) = *(data+ySize+i * 2+1);//U
//        *(pic_in->img.plane[2]+i) = *(data+ySize+i * 2);//v
//    }
    pic_in->i_pts = index++;
    //编码出的数据(结构体数组)
    x264_nal_t *pp_nal;
    int pi_nal;//数据个数
    x264_picture_t pic_out;
    if(x264_encoder_encode(x264Codec,&pp_nal,&pi_nal,pic_in,&pic_out)<0){
        LOGE("x264编码错误");
        pthread_mutex_unlock(&mutex);
        return;
    }

    int sps_len, pps_len;
    uint8_t *sps;
    uint8_t *pps;
    //uint8_t sps[100];
    //uint8_t pps[100];
    // SPS和PPS为描述信息，通过解析可以得到视频的分辨率，码率等信息
    //一组帧(H264中叫做GOP)会优先收到SPS与PPS没有这信息，无法解析图像
    //SPS与PPS可定连续出现，并且所占内存比较小，一般就几个字节，可以在一个包内发送
    //H.264原始码流（又称为“裸流”）是由一个一个的NALU组成的
    //其中每个NALU之间通过startcode（起始码）进行分隔，起始码分成两种：
    //0x000001（3Byte）或者0x00000001（4Byte）。如果NALU对应的Slice为一帧的开始就用0x00000001，
    //否则就用0x000001(一帧的一部分)。
    //00 00 00 01 或者 00 00 01分割
    for (int i = 0; i <pi_nal ; ++i) {
        if(pp_nal[i].i_type == NAL_SPS){
            sps_len = pp_nal[i].i_payload - 4;
            sps = (uint8_t *) malloc((size_t) (sps_len + 1));
            memcpy(sps,pp_nal[i].p_payload+4, (size_t) sps_len);
        } else if(pp_nal[i].i_type == NAL_PPS){
            pps_len = pp_nal[i].i_payload - 4;
            pps = (uint8_t *) malloc((size_t) (pps_len + 1));
            memcpy(pps,pp_nal[i].p_payload+4, (size_t) pps_len);
            sendSpsPps(sps, pps, sps_len, pps_len);
            free(sps);
            free(pps);
        } else{//关键帧或者非关键帧
            sendFrame(pp_nal[i].i_type, pp_nal[i].p_payload, pp_nal[i].i_payload);
        }
    }
    LOGE("视频编码结束");
    pthread_mutex_unlock(&mutex);
}

void VideoLive::sendSpsPps(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len) {
    //看表
    int bodySize = 13 + sps_len + 3 + pps_len;
    RTMPPacket *packet = new RTMPPacket;
    //
    RTMPPacket_Alloc(packet, bodySize);
    int i = 0;
    //固定头
    packet->m_body[i++] = 0x17;
    //类型
    packet->m_body[i++] = 0x00;
    //composition time 0x000000
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;

    //版本
    packet->m_body[i++] = 0x01;
    //编码规格
    packet->m_body[i++] = sps[1];
    packet->m_body[i++] = sps[2];
    packet->m_body[i++] = sps[3];
    packet->m_body[i++] = 0xFF;

    //整个sps
    packet->m_body[i++] = 0xE1;
    //sps长度
    packet->m_body[i++] = (sps_len >> 8) & 0xff;
    packet->m_body[i++] = sps_len & 0xff;
    memcpy(&packet->m_body[i], sps, sps_len);
    i += sps_len;

    //pps
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = (pps_len >> 8) & 0xff;
    packet->m_body[i++] = (pps_len) & 0xff;
    memcpy(&packet->m_body[i], pps, pps_len);

    //视频
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = bodySize;
    //随意分配一个管道（尽量避开rtmp.c中使用的）
    packet->m_nChannel = 10;
    //sps pps没有时间戳
    packet->m_nTimeStamp = 0;
    //不使用绝对时间
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;

    callBack(packet);
}

void VideoLive::sendFrame(int type, uint8_t *payload, int i_payload) {
    //去掉 00 00 00 01 / 00 00 01
    if (payload[2] == 0x00) {
        i_payload -= 4;
        payload += 4;
    } else {
        i_payload -= 3;
        payload += 3;
    }
    //看表
    int bodySize = 9 + i_payload;
    RTMPPacket *packet = new RTMPPacket;
    //
    RTMPPacket_Alloc(packet, bodySize);

    packet->m_body[0] = 0x27;
    if(type == NAL_SLICE_IDR){
        packet->m_body[0] = 0x17;
        LOGE("关键帧");
    }
    //类型
    packet->m_body[1] = 0x01;
    //时间戳
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;
    //数据长度 int 4个字节
    packet->m_body[5] = (i_payload >> 24) & 0xff;
    packet->m_body[6] = (i_payload >> 16) & 0xff;
    packet->m_body[7] = (i_payload >> 8) & 0xff;
    packet->m_body[8] = (i_payload) & 0xff;

    //图片数据
    memcpy(&packet->m_body[9], payload, i_payload);

    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = bodySize;
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nChannel = 0x10;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    callBack(packet);
}

void VideoLive::setVideoCallBack(VideoLive::videoCallBack callBack1) {
    this->callBack = callBack1;
}