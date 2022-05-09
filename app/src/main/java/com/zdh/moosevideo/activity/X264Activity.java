package com.zdh.moosevideo.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Camera;
import android.os.Bundle;

import com.zdh.live.AudioChannel;
import com.zdh.live.LivePusher;
import com.zdh.live.VideoChannel;
import com.zdh.moosevideo.CameraHelper;
import com.zdh.moosevideo.CameraXHelper;
import com.zdh.moosevideo.databinding.ActivityX264Binding;

public class X264Activity extends AppCompatActivity {

    static {
        System.loadLibrary("live");
    }
    ActivityX264Binding mBinding;
    VideoChannel videoPush;
    VideoChannel videoChannel;
    AudioChannel audioChannel;
    LivePusher livePusher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityX264Binding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        av();

        videoPush = new VideoChannel(480, 640, 15,1600_000);
        mBinding.btnStart.setOnClickListener(v-> {
//            videoPush.start("rtmp://live-push.bilivideo.com/live-bvc/?streamname=live_11852946_29221900&key=e8d63f1642891cfa3b342276147f62ca&schedule=rtmp&pflag=1");
            videoChannel.start("rtmp://live-push.bilivideo.com/live-bvc/?streamname=live_11852946_29221900&key=e8d63f1642891cfa3b342276147f62ca&schedule=rtmp&pflag=1");

        });
    }

    public void av() {
        videoChannel = new VideoChannel(480, 640, 15, 1600_000);
        audioChannel = new AudioChannel(44100, 1);
        livePusher = new LivePusher(audioChannel, videoChannel);
        audioChannel.start();
        CameraXHelper cameraXHelper = new CameraXHelper(this, this, mBinding.previewView);
        cameraXHelper.setListener(new CameraXHelper.OnCameraXHelperListener() {
            @Override
            public void onAnalyze(byte[] yuv) {
                if(videoChannel != null) {
                    videoChannel.pushVideoToNative(yuv);
                }
            }

            @Override
            public void onSizeChanged(int width, int height) {
                if (videoChannel != null) {
                    videoChannel.setVideoEncInfo(height,width);
                }
            }
        });
    }

    public void cameraX() {
        CameraXHelper cameraXHelper = new CameraXHelper(this, this, mBinding.previewView);
        cameraXHelper.setListener(new CameraXHelper.OnCameraXHelperListener() {
            @Override
            public void onAnalyze(byte[] yuv) {
                if(videoPush != null) {
                    videoPush.pushVideoToNative(yuv);
                }
            }

            @Override
            public void onSizeChanged(int width, int height) {
                if (videoPush != null) {
                    videoPush.setVideoEncInfo(height,width);
                }
            }
        });
    }

    public void camera() {
        CameraHelper cameraHelper = new CameraHelper(this, Camera.CameraInfo.CAMERA_FACING_BACK, 640,480);
        cameraHelper.setPreviewDisplay(mBinding.surfaceView.getHolder());
        cameraHelper.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                videoPush.pushVideoToNative(data);
            }
        });
        cameraHelper.setOnChangedSizeListener(new CameraHelper.OnChangedSizeListener() {
            @Override
            public void onChanged(int w, int h) {
                videoPush.setVideoEncInfo(h,w);
            }
        });
    }

    public void camera2(){
        //        Camera2Helper camera2Helper = new Camera2Helper(this, mBinding.textureView, new Camera2Helper.onCameraPreviewListener() {
//            @Override
//            public void onSizeChanged(Size size) {
//                videoPush.setVideoEncInfo(size.getHeight(),size.getWidth());
//            }
//
//            @Override
//            public void onReceiveData(byte[] yuv) {
//                videoPush.pushVideoToNative(yuv);
//            }
//        });
    }
}