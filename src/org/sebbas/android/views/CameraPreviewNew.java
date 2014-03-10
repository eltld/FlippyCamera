package org.sebbas.android.views;

import org.sebbas.android.flickcam.CameraThread;
import org.sebbas.android.interfaces.CameraPreviewListener;
import org.sebbas.android.listener.ScaleListener;
import org.sebbas.android.listener.ScaleListenerNew;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreviewNew extends SurfaceView implements SurfaceHolder.Callback {
    
    private static final String TAG = "camera_preview";

    private SurfaceHolder mHolder;
    private CameraThread mCameraThread;
    
    private ScaleGestureDetector mScaleDetector;
    
    public CameraPreviewNew(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @SuppressWarnings("deprecation")
    public CameraPreviewNew(Context context, CameraThread cameraThread) {
        super(context);
        mCameraThread = cameraThread;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListenerNew(this, mCameraThread));
        
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, "Surface was Changed");
        mCameraThread.startRecorder(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "Surface was Created");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Surface was Destroyed");
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(event);

        return true;
    }
}