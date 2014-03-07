package org.sebbas.android.listener;

import org.sebbas.android.interfaces.CameraPreviewListener;
import org.sebbas.android.views.CameraPreview;
import org.sebbas.android.views.CameraPreviewAdvanced;

import android.annotation.SuppressLint;
import android.hardware.Camera;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;

public class ScaleListener extends SimpleOnScaleGestureListener {

    private float mScaleFactor = 1.f;
    private CameraPreviewListener mListener;
    private CameraPreview mCameraPreview;
    private CameraPreviewAdvanced mCameraPreviewAdvanced;

    public ScaleListener(CameraPreviewListener listener, CameraPreview cameraPreview) {
       mListener = listener;
       mCameraPreview = cameraPreview;
    }
    
    public ScaleListener(CameraPreviewListener listener, CameraPreviewAdvanced cameraPreviewAdvanced) {
        mListener = listener;
        mCameraPreviewAdvanced = cameraPreviewAdvanced;
     }
    
    @SuppressLint("NewApi")
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        mScaleFactor  *= detector.getScaleFactor();
        
        // Don't let the object get too small or too large.
        mScaleFactor = Math.max(1.0f, Math.min(mScaleFactor, 2.0f));
        
        mListener.performZoom(mScaleFactor);
        
        // Depending on which type of Camera preview we are using (depends on minimum SDK of device) we use this
        if (mCameraPreviewAdvanced != null) {
            mCameraPreviewAdvanced.invalidate();
        } else {
            mCameraPreview.invalidate();
        }
        
        return true;
    }
}
