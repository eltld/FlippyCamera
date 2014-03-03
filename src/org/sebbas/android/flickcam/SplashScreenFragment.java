package org.sebbas.android.flickcam;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SplashScreenFragment extends Fragment {

    public static final String TAG = "splash_screen_fragment";

    public static SplashScreenFragment newInstance() {
        SplashScreenFragment ssf = new SplashScreenFragment();
        return ssf;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.splash_screen, container, false);
        return view;
    }
}