//
// Created by wanglei55 on 2019/6/4.
//

#ifndef WLIVE_WYUVUTILS_H
#define WLIVE_WYUVUTILS_H


#include "libyuv/include/libyuv.h"

class WYuvUtils {
public:
    static void nv21ToI420(signed char *src_nv21_data, int width, int height,signed char *dst_i420_data);
    static void i420ToNV21(signed char *src_i420_data, int width, int height,signed char *dst_nv21_data);
    static void rotateI420(signed char *src_i420_data, int width, int height,
            signed char *dst_i420_data, int degree);
    static void scaleI420(signed char *src_i420_data, int width, int height,
                   signed char *dst_i420_data, int dst_width,
                   int dst_height, int mode);
    static void mirrorI420(signed char *src_i420_data, int width, int height, signed char *dst_i420_data);

    /**
     * 裁剪操作：输出I420格式数据
     * libyuv做视频裁剪时，cropXY只能是偶数，否则会出现颜色出错现象
     * @param src_data
     * @param width
     * @param height
     * @param dst_i420_data
     * @param dst_width
     * @param dst_height
     * @param left
     * @param top
     */
    static void cropYUV(signed char *src_data, int src_length,int width, int height,
            signed char *dst_i420_data,int dst_width, int dst_height,int left,int top);
};


#endif //WLIVE_WYUVUTILS_H
