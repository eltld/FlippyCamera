package org.sebbas.android.flickcam;

import java.util.ArrayList;

import org.sebbas.android.adapter.MainPagerAdapter;
import org.sebbas.android.interfaces.CameraFragmentListener;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

public class MainFragment extends ActionBarActivity implements CameraFragmentListener {

    private static final String TAG = "main_fragment";
    private static final int CAMERA_FRAGMENT_NUMBER = 0;
    
    private MainPagerAdapter mPagerAdapter;
    private FragmentManager mFragmentManager;
    private int mPosition;
    private ActionBar mActionBar;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ON CREATE");
                
        mFragmentManager = getSupportFragmentManager();
        mActionBar = getSupportActionBar();
        mActionBar.hide(); // Immediately hide ActionBar for startup
        
        CameraFragmentUI cameraFragment = CameraFragmentUI.newInstance();
        final GalleryFragment galleryFragment = GalleryFragment.newInstance();
        
        ArrayList<Fragment> fragmentList = new ArrayList<Fragment>();
        fragmentList.add(cameraFragment);
        fragmentList.add(galleryFragment);
        
        setContentView(R.layout.viewpager_layout);
        
        mPagerAdapter = new MainPagerAdapter(mFragmentManager, fragmentList);
        mViewPager = (ViewPager)super.findViewById(R.id.viewpager);
        
        // This fixes the overlapping fragments inside the viewpager
        mViewPager.setPageMargin(getPageMargin());
        mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
        
            @Override
            public void onPageScrollStateChanged(int state) {
                switch(state) {
                case ViewPager.SCROLL_STATE_IDLE:
                    if (mPosition == 0) {
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                        mActionBar.hide();
                        if (galleryFragment.getActionMode() != null) {
                            galleryFragment.finishActionMode();
                        }
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
        //mViewPager.setPageTransformer(true, new DepthPageTransformer());
        mViewPager.setAdapter(mPagerAdapter);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.camera_icon:
            	mViewPager.setCurrentItem(CAMERA_FRAGMENT_NUMBER);
                return true;
            default: 
                return super.onOptionsItemSelected(item);
        }
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
    public void refreshAdapter() {
        mPagerAdapter.notifyDataSetChanged();
    }
}
