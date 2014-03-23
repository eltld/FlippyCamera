package org.sebbas.android.listener;

import org.sebbas.android.flickcam.CameraThread;
import org.sebbas.android.views.CameraPreviewAdvancedNew;
import org.sebbas.android.views.CameraPreviewNew;

import android.annotation.SuppressLint;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;

public class ScaleListenerNew extends SimpleOnScaleGestureListener {

    private float mScaleFactor = 1.f;
    private CameraThread mCameraThread;
    private CameraPreviewNew mCameraPreview;
    private CameraPreviewAdvancedNew mCameraPreviewAdvanced;

    public ScaleListenerNew(CameraPreviewNew cameraPreview, CameraThread cameraThread) {
       mCameraPreview = cameraPreview;
       mCameraThread = cameraThread;
    }
    
    public ScaleListenerNew(CameraPreviewAdvancedNew cameraPreviewAdvanced, CameraThread cameraThread) {
        mCameraPreviewAdvanced = cameraPreviewAdvanced;
        mCameraThread = cameraThread;
    }
    
    @SuppressLint("NewApi")
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        mScaleFactor  *= detector.getScaleFactor();
        
        // Don't let the object get too small or too large.
        mScaleFactor = Math.max(1.0f, Math.min(mScaleFactor, 2.0f));
        
        // Tell the camera thread to update the camera zoom
        mCameraThread.performZoom(mScaleFactor);
        
        // Depending on which type of Camera preview we are using (depends on minimum SDK of device) we use this
        if (mCameraPreviewAdvanced != null) {
            mCameraPreviewAdvanced.invalidate();
        } else {
            mCameraPreview.invalidate();
        }
        
        return true;
    }
}
