package com.wanglei.wlive.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.Calendar;

/**
 * 相机的自动对焦实现：监控手机移动，移动后静置则对焦一次
 */
public class SensorControler implements SensorEventListener {
    public static final String TAG = "SensorControler";
    private SensorManager mSensorManager;
    private Sensor mSensor;
    // 手机状态
    public static final int STATUS_NONE = 0;// 未知
    public static final int STATUS_STATIC = 1;// 静止
    public static final int STATUS_MOVE = 2;// 移动
    // 默认未知状态
    private int STATUE = STATUS_NONE;

    private CameraFocusListener mCameraFocusListener;

    private static SensorControler mInstance;

    private SensorControler(Context context) {
        mSensorManager = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public static SensorControler getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SensorControler(context);
        }
        return mInstance;
    }

    public void setCameraFocusListener(CameraFocusListener mCameraFocusListener) {
        this.mCameraFocusListener = mCameraFocusListener;
    }

    public void onStart() {
        restParams();
        mSensorManager.registerListener(this, mSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void restParams() {
        STATUE = STATUS_NONE;
        mX = 0;
        mY = 0;
        mZ = 0;
    }

    public void onStop() {
        mSensorManager.unregisterListener(this, mSensor);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private int mX, mY, mZ;
    private Calendar mCalendar;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == null) {
            return;
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            int x = (int) event.values[0];
            int y = (int) event.values[1];
            int z = (int) event.values[2];
            mCalendar = Calendar.getInstance();
            long stamp = mCalendar.getTimeInMillis();
            if (STATUE != STATUS_NONE) {
                int px = Math.abs(mX - x);
                int py = Math.abs(mY - y);
                int pz = Math.abs(mZ - z);
                Log.i(TAG, "pX:" + px + "  pY:" + py + "  pZ:" + pz
                        + "    stamp:" + stamp);
                double value = Math.sqrt(px * px + py * py + pz * pz);
                Log.i(TAG, "value:" + value);
                if (value > 1.4) {// 设备移动状态
                    Log.i(TAG, "mobile moving");
                    STATUE = STATUS_MOVE;
                } else {// 设备静止状态
                    Log.i(TAG, "mobile static");
                    if (STATUE == STATUS_MOVE) {// 上一次是移动状态
                        Log.i(TAG, "调用onFocus");
                        // 移动后静止，可以发生对焦行为
                        if (mCameraFocusListener != null) {
                            mCameraFocusListener.onFocus();
                        }
                    }
                    STATUE = STATUS_STATIC;
                }
            } else {
                Log.i(TAG, "STATUS_NONE");
                STATUE = STATUS_STATIC;
            }
            mX = x;
            mY = y;
            mZ = z;
        }
    }

    public interface CameraFocusListener {
        void onFocus();
    }
}
