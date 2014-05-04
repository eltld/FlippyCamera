package org.sebbas.android.flickcam;

import java.util.ArrayList;

import org.sebbas.android.adapter.MainPagerAdapter;
import org.sebbas.android.interfaces.AdapterCallback;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

public class MainFragmentActivity extends ActionBarActivity implements AdapterCallback {

    private static final String TAG = "main_fragment";
    private static final int SETTINGS_FRAGMENT_NUMBER = 0;
    private static final int CAMERA_FRAGMENT_NUMBER = 1;
    private static final int GALLERY_FRAGMENT_NUMBER = 2;
    private static final String SETTINGS_TITLE = "Settings";
    private static final String APP_TITLE = "FlickCam";
    private static final String PREFS_NAME = "FlickCamPrefsFile";
        
    private MainPagerAdapter mPagerAdapter;
    private FragmentManager mFragmentManager;
    private int mPosition;
    private ActionBar mActionBar;
    private ViewPager mViewPager;
    
    // Fragments
    private SettingsFragment mSettingsFragment;
    private CameraFragmentUI mCameraFragment;
    private FolderFragment mFolderFragment;
    
    // Overflow items
    private MenuItem showHidden;
    private MenuItem hideHidden;
    private MenuItem spinnerIcon;
    private Menu mMenu;
    private boolean mHideFolders = true;
    
    private boolean mIsRefreshing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ON CREATE");
        
        restoreSharedPreferences();
        
        mFragmentManager = getSupportFragmentManager();
        mActionBar = getSupportActionBar();
        mActionBar.hide(); // Immediately hide ActionBar for startup
        
        mSettingsFragment = SettingsFragment.newInstance();
        mCameraFragment = CameraFragmentUI.newInstance();
        mFolderFragment = FolderFragment.newInstance(mHideFolders);
        
        ArrayList<Fragment> fragmentList = new ArrayList<Fragment>();
        fragmentList.add(mSettingsFragment);
        fragmentList.add(mCameraFragment);
        fragmentList.add(mFolderFragment);
        
        setContentView(R.layout.viewpager_layout);
        
        mPagerAdapter = new MainPagerAdapter(this, mFragmentManager, fragmentList);
        mViewPager = (ViewPager)super.findViewById(R.id.viewpager);
        
        // This fixes the overlapping fragments inside the viewpager
        mViewPager.setPageMargin(getPageMargin());
        mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
        
            @Override
            public void onPageScrollStateChanged(int state) {
                switch(state) {
                case ViewPager.SCROLL_STATE_IDLE:
                    setActionItems();
                    handleHomeUpNavigation();
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
        mViewPager.setCurrentItem(CAMERA_FRAGMENT_NUMBER);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        showHidden = menu.findItem(R.id.show_hidden);
        hideHidden = menu.findItem(R.id.hide_hidden); 
        spinnerIcon = menu.findItem(R.id.spinner_icon);
        mMenu = menu;
        setSpinnerIconInProgress(mIsRefreshing);
        setMenuItemVisibility();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.camera_icon:
                mViewPager.setCurrentItem(CAMERA_FRAGMENT_NUMBER);
                return true;
            case R.id.action_settings:
                return true;
            case R.id.show_hidden:
            case R.id.hide_hidden:
                mHideFolders = !mHideFolders;
                setMenuItemVisibility();
                mFolderFragment.reloadAdapterContent(mHideFolders);
                return true;
                
            default: 
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "ON RESUME");
        restoreSharedPreferences(); // Restore the preferences from the previous session
        setActionItems(); // Re-set the action bar (showing or not)
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        saveSharePreferences();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        //This method is called when the up button is pressed. Just the pop back stack.
        getSupportFragmentManager().popBackStackImmediate();
        handleHomeUpNavigation();
        return true;
    }

    @Override
	public void onBackPressed() {
		super.onBackPressed();
		//This method is called when the up button is pressed. Just the pop back stack.
        getSupportFragmentManager().popBackStackImmediate();
        handleHomeUpNavigation();
	}
    
    private void handleHomeUpNavigation() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
        	// Turn on the "off" back navigation option
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        } else {
        	// Turn on the "on" back navigation option
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

	private int getPageMargin() {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20*2, getResources().getDisplayMetrics());
    }
    
    private void restoreSharedPreferences() {
        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        mHideFolders = settings.getBoolean("hideFolderMode", false);
        //mPosition = settings.getInt("viewpager_position", CAMERA_FRAGMENT_NUMBER);
    }
    
    private void saveSharePreferences() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        
        // Save the hide folder mode
        editor.putBoolean("hideFolderMode", mHideFolders);
        
        // Save the current viewpager position
        //editor.putInt("viewpager_position", mPosition);

        // Commit the edits!
        editor.commit();
    }
    
    private void setMenuItemVisibility() {
        showHidden.setVisible(!mHideFolders);
        hideHidden.setVisible(mHideFolders);
    }
    
    
    private void setActionItems() {
    	System.out.println("count is " + getSupportFragmentManager().getBackStackEntryCount());
        if (mPosition == CAMERA_FRAGMENT_NUMBER) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            mActionBar.hide();
            mFolderFragment.finishActionMode(); // Also handles finish action mode of any child fragments
            
        } else if (mPosition == GALLERY_FRAGMENT_NUMBER) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            mActionBar.show();
            mActionBar.setTitle(APP_TITLE);
           
        } else if (mPosition == SETTINGS_FRAGMENT_NUMBER) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            mActionBar.show();
            mActionBar.setTitle(SETTINGS_TITLE);
        }
    }
    
    public void setSpinnerIconInProgress(boolean refreshing) {
        if (spinnerIcon == null) {
            mIsRefreshing = refreshing;
        } else {
            if (refreshing) {
                MenuItemCompat.setActionView(spinnerIcon, R.layout.actionbar_indeterminate_progress);
            } else {
                MenuItemCompat.setActionView(spinnerIcon, null);
            }
        }
    }
    
    // Allows other threads to request the folderfragment to reload
    public void reloadFolderGallery() {
        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mFolderFragment.reloadAdapterContent(mHideFolders);
            }
            
        });
    }
    
    @Override
    public void refreshAdapter() {
        mPagerAdapter.notifyDataSetChanged();
    }

    @Override
    public void reloadAdapterContent(boolean hiddenFolders) {
        // Not needed here, hence not implemented
    }
    
    public MainPagerAdapter getAdapter() {
    	return mPagerAdapter;
    }

	@Override
	public void updateAdapterInstanceVariables() {
		// Not needed here, hence not implemented
	}
}
