package org.sebbas.android.flippycamera;

import java.util.ArrayList;
import java.util.List;

import org.sebbas.android.adapter.MainPagerAdapter;
import org.sebbas.android.helper.Utils;
import org.sebbas.android.interfaces.AdapterCallback;
import org.sebbas.android.viewpager.DepthPageTransformer;
import org.sebbas.android.viewpager.ZoomOutPageTransformer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

public class MainFragmentActivity extends ActionBarActivity implements AdapterCallback<String> {

    private static final String TAG = "main_fragment";
    private static final int CAMERA_FRAGMENT_NUMBER = 0;
    private static final int FOLDER_FRAGMENT_NUMBER = 1;
    private static final String PREFS_NAME = "FlippyCameraPrefsFile";
        
    private MainPagerAdapter mPagerAdapter;
    private FragmentManager mFragmentManager;
    private int mPosition;
    private ActionBar mActionBar;
    private ViewPager mViewPager;
    private ArrayList<List <String>> mImagePaths;
    private Utils mUtils;
    
    // Fragments
    private CameraFragmentUI mCameraFragment;
    private FolderFragment mFolderFragment;
    
    // Overflow items
    private MenuItem mMenuShowHidden;
    private MenuItem mMenuHideHidden;
    private MenuItem mSpinnerIcon;
    private boolean mHiddenFolders = true;
    
    private boolean mIsRefreshing = false;
    
    // Navigation drawers
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerLeftList;
    private String[] mDrawerLeftContent;
    private LinearLayout mDrawerLeftLayout;
    private ListView mDrawerRightList;
    private String[] mDrawerRightContent;
    private LinearLayout mDrawerRightLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ON CREATE");
        
        restoreSharedPreferences();
        
        mFragmentManager = getSupportFragmentManager();
        mUtils = new Utils(this);
        mActionBar = getSupportActionBar();
        mActionBar.hide(); // Immediately hide ActionBar for startup
        
        mCameraFragment = CameraFragmentUI.newInstance();
        mFolderFragment = FolderFragment.newInstance();
        
        ArrayList<Fragment> fragmentList = new ArrayList<Fragment>();
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
                    lockDrawerForFragments();
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
        
        //if (DeviceInfo.supportsSDK(11)) mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setCurrentItem(CAMERA_FRAGMENT_NUMBER);
        
        // Navigation drawers
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLeftList = (ListView) findViewById(R.id.left_drawer);
        mDrawerLeftLayout = (LinearLayout) findViewById(R.id.left_drawer_layout);
        mDrawerLeftContent = getResources().getStringArray(R.array.drawer_left_array);
        
        mDrawerRightList = (ListView) findViewById(R.id.right_drawer);
        mDrawerRightLayout = (LinearLayout) findViewById(R.id.right_drawer_layout);
        mDrawerRightContent = getResources().getStringArray(R.array.drawer_right_array);
        
        // set up the drawer's list view with items and click listener
        mDrawerLeftList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, mDrawerLeftContent));
        mDrawerLeftList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerRightList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, mDrawerRightContent));
        mDrawerRightList.setOnItemClickListener(new DrawerItemClickListener());
        
        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                // TODO --> Animate the items in the navigation drawer
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        
        lockDrawerForFragments();

        if (savedInstanceState == null) {
            selectItem(0);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        mMenuShowHidden = menu.findItem(R.id.show_hidden);
        mMenuHideHidden = menu.findItem(R.id.hide_hidden); 
        mSpinnerIcon = menu.findItem(R.id.spinner_icon);
        MenuItemCompat.setActionView(mSpinnerIcon, R.layout.actionbar_indeterminate_progress);
        setSpinnerVisibility();
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
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.show_hidden:
            case R.id.hide_hidden:
                mHiddenFolders = !mHiddenFolders;
                this.supportInvalidateOptionsMenu();
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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
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
    
    private void setSpinnerVisibility() {
        if (mIsRefreshing) {
            mSpinnerIcon.setVisible(true);
        } else {
            mSpinnerIcon.setVisible(false);
        }
    }
    
    private void setActionItems() {
        if (mPosition == CAMERA_FRAGMENT_NUMBER) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            mActionBar.hide();
            mFolderFragment.finishActionMode(); // Also handles finish action mode of any child fragments
        } else if (mPosition == FOLDER_FRAGMENT_NUMBER) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            mActionBar.show();
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
    
    public ArrayList<List <String>> getImagePaths() {
        return mImagePaths;
    }
    
    public FolderFragment getFolderFragment() {
    	return mFolderFragment;
    }
    
    private class ImagePathLoader extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mIsRefreshing = true;
            MainFragmentActivity.this.supportInvalidateOptionsMenu();
        }

        @Override
        protected Void doInBackground(Void... params) {
            ArrayList<List<String>> realImagePaths = mUtils.getImagePaths(getHiddenFoldersMode());
            //mImagePaths = mUtils.convertImagePathsToThumbnailPaths(realImagePaths);
            mImagePaths = realImagePaths;
            mFolderFragment.updateAdapterContent(mImagePaths);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mFolderFragment.refreshAdapter();
            mIsRefreshing = false;
            MainFragmentActivity.this.supportInvalidateOptionsMenu();
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
    public void updateImagePaths(ArrayList<List <String>> imagePaths) {
        mImagePaths = imagePaths;
    }
    
    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }
    
    private void selectItem(int position) {
        // Update selected item and title, then close the drawer
        mDrawerLeftList.setItemChecked(position, true);
        mDrawerRightList.setItemChecked(position, true);
        
        //setTitle(mDrawerLeftContent[position]);
        //setTitle(mDrawerRightContent[position]);
        
        mDrawerLayout.closeDrawer(mDrawerLeftLayout);
        mDrawerLayout.closeDrawer(mDrawerRightLayout);
    }
    
    private void lockDrawerForFragments() {
        if (mPosition == CAMERA_FRAGMENT_NUMBER) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, findViewById(R.id.right_drawer_layout));
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, findViewById(R.id.left_drawer_layout));
        } else if (mPosition == FOLDER_FRAGMENT_NUMBER) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, findViewById(R.id.left_drawer_layout));
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, findViewById(R.id.right_drawer_layout));
        }
    }
}
