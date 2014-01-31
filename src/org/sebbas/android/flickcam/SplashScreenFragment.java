package org.sebbas.android.flickcam;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

public class SplashScreenFragment extends Fragment {

	private ImageView mSplashIcon;
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.splash_screen, container, false);
        mSplashIcon = (ImageView) view.findViewById (R.id.splash_icon);
        //mSplashIcon.startAnimation(getBlinkAnimation());
        return view;
    }
    
    private AlphaAnimation getBlinkAnimation() {
        while(true) {
            AlphaAnimation blinkAnimation= new AlphaAnimation((float)1.0, (float) 0.2); // Change alpha from fully visible to invisible
            blinkAnimation.setDuration(1000);
            blinkAnimation.setInterpolator(new LinearInterpolator());
            blinkAnimation.setRepeatCount(100); 
            blinkAnimation.setRepeatMode(Animation.REVERSE);
            return blinkAnimation;
    	}
    }
}