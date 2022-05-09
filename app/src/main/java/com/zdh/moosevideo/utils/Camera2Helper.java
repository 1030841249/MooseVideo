package com.zdh.moosevideo.utils;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * author: ZDH
 * Date: 2022/3/29
 * Description: 
 */
public class Camera2Helper implements TextureView.SurfaceTextureListener{
    // 操作摄像头是耗时操作，需要开启线程
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private CameraManager mCameraManager;

    private Activity mActivity;
    private TextureView mTextureView;


    private Size mPreviewSize;
    private Point previewViewSize;
    private ImageReader mImageReader;

    private String mCameraId = "0";
    private int mCameraSensorOrientation = 0;        //摄像头方向
    private int mCameraFacing = CameraCharacteristics.LENS_FACING_BACK;        //默认使用后置摄像头
    private int mDisplayRotation;  //手机方向

    private CameraCaptureSession mCameraCaptureSession;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mRequestBuilder;
    private CameraCharacteristics mCameraCharacteristics;

    private onCameraPreviewListener onCameraPreviewListener;

    public Camera2Helper(Activity activity, TextureView textureView, onCameraPreviewListener listener) {
        mActivity = activity;
        mTextureView = textureView;
        onCameraPreviewListener = listener;
        mHandlerThread = new HandlerThread("camera2_handlerThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mDisplayRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        mTextureView.setSurfaceTextureListener(this);
        checkPermission();
    }

    private void initCameraInfo() throws CameraAccessException {
        mCameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        for (String s : mCameraManager.getCameraIdList()) {
            Log.i("CameraInfo", "initCameraInfo: " + s);
        }
        // 获取摄像头特性
        mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
        // 获取支持的尺寸
        StreamConfigurationMap streamConfigurationMap =
                mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        // 寻找最佳尺寸，这里的尺寸是被设定的一个列表，只包含有一部分标准尺寸，对于当前手机需要进行最佳匹配
        mPreviewSize = getBestSupportedSize(
                Arrays.asList(streamConfigurationMap.getOutputSizes(SurfaceTexture.class)));

        if(onCameraPreviewListener != null) {
            onCameraPreviewListener.onSizeChanged(mPreviewSize);
        }

        // 图片转换
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                ImageFormat.YUV_420_888, 2);
        // 设置监听
        mImageReader.setOnImageAvailableListener(new OnImageAvaliableImpl(),mHandler);
        if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mCameraManager.openCamera(mCameraId,mStateCallback,mHandler);

    }

    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mActivity.requestPermissions(new String[]{Manifest.permission.CAMERA},1);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        try {
            initCameraInfo();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    byte[] nv21;
    byte[] y,u,v;

    private class OnImageAvaliableImpl implements ImageReader.OnImageAvailableListener {

        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            Image.Plane[] planes = image.getPlanes();
            if (y == null) {
                y = new byte[planes[0].getBuffer().limit() - planes[0].getBuffer().position()];
                u = new byte[planes[1].getBuffer().limit() - planes[1].getBuffer().position()];
                v = new byte[planes[2].getBuffer().limit() - planes[2].getBuffer().position()];

            }
            if (image.getPlanes()[0].getBuffer().remaining() == y.length) {
//                分别填到 yuv
                planes[0].getBuffer().get(y);
                planes[1].getBuffer().get(u);
                planes[2].getBuffer().get(v);
                // y+u = nv12
                // y+v = nv21
                if (onCameraPreviewListener != null) {
                    if (nv21 == null) {
                        nv21 = new byte[y.length + v.length];
                    }
                    // 使用的是nv21=y+vu
                    System.arraycopy(nv21, 0, y, 0,y.length);
                    System.arraycopy(nv21, y.length, v, 0, v.length);
                    onCameraPreviewListener.onReceiveData(rotate90(nv21));
                }
            }
            image.close();
        }
    }

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            try {
                createCameraSession();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };

    private void createCameraSession() throws CameraAccessException {
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        // 设置预览宽高
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
        // 创建画布
        Surface surface = new Surface(surfaceTexture);
        // 发起预览请求
        mRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        // 设置需要渲染数据到哪个画布
        mRequestBuilder.addTarget(surface);
        mRequestBuilder.addTarget(mImageReader.getSurface());
        mCameraDevice.createCaptureSession(Arrays.asList(surface,mImageReader.getSurface()),mCaptureStateCallback,mHandler);
    }

    private CameraCaptureSession.StateCallback mCaptureStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mCameraCaptureSession = session;
            try {
                mCameraCaptureSession.setRepeatingRequest(mRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                }, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    };

    private Size getBestSupportedSize(List<Size> sizes) {
        Point maxPreviewSize = new Point(1920, 1080);
        Point minPreviewSize = new Point(1280, 720);
        Size defaultSize = sizes.get(0);
        Size[] tempSizes = sizes.toArray(new Size[0]);
        Arrays.sort(tempSizes, new Comparator<Size>() {
            @Override
            public int compare(Size o1, Size o2) {
                if (o1.getWidth() > o2.getWidth()) {
                    return -1;
                } else if (o1.getWidth() == o2.getWidth()) {
                    return o1.getHeight() > o2.getHeight() ? -1 : 1;
                } else {
                    return 1;
                }
            }
        });
        sizes = new ArrayList<>(Arrays.asList(tempSizes));
        for (int i = sizes.size() - 1; i >= 0; i--) {
            if (maxPreviewSize != null) {
                if (sizes.get(i).getWidth() > maxPreviewSize.x || sizes.get(i).getHeight() > maxPreviewSize.y) {
                    sizes.remove(i);
                    continue;
                }
            }
            if (minPreviewSize != null) {
                if (sizes.get(i).getWidth() < minPreviewSize.x || sizes.get(i).getHeight() < minPreviewSize.y) {
                    sizes.remove(i);
                }
            }
        }
        if (sizes.size() == 0) {
            return defaultSize;
        }
        Size bestSize = sizes.get(0);
        float previewViewRatio;
        if (previewViewSize != null) {
            previewViewRatio = (float) previewViewSize.x / (float) previewViewSize.y;
        } else {
            previewViewRatio = (float) bestSize.getWidth() / (float) bestSize.getHeight();
        }

        if (previewViewRatio > 1) {
            previewViewRatio = 1 / previewViewRatio;
        }

        for (Size s : sizes) {
            if (Math.abs((s.getHeight() / (float) s.getWidth()) - previewViewRatio) < Math.abs(bestSize.getHeight() / (float) bestSize.getWidth() - previewViewRatio)) {
                bestSize = s;
            }
        }
        return bestSize;
    }

    private byte[] rotate90(byte[] data) {
        byte[] ret = new byte[data.length];
        int index = 0;
        for (int i = 0; i < mPreviewSize.getWidth(); i++) {
            for (int j = mPreviewSize.getHeight() - 1; j >= 0; j--) {
                ret[index++] = data[j * mPreviewSize.getWidth() + i];
            }
        }
        int ySize = mPreviewSize.getHeight() * mPreviewSize.getWidth();
        int uvHeight = mPreviewSize.getHeight() / 2;
        for (int i = 0; i < mPreviewSize.getWidth(); i += 2) {
            for (int j = uvHeight - 1; j >= 0; j--) {
                ret[i++] = data[ySize + j * mPreviewSize.getWidth() + i + 1];
                ret[i++] = data[ySize + j * mPreviewSize.getWidth() + i];
            }
        }
        return ret;
    }

    public interface onCameraPreviewListener {

        void onSizeChanged(Size size);
        void onReceiveData(byte[] yuv);
    }
}
