package com.zdh.moosevideo.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import com.zdh.mediacodec.h265.H265Encoder;
import com.zdh.mediacodec.h265.SocketPushLive;
import com.zdh.moosevideo.databinding.ActivityPushBinding;

public class PushActivity extends AppCompatActivity {

    ActivityPushBinding mBinding;
    SocketPushLive socketPushLive;
    MediaProjectionManager mediaProjectionManager;
    MediaProjection mediaProjection;
    H265Encoder h265Encoder;

    ActivityResultLauncher<Intent> mMediaProjectionResult =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() != null) {
                    mediaProjection = mediaProjectionManager.getMediaProjection(result.getResultCode(), result.getData());
                    h265Encoder.init(mediaProjection,socketPushLive);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityPushBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        h265Encoder = new H265Encoder();
        socketPushLive = new SocketPushLive();
        socketPushLive.init();
        projection();
    }

    void projection() {
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = mediaProjectionManager.createScreenCaptureIntent();
        mMediaProjectionResult.launch(intent);
    }
}