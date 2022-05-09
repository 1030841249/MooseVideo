package com.zdh.moosevideo;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * author: ZDH
 * Date: 2022/3/24
 * Description:
 */
class MooseSurfaceView extends SurfaceView implements SurfaceHolder.Callback,Camera.PreviewCallback {
    Camera.Size size;
    byte[] buffer;

    public MooseSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        open();
    }

    public void open() {
        Camera camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        Camera.Parameters parameters = camera.getParameters();
        size = parameters.getPreviewSize();
        try {
            camera.setPreviewDisplay(getHolder());
            camera.setDisplayOrientation(90);
            buffer = new byte[size.width * size.height * 3 / 2];
            camera.addCallbackBuffer(buffer);
            camera.setPreviewCallbackWithBuffer(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }

    private void rotate(byte[] data) {
        int yLen = size.width * size.height;
        int k = 0;
        for(int i = 0; i < size.width; i++) {
            for(int j = size.height - 1; j >= 0; j--) {
                buffer[k++] = data[size.width * j + i];
            }
        }
        int uvHeight = size.height/2;
        for(int i = 0; i < size.width; i+=2) {
            for (int j = uvHeight - 1; j >= 0; j--) {
                buffer[k++] = data[yLen + size.width * i + j];
                buffer[k++] = data[yLen + size.width * i + j + 1];
            }
        }
    }
}
