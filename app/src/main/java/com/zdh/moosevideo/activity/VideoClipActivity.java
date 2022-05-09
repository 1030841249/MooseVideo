package com.zdh.moosevideo.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import com.jaygoo.widget.OnRangeChangedListener;
import com.jaygoo.widget.RangeSeekBar;
import com.zdh.moosevideo.databinding.ActivityVideoClipBinding;
import com.zdh.musicsupport.ClipHelper;

import java.io.File;

public class VideoClipActivity extends AppCompatActivity {

    ActivityVideoClipBinding mBinding;
    int videoVolume,audioVolume;
    ClipHelper clipHelper;
    int duration;
    Runnable runnable;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityVideoClipBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        clipHelper = new ClipHelper();
        mBinding.sbVideoOri.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                videoVolume = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mBinding.sbAudioOri.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioVolume = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mBinding.btnCommit.setOnClickListener(v -> {
            makeVideo();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "input1.mp4");

        startPlay(file.getAbsolutePath());
    }

    private void startPlay(String path) {
        ViewGroup.LayoutParams layoutParams = mBinding.videoView.getLayoutParams();
        layoutParams.height = 675;
        layoutParams.width = 1285;
        mBinding.videoView.setLayoutParams(layoutParams);
        mBinding.videoView.setVideoPath(path);
        mBinding.videoView.start();
        mBinding.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                duration = mp.getDuration() / 1000;
                mp.setLooping(true);
                mBinding.videoRangerBar.setRange(0, duration);
                mBinding.videoRangerBar.setProgress(0, duration);
                mBinding.videoRangerBar.setEnabled(true);
                mBinding.videoRangerBar.requestLayout();
                mBinding.videoRangerBar.setOnRangeChangedListener(new OnRangeChangedListener() {
                    @Override
                    public void onRangeChanged(RangeSeekBar view, float min, float max, boolean isFromUser) {
                        mBinding.videoView.seekTo((int) min * 1000);
                    }

                    @Override
                    public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {

                    }

                    @Override
                    public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {

                    }
                });
                final Handler handler = new Handler();
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (mBinding.videoView.getCurrentPosition() >= mBinding.videoRangerBar.getRightSeekBar().getProgress() * 1000) {
                            mBinding.videoView.seekTo((int) mBinding.videoRangerBar.getLeftSeekBar().getProgress() * 1000);
                        }
                        handler.postDelayed(runnable, 1000);
                    }
                };
                handler.postDelayed(runnable, 1000);
            }
        });
    }

    public void makeVideo() {
        File cacheDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File video = new File(cacheDir, "input1.mp4");
        File audio = new File(cacheDir, "music.mp3");
        File outputFile = new File(cacheDir, "output.mp4");
        new Thread(() -> {
            clipHelper.mixVideoAndAudio(video.getAbsolutePath(),audio.getAbsolutePath(),outputFile.getAbsolutePath(),
                    (int) (mBinding.videoRangerBar.getLeftSeekBar().getProgress() * 1000* 1000),
                    (int) (mBinding.videoRangerBar.getRightSeekBar().getProgress() * 1000* 1000),
                    videoVolume,
                    audioVolume);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startPlay(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "output.mp4").getAbsolutePath());
                    Toast.makeText(VideoClipActivity.this, "剪辑完毕", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}