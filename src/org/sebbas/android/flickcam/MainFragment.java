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
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.WindowManager;

public class MainFragment extends ActionBarActivity implements CameraFragmentListener {

    private static final String TAG = "main_fragment";
    
    private MainPagerAdapter mPagerAdapter;
    private SplashScreenFragment mSplashScreenFragment;
    private FragmentManager mFragmentManager;
    private int mPosition;
    private ActionBar mActionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ON CREATE");
                
        mFragmentManager = getSupportFragmentManager();
        mActionBar = getSupportActionBar();
        mActionBar.hide(); // Immediately hide ActionBar for startup
        
        CameraFragmentUI cameraFragment = CameraFragmentUI.newInstance();
        GalleryFragment galleryFragment = GalleryFragment.newInstance();
        
        
        ArrayList<Fragment> fragmentList = new ArrayList<Fragment>();
        fragmentList.add(cameraFragment);
        fragmentList.add(galleryFragment);
        
        // Optional splash screen that stays as long as the camera initializes
        // SplashScreenFragment splashScreenFragment = SplashScreenFragment.newInstance();
        // mFragmentManager.beginTransaction().add(android.R.id.content, splashScreenFragment, SplashScreenFragment.TAG).commit();
        
        setContentView(R.layout.viewpager_layout);
        
        mPagerAdapter = new MainPagerAdapter(mFragmentManager, fragmentList);
        ViewPager pager = (ViewPager)super.findViewById(R.id.viewpager);
        
        // This fixes the overlapping fragments inside the viewpager
        pager.setPageMargin(getPageMargin());
        pager.setOnPageChangeListener(new OnPageChangeListener() {
        
            @Override
            public void onPageScrollStateChanged(int state) {
                switch(state) {
                case ViewPager.SCROLL_STATE_IDLE:
                    if(mPosition == 0) {
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                        mActionBar.hide();
                    } else {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                        mActionBar.show();
                    }
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                mPosition = position;
            }
            
        });
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
}
