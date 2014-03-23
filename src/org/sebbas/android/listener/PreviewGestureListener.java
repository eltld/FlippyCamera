package org.sebbas.android.listener;

import org.sebbas.android.flickcam.CameraThread;
import org.sebbas.android.views.CameraPreviewAdvancedNew;
import org.sebbas.android.views.CameraPreviewNew;

import android.hardware.Camera.Parameters;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

public class PreviewGestureListener implements OnGestureListener {
    
    private CameraThread mCameraThread;
    private CameraPreviewNew mCameraPreview;
    private CameraPreviewAdvancedNew mCameraPreviewAdvanced;
    private int mCurrentEffectNumber = 0;
    
    public PreviewGestureListener(CameraPreviewNew cameraPreview, CameraThread cameraThread) {
       mCameraPreview = cameraPreview;
       mCameraThread = cameraThread;
    }
        
    public PreviewGestureListener(CameraPreviewAdvancedNew cameraPreviewAdvanced, CameraThread cameraThread) {
        mCameraPreviewAdvanced = cameraPreviewAdvanced;
        mCameraThread = cameraThread;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
            float velocityY) {
        if (velocityY < 0) {
            mCurrentEffectNumber = (mCurrentEffectNumber+1) % mCameraThread.NUMBER_OF_COLOR_EFFECTS;
        } else if (velocityY > 0) {
            if (mCurrentEffectNumber == 0) {
            	mCurrentEffectNumber = mCameraThread.NUMBER_OF_COLOR_EFFECTS-1;
            } else {
            	mCurrentEffectNumber--;
            }
        }
        mCameraThread.setCameraEffect(mCurrentEffectNumber);
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
            float distanceY) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        // TODO Auto-generated method stub
        return false;
    }

}
