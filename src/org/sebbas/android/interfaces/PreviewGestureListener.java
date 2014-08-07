package org.sebbas.android.interfaces;

import org.sebbas.android.threads.CameraThread;

import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

public class PreviewGestureListener implements OnGestureListener {
    
    private static final int FLING_EFFECT_SENSITIVITY_Y_POS = 3000;
    private static final int FLING_EFFECT_SENSITIVITY_Y_NEG = -3000;
    
    private CameraThread mCameraThread;
    private int mCurrentEffectNumber = 0;
    
    public PreviewGestureListener(CameraThread cameraThread) {
        mCameraThread = cameraThread;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
            float velocityY) {
        System.out.println("velocity is " + velocityY);
        if (mCameraThread.getZoomValue() == 0) { // Switching effect on zoom not supported on some devices
            if (velocityY < FLING_EFFECT_SENSITIVITY_Y_NEG) {
                mCurrentEffectNumber = (mCurrentEffectNumber+1) % mCameraThread.getNumberOfColorEffects();
            } else if (velocityY > FLING_EFFECT_SENSITIVITY_Y_POS) {
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
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }
}
