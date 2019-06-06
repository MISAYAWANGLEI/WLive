//
// Created by wanglei55 on 2019/6/4.
//
#include "WYuvUtils.h"


/**
 * nv21数据转i420
 * NV21->YUV420SP
 * I420->YUV420P
 * @param src_nv21_data
 * @param width
 * @param height
 * @param dst_i420_data
 */
void WYuvUtils::nv21ToI420(signed char *src_nv21_data, int width, int height,
        signed char *dst_i420_data) {

    int src_y_size = width * height;
    int src_u_size = (width >> 1) * (height >> 1);

    signed char *src_nv21_y_data = src_nv21_data;
    signed char *src_nv21_vu_data = src_nv21_data + src_y_size;

    signed char *dst_i420_y_data = dst_i420_data;
    signed char *dst_i420_u_data = dst_i420_data + src_y_size;
    signed char *dst_i420_v_data = dst_i420_data + src_y_size + src_u_size;

    libyuv::NV21ToI420((const uint8_t *)src_nv21_y_data, width,
                       (const uint8_t *)src_nv21_vu_data, width,
                       (uint8_t *)dst_i420_y_data, width,
                       (uint8_t *)dst_i420_u_data, width >> 1,
                       (uint8_t *)dst_i420_v_data, width >> 1,
               width, height);
}

void WYuvUtils::i420ToNV21(signed char *src_i420_data, int width, int height,
                           signed char *dst_nv21_data) {

    int src_y_size = width * height;
    int src_u_size = (width >> 1) * (height >> 1);

    signed char *src_i420_y_data = src_i420_data;
    signed char *src_i420_u_data = src_i420_data + src_y_size;
    signed char *src_i420_v_data = src_i420_data + src_y_size + src_u_size;

    signed char *dst_nv21_y_data = dst_nv21_data;
    signed char *dst_nv21_uv_data = dst_nv21_data + src_y_size;

    libyuv::I420ToNV21(
            (const uint8_t *) src_i420_y_data, width,
            (const uint8_t *) src_i420_u_data, width >> 1,
            (const uint8_t *) src_i420_v_data, width >> 1,
            (uint8_t *) dst_nv21_y_data, width,
            (uint8_t *) dst_nv21_uv_data, width,
            width, height);
}

void WYuvUtils:: scaleI420(signed char *src_i420_data, int width, int height,
        signed char *dst_i420_data, int dst_width,
               int dst_height, int mode) {

    int src_i420_y_size = width * height;
    int src_i420_u_size = (width >> 1) * (height >> 1);
    signed char *src_i420_y_data = src_i420_data;
    signed char *src_i420_u_data = src_i420_data + src_i420_y_size;
    signed char *src_i420_v_data = src_i420_data + src_i420_y_size + src_i420_u_size;

    int dst_i420_y_size = dst_width * dst_height;
    int dst_i420_u_size = (dst_width >> 1) * (dst_height >> 1);
    signed char *dst_i420_y_data = dst_i420_data;
    signed char *dst_i420_u_data = dst_i420_data + dst_i420_y_size;
    signed char *dst_i420_v_data = dst_i420_data + dst_i420_y_size + dst_i420_u_size;
    //缩放mode说明
    //kFilterNone = 0,      // Point sample; Fastest.
    //kFilterLinear = 1,    // Filter horizontally only.
    //kFilterBilinear = 2,  // Faster than box, but lower quality scaling down.
    //kFilterBox = 3        // Highest quality.
    libyuv::I420Scale((const uint8_t *) src_i420_y_data, width,
                      (const uint8_t *) src_i420_u_data, width >> 1,
                      (const uint8_t *) src_i420_v_data, width >> 1,
                      width, height,
                      (uint8_t *) dst_i420_y_data, dst_width,
                      (uint8_t *) dst_i420_u_data, dst_width >> 1,
                      (uint8_t *) dst_i420_v_data, dst_width >> 1,
                      dst_width, dst_height,
                      (libyuv::FilterMode) mode);
}

void WYuvUtils::rotateI420(signed char *src_i420_data, int width,
        int height, signed char *dst_i420_data, int degree) {

    int src_i420_y_size = width * height;
    int src_i420_u_size = (width >> 1) * (height >> 1);

    signed char *src_i420_y_data = src_i420_data;
    signed char *src_i420_u_data = src_i420_data + src_i420_y_size;
    signed char *src_i420_v_data = src_i420_data + src_i420_y_size + src_i420_u_size;

    signed char *dst_i420_y_data = dst_i420_data;
    signed char *dst_i420_u_data = dst_i420_data + src_i420_y_size;
    signed char *dst_i420_v_data = dst_i420_data + src_i420_y_size + src_i420_u_size;

    if (degree == libyuv::kRotate90 || degree == libyuv::kRotate270) {
        //要注意这里的width和height在旋转之后是相反的
        libyuv::I420Rotate((const uint8_t *) src_i420_y_data, width,
                           (const uint8_t *) src_i420_u_data, width >> 1,
                           (const uint8_t *) src_i420_v_data, width >> 1,
                           (uint8_t *) dst_i420_y_data, height,
                           (uint8_t *) dst_i420_u_data, height >> 1,
                           (uint8_t *) dst_i420_v_data, height >> 1,
                           width, height,
                           (libyuv::RotationMode) degree);
    } else if (degree == libyuv::kRotate180){
        //图像宽高不变
        libyuv::I420Rotate((const uint8_t *) src_i420_y_data, width,
                           (const uint8_t *) src_i420_u_data, width >> 1,
                           (const uint8_t *) src_i420_v_data, width >> 1,
                           (uint8_t *) dst_i420_y_data, width,
                           (uint8_t *) dst_i420_u_data, width >> 1,
                           (uint8_t *) dst_i420_v_data, width >> 1,
                           width, height,
                           (libyuv::RotationMode) degree);
    }
}

void WYuvUtils::mirrorI420(signed char *src_i420_data,
        int width, int height, signed char *dst_i420_data) {
    int src_i420_y_size = width * height;
    int src_i420_u_size = (width >> 1) * (height >> 1);

    signed char  *src_i420_y_data = src_i420_data;
    signed char  *src_i420_u_data = src_i420_data + src_i420_y_size;
    signed char  *src_i420_v_data = src_i420_data + src_i420_y_size + src_i420_u_size;

    signed char  *dst_i420_y_data = dst_i420_data;
    signed char  *dst_i420_u_data = dst_i420_data + src_i420_y_size;
    signed char  *dst_i420_v_data = dst_i420_data + src_i420_y_size + src_i420_u_size;

    libyuv::I420Mirror((const uint8_t *) src_i420_y_data, width,
                       (const uint8_t *) src_i420_u_data, width >> 1,
                       (const uint8_t *) src_i420_v_data, width >> 1,
                       (uint8_t *) dst_i420_y_data, width,
                       (uint8_t *) dst_i420_u_data, width >> 1,
                       (uint8_t *) dst_i420_v_data, width >> 1,
                       width, height);
}

void WYuvUtils::cropYUV(signed char *src_data, int src_length,int width, int height,
        signed char *dst_i420_data,
                        int dst_width, int dst_height, int left, int top) {

    if (left + dst_width > width || top + dst_height > height) {
        return;
    }
    //libyuv做视频裁剪时，cropXY只能是偶数，否则会出现颜色出错现象
    if (left % 2 != 0 || top % 2 != 0) {
        return;
    }

    int dst_i420_y_size = dst_width * dst_height;
    int dst_i420_u_size = (dst_width >> 1) * (dst_height >> 1);

    signed char *dst_i420_y_data = dst_i420_data;
    signed char *dst_i420_u_data = dst_i420_data + dst_i420_y_size;
    signed char *dst_i420_v_data = dst_i420_data + dst_i420_y_size + dst_i420_u_size;
    //Convert camera sample to I420 with cropping, rotation and vertical flip
    libyuv::ConvertToI420((const uint8_t *) src_data, src_length,
                          (uint8_t *) dst_i420_y_data, dst_width,
                          (uint8_t *) dst_i420_u_data, dst_width >> 1,
                          (uint8_t *) dst_i420_v_data, dst_width >> 1,
                          left, top,
                          width, height,
                          dst_width, dst_height,
                          libyuv::kRotate0, libyuv::FOURCC_I420);
}