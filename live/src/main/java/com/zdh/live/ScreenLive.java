package com.zdh.live;

import android.app.Activity;
import android.view.Surface;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * author: ZDH
 * Date: 2022/3/29
 * Description:
 */
public class ScreenLive implements Runnable{
    static {
        System.loadLibrary("live");
    }

    private LinkedBlockingDeque<RTMPPackage> mQueue = new LinkedBlockingDeque<>(1000);

    String url;
    Activity mActivity;
    boolean isLiving = false;
    boolean isConnected = false;

    public ScreenLive(String url) {
        this.url = url;
    }

    public void start() {
        isLiving = true;
        new Thread(this).start();
    }

    public void stop() {
        isLiving = false;
    }

    @Override
    public void run() {
        isConnected = connect(url) != 0;
        if (!isConnected) {
            return;
        }
        while (isLiving) {
            try {
                RTMPPackage rtmpPackage = mQueue.take();
                send(rtmpPackage.data, rtmpPackage.len, rtmpPackage.tms,rtmpPackage.type);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendData(RTMPPackage rtmpPackage){
        mQueue.push(rtmpPackage);
    }

    private native int connect(String _url);

    private native void send(byte[] _data,int _len,long _tms,int type);
}
