package com.wanglei.wlive;

import android.app.Activity;
import android.view.SurfaceHolder;

public class LivePusher implements AudioLive.OnAudioCaptureListener {

    static {
        System.loadLibrary("native-lib");
    }

    private AudioLive audioLive;
    private VideoLive videoLive;

    public LivePusher(Activity activity, int width, int height, int bitrate,
                      int fps, int cameraId) {
        native_init();
        videoLive = new VideoLive(this,activity, width,
                height, bitrate, fps, cameraId);
        audioLive = new AudioLive(this);
        audioLive.setOnAudioCaptureListener(this);
    }

    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        videoLive.setPreviewDisplay(surfaceHolder);
    }

    public void switchCamera() {
        videoLive.switchCamera();
    }

    public void startLive(String path) {
        native_start(path);
        videoLive.startLive();
        audioLive.startLive();
    }

    public void stopLive(){
        videoLive.stopLive();
        audioLive.stopLive();
        native_stop();
    }

    public void release(){
        videoLive.release();
        audioLive.release();
        native_release();
    }

    @Override
    public void onAudioFrameCaptured(byte[] bytes) {
        native_pushAudio(bytes);
    }

    //
    private native void native_init();
    private native void native_start(String path);
    private native void native_stop();
    private native void native_release();
    public native int getInputSamples();//获取编码器一次能输入数据的样本数
    public native void native_setAudioEncInfo(int sampleRateInHz, int channels);
    public native void native_setVideoEncoderInfo(int width,int height,int fps, int bitrate);
    public native void native_pushVideo(byte[] data);
    public native void native_pushAudio(byte[] data);
}
