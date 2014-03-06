package org.sebbas.android.views;

import org.sebbas.android.flickcam.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.OrientationEventListener;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;

public class OrientationImageButton extends ImageButton {

    private static final float PIVOT_X = (float) 0.5f;
    private static final float PIVOT_Y = (float) 0.5f;
    private static final int DEFAULT_ANIMATION_DURATION = 1000; // in milliseconds
    private static final int ROTATION_OFFSET = 90;
    
    private int mAnimationDuration;
    private OrientationEventListener mOrientationEventListener;
    private int mNewRotation;
    
    public OrientationImageButton(Context context) {
        super(context);
    }
    
    public OrientationImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
        /*mOrientationEventListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
            
            @Override
            public void onOrientationChanged(int currentOrientation) {
                
                mOldRotation = mNewRotation;
                
                if (currentOrientation < 45 || currentOrientation >= 315) {
                    mNewRotation = 0;
                } else if (currentOrientation >= 45 && currentOrientation < 135) {
                    mNewRotation = 90;
                } else if (currentOrientation >= 135 && currentOrientation < 225) {
                    mNewRotation = 180;
                } else if (currentOrientation >= 225 && currentOrientation < 315){
                    mNewRotation = 270;
                }
                //System.out.println("old rotation = " + mOldRotation + " / " + "new rotation = " + mNewRotation);
                if (mOldRotation == 0 && mNewRotation == 90 || mOldRotation == 90 && mNewRotation == 180 ||
                        mOldRotation == 180 && mNewRotation == 270 || mOldRotation == 270 && mNewRotation == 0) {
                    startAnimation(currentOrientation, mOldRotation, false);
                }
                if (mOldRotation == 0 && mNewRotation == 270 || mOldRotation == 270 && mNewRotation == 180 ||
                        mOldRotation == 180 && mNewRotation == 90 || mOldRotation == 90 && mNewRotation == 0) {
                    startAnimation(currentOrientation, mOldRotation, true);
                }
            }
        };*/
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        disableOrientationListener();
    }

    private void startAnimation(int oldRotation, boolean clockwise) {
        Animation animation;
        if (clockwise) {
            animation = new RotateAnimation(-oldRotation, -oldRotation + ROTATION_OFFSET, 
                    Animation.RELATIVE_TO_SELF, PIVOT_X, Animation.RELATIVE_TO_SELF, PIVOT_Y);
        } else {
            animation = new RotateAnimation(-oldRotation,  -oldRotation - ROTATION_OFFSET, 
                    Animation.RELATIVE_TO_SELF, PIVOT_X, Animation.RELATIVE_TO_SELF, PIVOT_Y);
        }
        animation.setDuration(mAnimationDuration);
        animation.setRepeatCount(0);
        animation.setFillAfter(true);
        
        this.startAnimation(animation);
        
    }
    
    public void enableOrientationListener() {
        if (mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        }
    }
    
    public void disableOrientationListener() {
        mOrientationEventListener.disable();
    }

    @SuppressLint("NewApi")
	@Override
	public void setImageDrawable(Drawable drawable) {
		super.setImageDrawable(drawable);
		this.setImageDrawable(drawable);
		this.setRotation(mNewRotation);
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
                    //System.out.println("old rotation = " + mOldRotation + " / " + "new rotation = " + mNewRotation);
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
