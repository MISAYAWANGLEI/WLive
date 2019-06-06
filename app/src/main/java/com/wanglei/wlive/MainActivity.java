package com.wanglei.wlive;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.wanglei.wlive.utils.CameraUtils;
import com.wanglei.wlive.utils.SensorControler;

import kr.co.namee.permissiongen.PermissionFail;
import kr.co.namee.permissiongen.PermissionGen;
import kr.co.namee.permissiongen.PermissionSuccess;

public class MainActivity extends AppCompatActivity implements SensorControler.CameraFocusListener {

    static {
        System.loadLibrary("native-lib");
    }
    private ImageView ivFoucView;
   private LivePusher livePusher;
   private SurfaceView surfaceView;
    //加速度传感器
    private SensorControler sensorControler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置无标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surfaceView);
        ivFoucView = findViewById(R.id.iv_focus);
        livePusher = new LivePusher(this, 720, 1280,
                800_000, 30, Camera.CameraInfo.CAMERA_FACING_BACK);
        sensorControler = SensorControler.getInstance(this);
        sensorControler.setCameraFocusListener(this);
        PermissionGen.with(MainActivity.this)
                .addRequestCode(100)
                .permissions(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA)
                .request();
    }

    @Override
    protected void onStart() {
        super.onStart();
        sensorControler.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (null != surfaceView) {
            surfaceView.setVisibility(View.VISIBLE);
        }
        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressWarnings("deprecation")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getPointerCount() == 1 && event.getAction() == MotionEvent.ACTION_DOWN) {
                    startAutoFocus(event.getX(), event.getY());
                }
                return true;
            }
        });
    }

    public void startAutoFocus(float x, float y) {
        //后置摄像头才有对焦功能
        if (livePusher != null && livePusher.getCurrentCameraType() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return;
        }
        if (x != -1 && y != -1) { //这里有一个对焦的动画
            //设置位置和初始状态
            ivFoucView.setTranslationX(x - (ivFoucView.getWidth()) / 2);
            ivFoucView.setTranslationY(y - (ivFoucView.getWidth()) / 2);
            ivFoucView.clearAnimation();

            //执行动画
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivFoucView, "scaleX", 1.75f, 1.0f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivFoucView, "scaleY", 1.75f, 1.0f);
            AnimatorSet animSet = new AnimatorSet();
            animSet.play(scaleX).with(scaleY);
            animSet.setDuration(500);
            animSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    ivFoucView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    ivFoucView.setVisibility(View.GONE);
                }
            });
            animSet.start();
        }
        livePusher.autoFacus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != surfaceView) {
            //surfaceView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorControler.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        PermissionGen.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @PermissionSuccess(requestCode = 100)
    public void doSomething(){
        Toast.makeText(this, "PermissionSuccess", Toast.LENGTH_SHORT).show();
        livePusher.setPreviewDisplay(surfaceView.getHolder());//立刻开启摄像头进行预览
    }

    @PermissionFail(requestCode = 100)
    public void doFailSomething(){
        Toast.makeText(this, "PermissionFail", Toast.LENGTH_SHORT).show();
    }

    public void switchCamera(View view) {
        livePusher.switchCamera();
    }


    public void startLive(View view) {
        livePusher.startLive("rtmp://localhost/myapp/mystream");
    }

    public void stopLive(View view) {
        livePusher.stopLive();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        livePusher.release();
    }

    @Override
    public void onFocus() {
        Log.i("MainActivity","onFocus");
        livePusher.autoFacus();
    }

    public void takepic(View view) {

        livePusher.takePic(new CameraUtils.TakePictureListener() {
            @Override
            public void onTakePicSuccess(Bitmap bitmap) {
               final int byteCount = bitmap.getByteCount();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,String.valueOf(byteCount),Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
