package com.zdh.moosevideo.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Environment;

import com.zdh.moosevideo.databinding.ActivityAudioClipBinding;
import com.zdh.musicsupport.ClipHelper;

public class AudioClipActivity extends AppCompatActivity {

    ActivityAudioClipBinding mBinding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityAudioClipBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        ClipHelper audioClip = new ClipHelper();
        mBinding.btnClip.setOnClickListener(v -> {
            audioClip.clipWithIOThread(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/music.mp3",
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/music_clip.pcm",
                    5*1000*1000,15*1000*1000);
        });
        mBinding.btnMix.setOnClickListener(v -> {
            audioClip.mixWithThread(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/input1.mp4",
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/music.mp3",
                    5*1000*1000,15*1000*1000);
        });
    }
}