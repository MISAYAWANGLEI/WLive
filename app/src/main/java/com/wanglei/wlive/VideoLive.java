package com.wanglei.wlive;

import android.app.Activity;
import android.hardware.Camera;
import android.view.SurfaceHolder;

import com.wanglei.wlive.utils.CameraUtils;

public class VideoLive implements CameraUtils.OnChangedSizeListener, Camera.PreviewCallback {

    private LivePusher mLivePusher;
    private CameraUtils cameraUtils;
    private int mBitrate;
    private int mFps;
    private boolean isLiving;

    public VideoLive(LivePusher livePusher, Activity activity, int width, int height,
                     int bitrate, int fps, int cameraId) {
        this.mLivePusher = livePusher;
        this.mBitrate = bitrate;
        this.mFps = fps;
        cameraUtils = new CameraUtils(activity,cameraId,width,height);
        cameraUtils.setPreviewCallback(this);
        cameraUtils.setOnChangedSizeListener(this);
    }

    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        cameraUtils.setPreviewDisplay(surfaceHolder);
    }

    public void switchCamera() {
        cameraUtils.switchCamera();
    }

    public void stopLive() {
        isLiving = false;
    }

    public void startLive() {
        isLiving = true;
    }

    @Override
    public void onChanged(int w, int h) {
        //初始化编码器
        mLivePusher.native_setVideoEncoderInfo(w, h, mFps, mBitrate);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (isLiving) {
            //将相机预览的数据进行编码
            mLivePusher.native_pushVideo(data);
        }
    }

    public void release() {
        if(null!=cameraUtils){
            cameraUtils.release();
        }
    }
}
