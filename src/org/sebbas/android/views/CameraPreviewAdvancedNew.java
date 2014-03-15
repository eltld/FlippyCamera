package org.sebbas.android.views;

import org.sebbas.android.flickcam.CameraThread;
import org.sebbas.android.helper.DeviceInfo;
import org.sebbas.android.listener.ScaleListenerNew;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;

@SuppressLint("NewApi")
public class CameraPreviewAdvancedNew extends TextureView implements
        SurfaceTextureListener {

    private static final String TAG = "camera_preview_advanced";
    private CameraThread mCameraThread;
    private ScaleGestureDetector mScaleDetector;
    private Context mContext;
    private int mScreenWidth;
    private int mScreenHeight;

    public CameraPreviewAdvancedNew(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }
    
    public CameraPreviewAdvancedNew(Context context, CameraThread cameraThread) {
        super(context);
        mContext = context;
        mScreenWidth = DeviceInfo.getRealScreenWidth(context);
        mScreenHeight = DeviceInfo.getRealScreenHeight(context);
        mCameraThread = cameraThread;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListenerNew(this, cameraThread));
        setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "ON SURFACE TEXTURE AVAILABLE");
        if (mCameraThread.isAlive()) {
            mCameraThread.setPreviewTexture(surface);
            mCameraThread.startCameraPreview();
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.d(TAG, "ON MEASURE");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        setMeasuredDimension(mScreenWidth, mScreenHeight);
        if (mCameraThread.isAlive()) {
            mCameraThread.setCameraPreviewSize(mScreenWidth, mScreenHeight);
        }
    }
}
