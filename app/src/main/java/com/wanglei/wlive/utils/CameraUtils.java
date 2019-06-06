package com.wanglei.wlive.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Camera操作的工具类：打开，关闭，切换摄像头等等
 */
public class CameraUtils implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "CameraUtils";
    private Activity mActivity;
    private int mHeight;
    private int mWidth;
    private int mCameraId;
    private Camera mCamera;
    private byte[] buffer;
    private SurfaceHolder mSurfaceHolder;
    private cameraDataListener mPreviewCallback;
    private int mRotation;
    private OnChangedSizeListener mOnChangedSizeListener;
    private boolean isStartPreview = false;
    private boolean isFocusing = false;

    private static CameraUtils mInstance = null;

    private CameraUtils(Activity activity, int cameraId, int width, int height) {
        mActivity = activity;
        mCameraId = cameraId;
        mWidth = width;
        mHeight = height;
    }

    public static CameraUtils getInstance(Activity activity,
                                          int cameraId, int width, int height) {
        if (mInstance == null) {
            mInstance = new CameraUtils(activity, cameraId, width, height);
        }
        return mInstance;
    }

    public void switchCamera() {
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        stopPreview();
        startPreview();
    }

    private void stopPreview() {
        if (mCamera != null) {
            //预览数据回调接口
            mCamera.setPreviewCallback(null);
            //停止预览
            mCamera.stopPreview();
            //释放摄像头
            mCamera.release();
            mCamera = null;
            isStartPreview = false;
            isFocusing = false;
        }
    }

    private void startPreview() {
        try {
            //获得camera对象
            mCamera = Camera.open(mCameraId);
            //配置camera的属性
            Camera.Parameters parameters = mCamera.getParameters();
            //设置预览数据格式为nv21
            parameters.setPreviewFormat(ImageFormat.NV21);
            List<String> focusModes = parameters.getSupportedFocusModes();
            //自动对焦的模式
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            //mWidth * mHeight
            //设置相机预览尺寸
            setPreviewSize(parameters);
            //设置相机生成图片图像
            //parameters.setPictureSize();
            // 设置摄像头 图像传感器的角度、方向
            setPreviewOrientation(parameters);
            mCamera.setParameters(parameters);
            //相机拍摄每帧图片为NV21格式，数据量大小为mWidth * mHeight * 3 / 2
            buffer = new byte[mWidth * mHeight * 3 / 2];
            //数据缓存区
            mCamera.addCallbackBuffer(buffer);
            mCamera.setPreviewCallbackWithBuffer(this);
            //设置预览画面
            mCamera.setPreviewDisplay(mSurfaceHolder);
            //回调外部通知外部相机真实预览的宽高，可能与使用者设置的宽高不一样
            mOnChangedSizeListener.onChanged(mWidth, mHeight);
            mCamera.startPreview();
            isStartPreview = true;
            isFocusing = false;
            autoFocus();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public interface TakePictureListener{
        void onTakePicSuccess(Bitmap bitmap);
    }
    private TakePictureListener takePictureListener;

    public void takePic(TakePictureListener takePictureListener){

        this.takePictureListener = takePictureListener;
        // 先聚焦，聚焦成功后在拍照
        if (mCamera!=null){
            mCamera.autoFocus(mAutoFocusCallback);
        }
    }

    // 聚焦回调
    @SuppressWarnings("deprecation")
    private Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {
                // takePicture(Camera.ShutterCallback shutter,
                // Camera.PictureCallback raw, Camera.PictureCallback jpeg)
                // shutter是快门按下时的回调，raw是获取拍照原始数据的回调，jpeg是获取经过压缩成jpg格式的图像数据的回调
                mCamera.takePicture(null, null, mPictureCallback);
            }
        }
    };

    // 拍照回调
    @SuppressWarnings("deprecation")
    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
            File pictureDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

            @SuppressWarnings("static-access")
            final String picturePath = pictureDir
                    + File.separator
                    + new DateFormat().format("yyyyMMddHHmmss", new Date())
                    .toString() + ".jpg";
            new Thread(new Runnable() {
                @Override
                public void run() {
                    File file = new File(picturePath);
                    try {
                        //
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0,
                                data.length);
                        bitmap = rotateBitmapByDegree(bitmap);
                        //拍照成功回调给外部调用者
                        if (takePictureListener!=null){
                            takePictureListener.onTakePicSuccess(bitmap);
                        }
                        BufferedOutputStream bos = new BufferedOutputStream(
                                new FileOutputStream(file));
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                        bos.flush();
                        bos.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            // 拍照完继续预览
            mCamera.startPreview();
        }
    };

    /**
     * 处理拍完照的照片：后置摄像头拍的则旋转90度，
     * 前置摄像头拍的先沿相机传感器方向X轴方向镜像，然后将镜像旋转180度
     * @return
     */
    private Bitmap rotateBitmapByDegree(Bitmap bm) {
        Bitmap returnBm = null;
        Matrix matrix = new Matrix();
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            //
            matrix.postRotate(90);
            Log.i("WL", "CAMERA_FACING_BACK");
        } else {
            //
            Log.i("WL", "CAMERA_FACING_FRONT");
            matrix.postScale(-1, 1);// 沿相机传感器方向X轴方向镜像
            matrix.postRotate(90);
        }
        try {
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),
                    bm.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bm;
        }
        if (bm != returnBm) {
            bm.recycle();
        }
        return returnBm;
    }

    // 判断相机是否支持 :暂时没用到
    public boolean checkCameraHardware(Context context) {
        //
        return context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA);
    }



    //自动对焦
    public void autoFocus() {
        try {
            if (mCamera != null && !isFocusing && isStartPreview) { //camera不为空，并且isFocusing=false的时候才去对焦
                mCamera.cancelAutoFocus();
                isFocusing = true;
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        isFocusing = false;
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setPreviewOrientation(Camera.Parameters parameters) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        mRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (mRotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90: // 横屏 左边是头部(home键在右边)
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:// 横屏 头部在右边
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        //设置角度
        mCamera.setDisplayOrientation(result);
    }

    /**
     * 设置相机预览的宽高信息，外部设置的宽高可能当前设备不支持，获取一个与
     * 设置的宽高最接近的宽高
     * @param parameters
     */
    private void setPreviewSize(Camera.Parameters parameters) {
        //获取摄像头支持的宽、高
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size size = supportedPreviewSizes.get(0);
        Log.d(TAG, "当前设备支持的宽高->" + size.width + "x" + size.height);
        //选择一个与设置的差距最小的支持分辨率
        int m = Math.abs(size.height * size.width - mWidth * mHeight);
        supportedPreviewSizes.remove(0);
        Iterator<Camera.Size> iterator = supportedPreviewSizes.iterator();
        //遍历
        while (iterator.hasNext()) {
            Camera.Size next = iterator.next();
            Log.d(TAG, "当前设备支持的宽高->" + next.width + "x" + next.height);
            int n = Math.abs(next.height * next.width - mWidth * mHeight);
            if (n < m) {
                m = n;
                size = next;
            }
        }
        mWidth = size.width;
        mHeight = size.height;
        parameters.setPreviewSize(mWidth, mHeight);//预览尺寸
        Log.d(TAG, "设置预览分辨率 width:" + size.width + " height:" + size.height);
    }

    //设置预览的SurfaceHolder
    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        mSurfaceHolder = surfaceHolder;
        mSurfaceHolder.addCallback(this);
    }

    public void setPreviewCallback(cameraDataListener previewCallback) {
        mPreviewCallback = previewCallback;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //释放摄像头
        stopPreview();
        //开启摄像头
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        switch (mRotation) {
            case Surface.ROTATION_0:
                //rotation90(data);
                if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    //后置摄像头顺时针旋转90度
                    mPreviewCallback.onPreviewFrame(data,mWidth,mHeight,true,90);
                } else {
                    //逆时针旋转90度,相当于顺时针旋转270
                    mPreviewCallback.onPreviewFrame(data,mWidth,mHeight,true,270);
                }
                break;
            case Surface.ROTATION_90: // 横屏 左边是头部(home键在右边)
                mPreviewCallback.onPreviewFrame(data,mWidth,mHeight,false,0);
                break;
            case Surface.ROTATION_270:// 横屏 头部在右边
                mPreviewCallback.onPreviewFrame(data,mWidth,mHeight,false,0);
                break;
        }
        camera.addCallbackBuffer(buffer);
    }

    public void setOnChangedSizeListener(OnChangedSizeListener listener) {
        mOnChangedSizeListener = listener;
    }

    public interface OnChangedSizeListener {
        void onChanged(int w, int h);
    }

    public void release() {
        mSurfaceHolder.removeCallback(this);
        stopPreview();
    }

    public interface cameraDataListener{
        void onPreviewFrame(byte[] nv21,int width,int height,boolean needRotate,int degree);
    }

}
