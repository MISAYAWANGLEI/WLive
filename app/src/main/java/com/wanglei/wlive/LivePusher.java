package com.wanglei.wlive;

import android.app.Activity;
import android.view.SurfaceHolder;
import android.widget.Toast;

public class LivePusher implements AudioLive.OnAudioCaptureListener {

    static {
        System.loadLibrary("native-lib");
    }

    private AudioLive audioLive;
    private VideoLive videoLive;
    private Activity activity;

    public LivePusher(Activity activity, int width, int height, int bitrate,
                      int fps, int cameraId) {
        this.activity = activity;
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
    }

    //NDK回调java
    public void onPrepare(int isSuccess){
        if(isSuccess == 1){
            videoLive.startLive();
            audioLive.startLive();
        }else {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity,"rtmp创建或者链接失败",Toast.LENGTH_LONG).show();
                }
            });
        }
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
