package org.sebbas.android.listener;

import org.sebbas.android.flickcam.CameraThread;
import org.sebbas.android.views.CameraPreviewAdvanced;
import org.sebbas.android.views.CameraPreview;

import android.hardware.Camera.Parameters;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

public class PreviewGestureListener implements OnGestureListener {
    
    private static final int FLING_EFFECT_SENSITIVITY_Y = 4000;
    private static final int FLING_EFFECT_SENSITIVITY_X = 500;
    
    private CameraThread mCameraThread;
    private CameraPreview mCameraPreview;
    private CameraPreviewAdvanced mCameraPreviewAdvanced;
    private int mCurrentEffectNumber = 0;
    
    public PreviewGestureListener(CameraPreview cameraPreview, CameraThread cameraThread) {
       mCameraPreview = cameraPreview;
       mCameraThread = cameraThread;
    }
        
    public PreviewGestureListener(CameraPreviewAdvanced cameraPreviewAdvanced, CameraThread cameraThread) {
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
        System.out.println("velocity is " + velocityY);
        if (mCameraThread.getZoomValue() == 0 && velocityX < FLING_EFFECT_SENSITIVITY_X) {
            if (velocityY < FLING_EFFECT_SENSITIVITY_Y) {
                mCurrentEffectNumber = (mCurrentEffectNumber+1) % mCameraThread.getNumberOfColorEffects();
            } else if (velocityY > FLING_EFFECT_SENSITIVITY_Y) {
                if (mCurrentEffectNumber == 0) {
                    mCurrentEffectNumber = mCameraThread.getNumberOfColorEffects()-1;
                } else {
                    mCurrentEffectNumber--;
                }
            }
            mCameraThread.setCameraEffect(mCurrentEffectNumber);
        }
        return true;
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
