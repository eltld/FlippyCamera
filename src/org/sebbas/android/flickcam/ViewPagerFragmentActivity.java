package org.sebbas.android.flickcam;

import java.util.List;
import java.util.Vector;

import org.sebbas.android.flickcam.CameraLoaderFragment.ProgressListener;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.TextView;

public class ViewPagerFragmentActivity extends FragmentActivity implements ProgressListener {

    private static final String TAG_SPLASH_SCREEN = "splash_screen";
    private static final String TAG_CAMERA_LOADER = "camera_loader";
    private static final String TAG = "view_pager_fragment_activity";

    private PagerAdapter mPagerAdapter;
    private SplashScreenFragment mSplashScreenFragment;
    private CameraLoaderFragment mCameraLoaderFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ON CREATE");
        initialiseStartup();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "ON PAUSE");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "ON RESUME");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "ON START");
        if (mCameraLoaderFragment != null) {
            checkCompletionStatus();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "ON STOP");
        if (mCameraLoaderFragment != null) {
            mCameraLoaderFragment.removeProgressListener();
        }
    }

    @Override
    public void onCompletion(boolean setupSuccesful) {
        Log.d(TAG, "ON COMPLETION");
        /*if (setupSuccesful) {
            setContentView(R.layout.viewpager_layout);
            initialisePaging();
        }*/
        TextView tv = new TextView(this);
        tv.setText("hello");
        setContentView(tv);
        mCameraLoaderFragment = null; 
    }

    @Override
    public void onProgressUpdate(int value) {
        // Not yet implemented
    }

    private void initialisePaging() {
        List<Fragment> fragments = new Vector<Fragment>();
        fragments.add(Fragment.instantiate(this, CameraFragment.class.getName()));
        fragments.add(Fragment.instantiate(this, GalleryFragment.class.getName()));
        
        mPagerAdapter = new PagerAdapter(super.getSupportFragmentManager(), fragments);
        ViewPager pager = (ViewPager)super.findViewById(R.id.viewpager);
        pager.setAdapter(mPagerAdapter);
    }
    
    private void initialiseStartup() {
        final FragmentManager fm = getSupportFragmentManager();
        
        mCameraLoaderFragment = (CameraLoaderFragment) fm.findFragmentByTag(TAG_CAMERA_LOADER);
        if (mCameraLoaderFragment == null) {
            mCameraLoaderFragment = new CameraLoaderFragment();
            mCameraLoaderFragment.setProgressListener(this);
            mCameraLoaderFragment.startLoading();
            fm.beginTransaction().add(mCameraLoaderFragment, TAG_CAMERA_LOADER).commit();
        } else {
            if (checkCompletionStatus()) {
                return;
            }
        }
        
        mSplashScreenFragment = (SplashScreenFragment)fm.findFragmentByTag(TAG_SPLASH_SCREEN);
        if (mSplashScreenFragment == null) {
            mSplashScreenFragment = new SplashScreenFragment();
            fm.beginTransaction().add(android.R.id.content, mSplashScreenFragment, TAG_SPLASH_SCREEN).commit();
        }
    }
    
    private boolean checkCompletionStatus() {
        if (mCameraLoaderFragment.hasLoaded()) {
            onCompletion(mCameraLoaderFragment.getSetupStatus());
            FragmentManager fm = getSupportFragmentManager();
            mSplashScreenFragment = (SplashScreenFragment) fm.findFragmentByTag(TAG_SPLASH_SCREEN);
            if (mSplashScreenFragment != null) {
                fm.beginTransaction().remove(mSplashScreenFragment).commit();
            }
            
            return true;
        }
        mCameraLoaderFragment.setProgressListener(this);
        return false;
    }
}
