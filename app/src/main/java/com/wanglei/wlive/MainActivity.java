package com.wanglei.wlive;

import android.Manifest;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import kr.co.namee.permissiongen.PermissionFail;
import kr.co.namee.permissiongen.PermissionGen;
import kr.co.namee.permissiongen.PermissionSuccess;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("native-lib");
    }

   private LivePusher livePusher;
   private SurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surfaceView);
        livePusher = new LivePusher(this, 800, 480,
                800_000, 10, Camera.CameraInfo.CAMERA_FACING_BACK);
        PermissionGen.with(MainActivity.this)
                .addRequestCode(100)
                .permissions(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA)
                .request();
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
}
