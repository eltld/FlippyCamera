package org.sebbas.android.views;

import org.sebbas.android.flickcam.CameraThread;
import org.sebbas.android.helper.DeviceInfo;
import org.sebbas.android.listener.PreviewGestureListener;
import org.sebbas.android.listener.ScaleListener;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;

@SuppressLint("NewApi")
public class CameraPreviewAdvanced extends TextureView implements
        SurfaceTextureListener {

    private static final String TAG = "camera_preview_advanced";
    private CameraThread mCameraThread;
    private ScaleGestureDetector mScaleDetector;
    private Context mContext;
    private int mScreenWidth;
    private int mScreenHeight;
    private GestureDetectorCompat mGestureDetector;

    public CameraPreviewAdvanced(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }
    
    public CameraPreviewAdvanced(Context context, CameraThread cameraThread) {
        super(context);
        mContext = context;
        mScreenWidth = DeviceInfo.getRealScreenWidth(context);
        mScreenHeight = DeviceInfo.getRealScreenHeight(context);
        mCameraThread = cameraThread;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener(this, cameraThread));
        mGestureDetector = new GestureDetectorCompat(context, new PreviewGestureListener(this, cameraThread));
        setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "ON SURFACE TEXTURE AVAILABLE");
        if (mCameraThread.isAlive()) {
            mCameraThread.setPreviewTexture(surface);
            mCameraThread.startCameraPreview();
            mCameraThread.setCameraPreviewSize(mScreenWidth, mScreenHeight);
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
        mGestureDetector.onTouchEvent(event);
        
        // This is disabled temporarly because not working properly
        /*if (event.getAction() == MotionEvent.ACTION_DOWN) {
        	float x = event.getX();
        	float y = event.getY();
        	float touchMajor = event.getTouchMajor();
        	float touchMinor = event.getTouchMinor();
        	
        	Rect touchRect = new Rect(
        		(int)(x - touchMajor/2),
        		(int)(y - touchMinor/2),
        		(int)(x + touchMajor/2),
                (int)(y + touchMinor/2));
            if (mCameraThread.isAlive()) {
            	mCameraThread.touchFocus(touchRect);
            }
        }*/
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.d(TAG, "ON MEASURE");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        setMeasuredDimension(mScreenWidth, mScreenHeight);
        
    }
}
