package com.zdh.moosevideo.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.media.MediaCodec;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import com.zdh.live.RTMPPackage;
import com.zdh.live.ScreenLive;
import com.zdh.mediacodec.AudioEncoder;
import com.zdh.mediacodec.h264.H264Encoder;
import com.zdh.moosevideo.databinding.ActivityProjectionLiveBinding;

import java.nio.ByteBuffer;

public class ProjectionLiveActivity extends AppCompatActivity {

    ActivityProjectionLiveBinding mBinding;
    MediaProjectionManager mediaProjectionManager;
    MediaProjection mediaProjection;
    H264Encoder h264Encoder;
    AudioEncoder audioEncoder;
    ScreenLive screenLive;
    ActivityResultLauncher<Intent> mMediaProjectionResult =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() != null) {
                    mediaProjection = mediaProjectionManager.getMediaProjection(result.getResultCode(), result.getData());
                    h264Encoder.init(mediaProjection, new H264Encoder.OnEncodeFrameListener() {
                        @Override
                        public void decodeOneFrame(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
                            byte[] buffer = new byte[bufferInfo.size];
                            byteBuffer.get(buffer);
                            screenLive.sendData(new RTMPPackage(RTMPPackage.RTMP_TYPE_VIDEO,
                                    buffer,bufferInfo.size,bufferInfo.presentationTimeUs));
                        }
                    });
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityProjectionLiveBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
        mBinding.btnOpenLive.setOnClickListener(v -> {
            projection();
            screenLive.start();
            audioEncoder.start();
        });
        screenLive = new ScreenLive("rtmp://live-push.bilivideo.com/live-bvc/?streamname=live_11852946_29221900&key=e8d63f1642891cfa3b342276147f62ca&schedule=rtmp&pflag=1");
        h264Encoder = new H264Encoder(screenLive);
        audioEncoder = new AudioEncoder(screenLive,new AudioEncoder.OnAudioEncodedOneFrameListener() {
            @Override
            public void onEncodedOneFreame(int type,ByteBuffer byteBuffer, MediaCodec.BufferInfo info) {
                byte[] buffer = new byte[info.size];
                byteBuffer.get(buffer);
                screenLive.sendData(new RTMPPackage(type,buffer,info.size,info.presentationTimeUs));
            }
        });
    }

    void projection() {
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = mediaProjectionManager.createScreenCaptureIntent();
        mMediaProjectionResult.launch(intent);
    }
}