package org.sebbas.android.views;

import org.sebbas.android.flickcam.R;

import com.nineoldandroids.animation.ObjectAnimator;

import android.content.Context;
import android.content.res.TypedArray;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.OrientationEventListener;
import android.widget.ImageButton;

public class OrientationImageButton extends ImageButton {

    private static final int DEFAULT_ANIMATION_DURATION = 1000; // in milliseconds
    private static final int ROTATION_OFFSET = 90;
    
    private int mAnimationDuration;
    private OrientationEventListener mOrientationEventListener;
    private int mNewRotation;
    
    public OrientationImageButton(Context context) {
        super(context);
    }
    
    public OrientationImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.OrientationImageButton, 0, 0);
        try {
            
            mAnimationDuration = a.getInt(R.styleable.OrientationImageButton_animationDuration, DEFAULT_ANIMATION_DURATION);
            
        } finally {
            a.recycle();
        }
        OrientationHandler oi = new OrientationHandler();
        oi.execute(context);
    } 

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        disableOrientationListener();
    }

    private void startAnimation(int oldRotation, boolean clockwise) {
        if (clockwise) {
            ObjectAnimator.ofFloat(this, "rotation", -oldRotation, -oldRotation + ROTATION_OFFSET)
                .setDuration(mAnimationDuration)
                .start();
        } else {
            ObjectAnimator.ofFloat(this, "rotation", -oldRotation, -oldRotation - ROTATION_OFFSET)
                .setDuration(mAnimationDuration)
                .start();
        }
    }
    
    public void enableOrientationListener() {
        if (mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        }
    }
    
    public void disableOrientationListener() {
        mOrientationEventListener.disable();
    }

    private class OrientationHandler extends AsyncTask<Context, Void, Void> {

        @Override
        protected Void doInBackground(Context... params) {
            
            mOrientationEventListener = new OrientationEventListener(params[0], SensorManager.SENSOR_DELAY_NORMAL) {
                
                int oldRotation = 0;
                
                @Override
                public void onOrientationChanged(int currentOrientation) {
                    
                    oldRotation = mNewRotation;;
                    
                    if (currentOrientation < 45 || currentOrientation >= 315) {
                        mNewRotation = 0;
                    } else if (currentOrientation >= 45 && currentOrientation < 135) {
                        mNewRotation = 90;
                    } else if (currentOrientation >= 135 && currentOrientation < 225) {
                        mNewRotation = 180;
                    } else if (currentOrientation >= 225 && currentOrientation < 315){
                        mNewRotation = 270;
                    }
                    if (oldRotation == 0 && mNewRotation == 90 || oldRotation == 90 && mNewRotation == 180 ||
                            oldRotation == 180 && mNewRotation == 270 || oldRotation == 270 && mNewRotation == 0) {
                        startAnimation(oldRotation, false);
                    }
                    if (oldRotation == 0 && mNewRotation == 270 || oldRotation == 270 && mNewRotation == 180 ||
                            oldRotation == 180 && mNewRotation == 90 || oldRotation == 90 && mNewRotation == 0) {
                        startAnimation(oldRotation, true);
                    }
                }
            };
            enableOrientationListener();
            return null;
        }
    }
}
