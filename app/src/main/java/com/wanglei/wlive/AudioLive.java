package com.wanglei.wlive;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioLive {
    private static final String TAG = AudioLive.class.getSimpleName();
    private AudioRecord mAudioRecord;
    public static final int DEFAULT_SAMPLE_RATE = 44100; //默认采样率
    //AudioFormat.CHANNEL_IN_STEREO 双声道
    public static final int DEFAULT_CHANNEL = AudioFormat.CHANNEL_IN_MONO; //默认单通道
    public static final int SIMPLE_FORMAT = AudioFormat.ENCODING_PCM_16BIT; //16位量化
    private static final int DEFAULT_SOURCE_MIC = MediaRecorder.AudioSource.MIC; //声音从麦克风采集而来
    private boolean isLiving = false;
    private Thread captureThread;
    private int encodeinputSize;//faac编码器一次能放入的数据
    private LivePusher livePusher;

    public AudioLive(LivePusher livePusher){
        this.livePusher = livePusher;
        livePusher.native_setAudioEncInfo(DEFAULT_SAMPLE_RATE,1);
        mAudioRecord = createAudioRecord(DEFAULT_SOURCE_MIC, DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL, SIMPLE_FORMAT);
    }

    private AudioRecord createAudioRecord(int audioSource, int simpleRate, int channels, int audioFormat) {
        encodeinputSize = livePusher.getInputSamples() * 2;//livePusher.getInputSamples()返回的是样本量，每个样本16位表示，2字节
        //获取一帧音频帧的大小：调用系统提供的方法即可
        int minBufferSize = AudioRecord.getMinBufferSize(simpleRate, channels, audioFormat);
        if (minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.d(TAG, "获取音频帧大小失败!");
            return null;
        }
        int audioRecordBufferSize = minBufferSize * 2; //AudioRecord内部缓冲设置为4帧音频帧的大小句

        int audioBufferSize = audioRecordBufferSize > encodeinputSize?audioRecordBufferSize:encodeinputSize;

        AudioRecord audioRecord = new AudioRecord(audioSource,
                simpleRate, channels, audioFormat, audioBufferSize);
        if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            Log.d(TAG, "初始化AudioRecord失败!");
            return null;
        }
        return audioRecord;
    }

    public void startLive() {
        isLiving = true;
        mAudioRecord.startRecording(); //开始录制
        //开辟线程, 从 AudioRecord 中的缓冲区将音频数据读出来
        captureThread = new Thread(new AudioCaptureRunnable());
        captureThread.start();
    }

    public void stopLive() {
        isLiving = false;
    }

    public void release() {
        if(mAudioRecord!=null){
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }

    private class AudioCaptureRunnable implements Runnable {

        @Override
        public void run() {

            byte[] buffer = new byte[encodeinputSize]; //每次取faac编码器能盛放的数据量
            while (isLiving) {
                int result = mAudioRecord.read(buffer, 0, buffer.length);
                if (result == AudioRecord.ERROR_BAD_VALUE) {
                    Log.d(TAG, "run: ERROR_BAD_VALUE");
                } else if (result == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.d(TAG, "run: ERROR_INVALID_OPERATION");
                } else {
                    if (listener != null) {
                        Log.d(TAG, "run: capture buffer length is " + result);
                        listener.onAudioFrameCaptured(buffer);
                    }
                }
            }
            mAudioRecord.stop();
        }
    }

    private OnAudioCaptureListener listener;

    public void setOnAudioCaptureListener(OnAudioCaptureListener listener) {
        this.listener = listener;
    }

    //采集到的数据通过回调接口传到外部
    public interface OnAudioCaptureListener {
        void onAudioFrameCaptured(byte[] bytes);
    }

}
