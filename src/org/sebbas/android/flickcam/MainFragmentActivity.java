package org.sebbas.android.flickcam;

import java.util.ArrayList;
import java.util.List;

import org.sebbas.android.adapter.MainPagerAdapter;
import org.sebbas.android.flickcam.PreferenceListFragment.OnPreferenceAttachedListener;
import org.sebbas.android.helper.DeviceInfo;
import org.sebbas.android.helper.Utils;
import org.sebbas.android.interfaces.AdapterCallback;
import org.sebbas.android.viewpager.DepthPageTransformer;
import org.sebbas.android.viewpager.ZoomOutPageTransformer;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceScreen;
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

public class MainFragmentActivity extends ActionBarActivity implements AdapterCallback<String>, OnPreferenceAttachedListener {

    private static final String TAG = "main_fragment";
    private static final int SETTINGS_FRAGMENT_NUMBER = 0;
    private static final int CAMERA_FRAGMENT_NUMBER = 1;
    private static final int GALLERY_FRAGMENT_NUMBER = 2;
    private static final String SETTINGS_TITLE = "Settings";
    private static final String PREFS_NAME = "FlickCamPrefsFile";
        
    private MainPagerAdapter mPagerAdapter;
    private FragmentManager mFragmentManager;
    private int mPosition;
    private ActionBar mActionBar;
    private ViewPager mViewPager;
    private ArrayList<List <String>> mImagePaths;
    private Utils mUtils;
    
    // Fragments
    private SettingsFragment mSettingsFragment;
    private PreferenceListFragment mPreferenceListFragment;
    private CameraFragmentUI mCameraFragment;
    private FolderFragment mFolderFragment;
    
    // Overflow items
    private MenuItem mMenuShowHidden;
    private MenuItem mMenuHideHidden;
    private MenuItem spinnerIcon;
    private Menu mMenu;
    private boolean mHiddenFolders = true;
    
    private boolean mIsRefreshing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ON CREATE");
        
        restoreSharedPreferences();
        
        mFragmentManager = getSupportFragmentManager();
        mUtils = new Utils(this);
        mActionBar = getSupportActionBar();
        mActionBar.hide(); // Immediately hide ActionBar for startup
        
        mSettingsFragment = SettingsFragment.newInstance();
        mCameraFragment = CameraFragmentUI.newInstance();
        mFolderFragment = FolderFragment.newInstance();
        
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
        
        if (DeviceInfo.supportsSDK(11)) mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setCurrentItem(CAMERA_FRAGMENT_NUMBER);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        mMenuShowHidden = menu.findItem(R.id.show_hidden);
        mMenuHideHidden = menu.findItem(R.id.hide_hidden); 
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
                mHiddenFolders = !mHiddenFolders;
                setMenuItemVisibility();
                reloadFolderPaths();
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
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        saveSharePreferences();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        super.onSupportNavigateUp();
        handleNavigationBack();
        handleHomeUpNavigation();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handleNavigationBack();
        handleHomeUpNavigation();
        //setupActionBarTitle();
    }
    
    private void setupActionBarTitle() {
        this.getSupportActionBar().setTitle(R.string.app_name);
    }
    
    public void handleNavigationBack() {
        // Make sure the underlying fragments adapter is up to date
        mFolderFragment.updateAdapterContent(mImagePaths);
        mFolderFragment.refreshAdapter();
        //This method is called when the up button is pressed. Just pop the back stack.
        System.out.println("before count is " + getSupportFragmentManager().getBackStackEntryCount());
        getSupportFragmentManager().popBackStackImmediate();
        System.out.println("after count is " + getSupportFragmentManager().getBackStackEntryCount());
    }
    
    public void handleHomeUpNavigation() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0 && mPosition == GALLERY_FRAGMENT_NUMBER) {
            // Turn on the "off" back navigation option
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            // Set the app name in the action bar
            getSupportActionBar().setTitle(R.string.app_name);
        } else if (getSupportFragmentManager().getBackStackEntryCount() == 1 && mPosition == GALLERY_FRAGMENT_NUMBER) {
            // Turn on the "on" back navigation option
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Set the name of the current folder
            mFolderFragment.getGalleryFragment().setupActionBarTitle();
        } else if (getSupportFragmentManager().getBackStackEntryCount() == 2 && mPosition == GALLERY_FRAGMENT_NUMBER) {
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
        mHiddenFolders = settings.getBoolean("hideFolderMode", false);
    }
    
    private void saveSharePreferences() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        
        // Save the hide folder mode
        editor.putBoolean("hideFolderMode", mHiddenFolders);
        
        // Commit the edits!
        editor.commit();
    }
    
    private void setMenuItemVisibility() {
        mMenuShowHidden.setVisible(!mHiddenFolders);
        mMenuHideHidden.setVisible(mHiddenFolders);
    }
    
    private void setActionItems() {
        if (mPosition == CAMERA_FRAGMENT_NUMBER) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            mActionBar.hide();
            mFolderFragment.finishActionMode(); // Also handles finish action mode of any child fragments
        } else if (mPosition == GALLERY_FRAGMENT_NUMBER) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            mActionBar.show();
            //mActionBar.setTitle(R.string.app_name);
            refreshGalleryUI();
        } else if (mPosition == SETTINGS_FRAGMENT_NUMBER) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            mActionBar.show();
            mActionBar.setTitle(SETTINGS_TITLE);
            
            // Empty the back stack completely
            getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
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
    public void reloadFolderPaths() {
        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                ImagePathLoader loader = new ImagePathLoader();
                loader.execute();
            }
            
        });
    }
    
    public boolean getHiddenFoldersMode() {
        return mHiddenFolders;
    }
    
    public void setHiddenFoldersMode(boolean mode) {
        mHiddenFolders = mode;
    }
    
    public void updateImagePaths(ArrayList<List <String>> imagePaths) {
        mImagePaths = imagePaths;
    }
    
    public ArrayList<List <String>> getImagePaths() {
        return mImagePaths;
    }
    
    // Every time we swipe over to the still opened gallery fragment we refresh it (since there might be a new image)
    private void refreshGalleryUI() {
        GalleryFragment galleryFragment = mFolderFragment.getGalleryFragment();
        if(galleryFragment != null) {
            galleryFragment.refreshAdapter();
            FullScreenImageSliderFragment fullscreenFragment = galleryFragment.getFullScreenImageSliderFragment();
            if (fullscreenFragment != null) {
                fullscreenFragment.refreshAdapter();
            }
        }
    }
    
    private class ImagePathLoader extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setSpinnerIconInProgress(true);
        }

        @Override
        protected Void doInBackground(Void... params) {
            mImagePaths = mUtils.getImagePaths(getHiddenFoldersMode());
            mFolderFragment.updateAdapterContent(mImagePaths);
            
            // Update the gallery view fragment
            GalleryFragment galleryFragment = mFolderFragment.getGalleryFragment();
            if (galleryFragment != null) {
                int folderPosition = galleryFragment.getFolderPosition();
                
                galleryFragment.updateAdapterContent((ArrayList<String>) mImagePaths.get(folderPosition));
                
                // Update the fullscreen view fragment
                FullScreenImageSliderFragment fullscreenFragment = galleryFragment.getFullScreenImageSliderFragment();
                if (fullscreenFragment != null) {
                    fullscreenFragment.updateAdapterContent((ArrayList<String>) mImagePaths.get(folderPosition));
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mFolderFragment.refreshAdapter();
            setSpinnerIconInProgress(false);
        }
        
    }

    @Override
    public void refreshAdapter() {
        mPagerAdapter.notifyDataSetChanged();
    }

    @Override
    public void updateAdapterContent(ArrayList<String> list) {
        // Not needed here hence not implemented
    }

	@Override
	public void onPreferenceAttached(PreferenceScreen root, int xmlId) {
	}
}
