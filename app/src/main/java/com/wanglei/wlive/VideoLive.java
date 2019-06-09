package com.wanglei.wlive;

import android.app.Activity;
import android.util.Log;
import android.view.SurfaceHolder;

import com.wanglei.wlive.utils.CameraUtils;

public class VideoLive implements CameraUtils.OnChangedSizeListener, CameraUtils.cameraDataListener {

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
        cameraUtils = CameraUtils.getInstance(activity,cameraId,width,height);
        cameraUtils.setPreviewCallback(this);
        cameraUtils.setOnChangedSizeListener(this);
    }

    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        cameraUtils.setPreviewDisplay(surfaceHolder);
    }

    public void autoFacus(){
        cameraUtils.autoFocus();
    }

    public int getCurrentCameraType(){
        return cameraUtils.getCurrentCameraType();
    }

    public void takePic(CameraUtils.TakePictureListener takePictureListener){
        cameraUtils.takePic(takePictureListener);
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

    public void release() {
        if(null!=cameraUtils){
            cameraUtils.release();
        }
    }

    @Override
    public void onPreviewFrame(byte[] nv21,
                               int width, int height, boolean needRotate, int degree) {
        if (isLiving) {
            //将相机预览的数据进行编码
            Log.e("ffmpeg","VideoLive onPreviewFrame->"+nv21.length);
            mLivePusher.native_pushVideo(nv21,width,height,needRotate,degree);
        }
    }
}
