package org.sebbas.android.views;

import org.sebbas.android.flickcam.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.OrientationEventListener;
import android.widget.ImageButton;

public class OrientationImageButton extends ImageButton {

    private int mAnimationSpeed;
    private OrientationEventListener mOrientationEventListener;
    
    
    public OrientationImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.OrientationImageButton, 0, 0);
        
        try {
            mAnimationSpeed = a.getInteger(R.styleable.OrientationImageButton_animationSpeed, 10);
            
        } finally {
            a.recycle();
        }
        
        mOrientationEventListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {

            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation < 45 && orientation >= 315) {
                    
                } else if (orientation >= 45 && orientation < 135) {
                    
                } else if (orientation >= 135 && orientation < 225) {
                    
                } else {
                    
                }
            }
            
        };
    }
    
    public void enableOrientationListener() {
        if (mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        }
    }
    
    public void disableOrientationListener() {
        mOrientationEventListener.disable();
    }


}
