package com.zdh.moosevideo.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.zdh.mediacodec.h265.SocketPlayLive;
import com.zdh.moosevideo.databinding.ActivityPlayBinding;

public class PlayActivity extends AppCompatActivity {

    ActivityPlayBinding mBinding;
    SocketPlayLive socketPlayLive;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityPlayBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        socketPlayLive = new SocketPlayLive(mBinding.surfaceView);
    }
}