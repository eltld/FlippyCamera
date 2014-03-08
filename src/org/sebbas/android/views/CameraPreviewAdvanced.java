package org.sebbas.android.views;
import java.io.IOException;

import org.sebbas.android.interfaces.CameraPreviewListener;
import org.sebbas.android.listener.ScaleListener;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;

@SuppressLint("NewApi")
public class CameraPreviewAdvanced extends TextureView implements
        SurfaceTextureListener {

    private static final String TAG = "camera_preview_advanced";
    private CameraPreviewListener mListener;
    private Camera mCamera;
    private ScaleGestureDetector mScaleDetector;

    public CameraPreviewAdvanced(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public CameraPreviewAdvanced(Context context, CameraPreviewListener listener, Camera camera) {
        super(context);
        mCamera = camera;
        mListener = listener;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener(listener, this));
        setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "ON SURFACE TEXTURE AVAILABLE");
        
        try {
            if (mCamera != null) {
                mCamera.setPreviewTexture(surface);
                mListener.startRecorder();
            }
        } catch (IOException e) {
            // Something bad happened
            e.printStackTrace();
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "ON SURFACE TEXTURE DESTROYED");
        return true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "ON SURFACE TEXTURE SIZE CHANGED");
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(event);
        return true;
    }
}
