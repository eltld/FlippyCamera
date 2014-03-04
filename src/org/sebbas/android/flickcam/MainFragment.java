package org.sebbas.android.flickcam;

import java.util.ArrayList;

import org.sebbas.android.adapter.MainPagerAdapter;
import org.sebbas.android.interfaces.CameraFragmentListener;
import org.sebbas.android.viewpager.DepthPageTransformer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;

public class MainFragment extends FragmentActivity implements CameraFragmentListener {

    private static final String TAG = "main_fragment";
    
    private MainPagerAdapter mPagerAdapter;
    private SplashScreenFragment mSplashScreenFragment;
    private FragmentManager mFragmentManager;


    private boolean mShowingFrontCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ON CREATE");
        
        mFragmentManager = getSupportFragmentManager();
        
        /*CameraFragment cameraFragment = CameraFragment.newInstance();
        GalleryFragment galleryFragment = GalleryFragment.newInstance();
        SplashScreenFragment splashScreenFragment = SplashScreenFragment.newInstance();
        
        ArrayList<Fragment> fragmentList = new ArrayList<Fragment>();
        fragmentList.add(cameraFragment);
        fragmentList.add(galleryFragment);*/
        System.out.println("Called mainfragment");
        /*if (savedInstanceState == null) {
            mFragmentManager
                    .beginTransaction()
                    .add(R.id.viewpager, CameraFragment.newInstance(0), CameraFragment.TAG)
                    .commit();
        }*/
        
        // Optional splash screen that stays as long as the camera initializes
        //mFragmentManager.beginTransaction().add(android.R.id.content, splashScreenFragment, SplashScreenFragment.TAG).commit();
        
        setContentView(R.layout.viewpager_layout);
        
        mPagerAdapter = new MainPagerAdapter(mFragmentManager);
        ViewPager pager = (ViewPager)super.findViewById(R.id.viewpager);
        
        // This fixes the overlapping fragments inside the viewpager
        pager.setPageMargin(getPageMargin());
        pager.setPageTransformer(true, new DepthPageTransformer());
        pager.setAdapter(mPagerAdapter);
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
    
    private int getPageMargin() {
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20*2, getResources().getDisplayMetrics());
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
    
    @Override
    public void refreshAdapter() {
        mPagerAdapter.notifyDataSetChanged();
    }

    @Override
    public void switchCameraFragment() {
        mPagerAdapter.switchCameraFragment();
    }
}
