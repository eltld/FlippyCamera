package org.sebbas.android.flickcam;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.sebbas.android.adapter.MainPagerAdapter;
import org.sebbas.android.interfaces.CameraFragmentListener;
import org.sebbas.android.interfaces.ProgressListener;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

public class MainFragment extends FragmentActivity implements ProgressListener, CameraFragmentListener {

    private static final String TAG_SPLASH_SCREEN = "splash_screen";
    private static final String TAG_CAMERA_LOADER = "camera_loader";
    private static final String TAG_CAMERA_FRAGMENT = "camera_fragment";
    private static final String TAG_GALLERY_FRAGMENT = "gallery_fragment";
    private static final String TAG = "main_fragment";
    
    private MainPagerAdapter mPagerAdapter;
    private CameraLoaderFragment mCameraLoaderFragment;
    private SplashScreenFragment mSplashScreenFragment;
    private FragmentManager mFragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ON CREATE");
        
        initialiseStartup2();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "ON RESUME");
        
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void initialisePaging(ArrayList<Fragment> fragmentList) {
        System.out.println("initialisePaging");
        mPagerAdapter = new MainPagerAdapter(super.getSupportFragmentManager(), fragmentList);
        ViewPager pager = (ViewPager)super.findViewById(R.id.viewpager);
        
        // This fixes the overlapping fragments inside the viewpager
        int margin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20*2, getResources().getDisplayMetrics());
        pager.setPageMargin(margin);
        
        pager.setAdapter(mPagerAdapter);
    }
    
    private void initialiseStartup() {
        Log.d(TAG, "initialise Startup");
        mFragmentManager = getSupportFragmentManager();
        mCameraLoaderFragment = new CameraLoaderFragment();
        mCameraLoaderFragment.setProgressListener(this);
        mCameraLoaderFragment.startLoading(this.getApplicationContext());
        mFragmentManager.beginTransaction().add(mCameraLoaderFragment, TAG_CAMERA_LOADER).commit();
        
        mSplashScreenFragment = new SplashScreenFragment();
        mFragmentManager.beginTransaction().add(android.R.id.content, mSplashScreenFragment, TAG_SPLASH_SCREEN).commit();
        
        /*mFragmentManager = getSupportFragmentManager();
        
        mCameraLoaderFragment = (CameraLoaderFragment) mFragmentManager.findFragmentByTag(TAG_CAMERA_LOADER);
        if (mCameraLoaderFragment == null) {
            mCameraLoaderFragment = new CameraLoaderFragment();
            mCameraLoaderFragment.setProgressListener(this);
            mCameraLoaderFragment.startLoading(this.getApplicationContext());
            mFragmentManager.beginTransaction().add(mCameraLoaderFragment, TAG_CAMERA_LOADER).commit();
        } else {
            if (checkCompletionStatus()) {
                return;
            }
        }
        
        mSplashScreenFragment = (SplashScreenFragment)mFragmentManager.findFragmentByTag(TAG_SPLASH_SCREEN);
        if (mSplashScreenFragment == null) {
            mSplashScreenFragment = new SplashScreenFragment();
            mFragmentManager.beginTransaction().add(android.R.id.content, mSplashScreenFragment, TAG_SPLASH_SCREEN).commit();
        }*/
    }
    
    private void initialiseStartup2() {
        mFragmentManager = getSupportFragmentManager();
        
        ArrayList<Fragment> fragmentList = new ArrayList<Fragment>();
        fragmentList.add(Fragment.instantiate(getApplicationContext(), CameraFragment.class.getName()));
        fragmentList.add(Fragment.instantiate(getApplicationContext(), GalleryFragment.class.getName()));
        
        mSplashScreenFragment = new SplashScreenFragment();
        //mFragmentManager.beginTransaction().add(android.R.id.content, mSplashScreenFragment, SplashScreenFragment.TAG).commit();
        setContentView(R.layout.viewpager_layout);
        
        initialisePaging(fragmentList);
    }
    

    @Override
    public void onCompletion(ArrayList<Fragment> fragmentList) {
        if (fragmentList != null) {
            Log.d(TAG, "onCompletion");
            setContentView(R.layout.viewpager_layout);
            initialisePaging(fragmentList);
        }
    }

    @Override
    public void onProgressUpdate(int value) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onCompletion() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void startupComplete() {
        Log.d(TAG, "startupComplete");
        FragmentManager fm = getSupportFragmentManager();
        mSplashScreenFragment = (SplashScreenFragment) fm.findFragmentByTag(SplashScreenFragment.TAG);
        if (mSplashScreenFragment != null) {
            fm.beginTransaction().remove(mSplashScreenFragment).commit();
        }
    }
}
