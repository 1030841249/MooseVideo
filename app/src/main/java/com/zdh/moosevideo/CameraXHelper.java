package com.zdh.moosevideo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.Image;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.os.ExecutorCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

/**
 * author: ZDH
 * Date: 2022/4/6
 * Description:
 */
public class CameraXHelper implements ImageAnalysis.Analyzer {
    LifecycleOwner lifecycleOwner;
    PreviewView previewView;
    HandlerThread mHandlerThread;
    Handler mBackground;
    ProcessCameraProvider mCameraProvider;
    ListenableFuture<ProcessCameraProvider> mListenableFuture;
    Camera mCamera;
    int width = 1080;
    int height = 1920;
    byte[] y,u,v;
    byte[] nv21;
    byte[] nv21_rotated;
    OnCameraXHelperListener mListener;

    public CameraXHelper(Context context, LifecycleOwner lifecycleOwner, PreviewView previewView) {
        this.lifecycleOwner = lifecycleOwner;
        this.previewView = previewView;
        mHandlerThread = new HandlerThread("cameraX_thread");
        mHandlerThread.start();
        mBackground = new Handler(mHandlerThread.getLooper());
        mListenableFuture = ProcessCameraProvider.getInstance(context);
        mListenableFuture.addListener((Runnable) () -> {
            try {
                mCameraProvider = mListenableFuture.get();
                bindPreView();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(context));

    }

    public void setListener(OnCameraXHelperListener mListener) {
        this.mListener = mListener;
    }

    private void bindPreView() {
        mCamera = mCameraProvider.bindToLifecycle(lifecycleOwner, getSelector(), getPreview(),getAnalysis());
    }

    private CameraSelector getSelector() {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        return cameraSelector;
    }

    private Preview getPreview() {
        Preview preview = new Preview.Builder()
                .setTargetResolution(new Size(width, height))
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        return preview;
    }

    private ImageAnalysis getAnalysis() {
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(width, height))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(ExecutorCompat.create(mBackground),this);
        return imageAnalysis;
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        if (y == null) {
            y = new byte[planes[0].getBuffer().limit() - planes[0].getBuffer().position()];
            u = new byte[planes[1].getBuffer().limit() - planes[1].getBuffer().position()];
            v = new byte[planes[2].getBuffer().limit() - planes[2].getBuffer().position()];
            if (mListener != null) {
                mListener.onSizeChanged(image.getWidth(),image.getHeight());
            }
        }
        if (image.getPlanes()[0].getBuffer().remaining() == y.length) {
            planes[0].getBuffer().get(y);
            planes[1].getBuffer().get(u);
            planes[2].getBuffer().get(v);
            Size size = new Size(image.getWidth(), image.getHeight());
            if (nv21 == null) {
                nv21 = new byte[size.getHeight() * size.getWidth() * 3 / 2];
                nv21_rotated = new byte[size.getHeight() * size.getWidth() * 3 / 2];
            }
            ImageUtil.yuvToNv21(y,u,v,nv21,size.getWidth(),size.getHeight());
            ImageUtil.nv21_rotate_to_90(nv21, nv21_rotated, size.getWidth(), size.getHeight());
            if (mListener != null) {
                mListener.onAnalyze(nv21_rotated);
            }
        }
        image.close();
    }

    public interface OnCameraXHelperListener {
        void onAnalyze(byte[] yuv);
        void onSizeChanged(int width,int height);
    }
}
