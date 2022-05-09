package com.zdh.moosevideo.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import com.zdh.moosevideo.databinding.ActivityMergeVideoBinding;
import com.zdh.musicsupport.ClipHelper;
import com.zdh.musicsupport.MergeHelper;

import java.io.File;
import java.io.IOException;

public class MergeVideoActivity extends AppCompatActivity {
    MergeHelper mergeHelper;
    ActivityMergeVideoBinding mBinding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMergeVideoBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mergeHelper = new MergeHelper();
        mBinding.btnMerge.setOnClickListener(v -> {
            File cacheDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File video = new File(cacheDir, "input1.mp4");
            File appendVideo = new File(cacheDir, "input2.mp4");
            File outputFile = new File(cacheDir, "output.mp4");
            new Thread(() -> {
                try {
                    mergeHelper.appendVideo(appendVideo.getAbsolutePath(), video.getAbsolutePath(), outputFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MergeVideoActivity.this, "合并完成", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });
    }
}