package org.sebbas.android.flickcam;

import java.util.ArrayList;

import org.sebbas.android.adapter.MainPagerAdapter;
import org.sebbas.android.interfaces.CameraFragmentListener;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ON CREATE");
        
        initialiseStartup();
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
    
    private void initialiseStartup() {
        mFragmentManager = getSupportFragmentManager();
        
        CameraFragment cameraFragment = CameraFragment.newInstance();
        GalleryFragment galleryFragment = GalleryFragment.newInstance();
        SplashScreenFragment splashScreenFragment = SplashScreenFragment.newInstance();
        
        ArrayList<Fragment> fragmentList = new ArrayList<Fragment>();
        fragmentList.add(cameraFragment);
        fragmentList.add(galleryFragment);
        
        mFragmentManager.beginTransaction().add(android.R.id.content, splashScreenFragment, SplashScreenFragment.TAG).commit();
        setContentView(R.layout.viewpager_layout);
        
        initialisePaging(fragmentList);
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
    public void updateAdapter() {
        mPagerAdapter.notifyDataSetChanged();
    }
}
