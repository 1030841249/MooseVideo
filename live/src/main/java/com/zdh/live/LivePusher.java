package com.zdh.live;

/**
 * author: ZDH
 * Date: 2022/4/7
 * Description:
 */
public class LivePusher {
    AudioChannel audioChannel;
    VideoChannel videoChannel;

    public LivePusher(AudioChannel audioChannel,VideoChannel videoChannel) {
        this.audioChannel = audioChannel;
        this.videoChannel = videoChannel;
    }
}
