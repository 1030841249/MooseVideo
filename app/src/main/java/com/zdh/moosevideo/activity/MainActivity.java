package com.zdh.moosevideo.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.zdh.mediacodec.MediaHelper;
import com.zdh.moosevideo.R;
import com.zdh.moosevideo.databinding.ActivityMainBinding;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    MediaHelper mediaHelper = new MediaHelper();
    ActivityMainBinding mBinding;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        mBinding.btnPush.setOnClickListener(v -> {
            startActivity(new Intent(this,PushActivity.class));
        });
        mBinding.btnPlay.setOnClickListener(v -> {
            startActivity(new Intent(this,PlayActivity.class));
        });
        mBinding.btnClip.setOnClickListener(v -> {
            startActivity(new Intent(this,AudioClipActivity.class));
        });
        mBinding.btnClipVideo.setOnClickListener(v -> {
            startActivity(new Intent(this,VideoClipActivity.class));
        });
        mBinding.btnMergeVideo.setOnClickListener(v -> {
            startActivity(new Intent(this,MergeVideoActivity.class));
        });
        mBinding.btnCamera.setOnClickListener(v -> {
            startActivity(new Intent(this,Camera2Activity.class));
        });
        mBinding.btnProjectionLive.setOnClickListener(v -> {
            startActivity(new Intent(this,ProjectionLiveActivity.class));
        });
        mBinding.btnX264.setOnClickListener(v -> {
            startActivity(new Intent(this,X264Activity.class));
        });
    }

    void renderH264() {
        SurfaceView surfaceView = findViewById(R.id.surface_view);
        mediaHelper.init(surfaceView);
    }

    public void playVideo(View view) {
        new Thread(mediaHelper).start();
    }
}