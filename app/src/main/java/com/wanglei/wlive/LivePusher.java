package com.wanglei.wlive;

import android.app.Activity;
import android.view.Surface;
import android.widget.Toast;

import com.wanglei.cameralibrary.CameraView;

public class LivePusher implements AudioLive.OnAudioCaptureListener {

    static {
        System.loadLibrary("native-lib");
    }

    private AudioLive audioLive;
    private Activity activity;
    private int mBitrate;
    private int mFps;
    private boolean isLiving;
    private int mWidth;
    private int mHeight;

    public LivePusher(Activity activity, int bitrate,
                      int fps) {
        this.activity = activity;
        this.mBitrate = bitrate;
        this.mFps = fps;
        native_init();
        audioLive = new AudioLive(this);
        audioLive.setOnAudioCaptureListener(this);
    }

    //开始直播：推送数据
    public void startLive(String path) {
        native_start(path);
    }

    //NDK回调java
    public void onPrepare(int isSuccess){
        if(isSuccess == 1){
            isLiving = true;
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

    //停止直播
    public void stopLive(){
        isLiving = false;
        audioLive.stopLive();
        native_stop();
    }

    public void release(){
        audioLive.release();
        native_release();
    }

    public void onPreviewSizeConfirm(int w, int h) {
        //初始化编码器
        mWidth = w;
        mHeight = h;
        native_setVideoEncoderInfo(w, h, mFps, mBitrate);
    }

    private boolean needRotate = false;
    private int degree = 0;
    public void onPreviewFrame(byte[] nv21, CameraView mCameraView) {
        if (isLiving) {
            int mRotation = mCameraView.getCameraOrientation();//角度
            int mCameraId = mCameraView.getFacing();//前置还是后置摄像头
            switch (mRotation) {
                case Surface.ROTATION_0:
                    //rotation90(data);
                    if (mCameraId == CameraView.FACING_BACK) {
                        //后置摄像头顺时针旋转90度
                        needRotate = true;
                        degree = 90;
                    } else {
                        //逆时针旋转90度,相当于顺时针旋转270
                        needRotate = true;
                        degree = 270;
                    }
                    break;
            }
            //将相机预览的数据进行编码
            native_pushVideo(nv21,mWidth,mHeight,needRotate,degree);
        }
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
    public native void native_pushVideo(byte[] nv21,
                                        int width, int height, boolean needRotate, int degree);
    public native void native_pushAudio(byte[] data);
}
