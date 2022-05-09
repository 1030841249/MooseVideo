package com.zdh.moosevideo.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.zdh.moosevideo.databinding.ActivityCamera2Binding;
import com.zdh.moosevideo.utils.Camera2Helper;

public class Camera2Activity extends AppCompatActivity {

    ActivityCamera2Binding mBinding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityCamera2Binding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
//        Camera2Helper camera2Helper = new Camera2Helper(this, mBinding.textureView);
    }
}