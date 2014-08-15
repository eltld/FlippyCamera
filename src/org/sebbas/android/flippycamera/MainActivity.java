package org.sebbas.android.flippycamera;

import java.util.ArrayList;
import java.util.List;

import org.sebbas.android.adapter.MainPagerAdapter;
import org.sebbas.android.adapter.NavigationDrawerListAdapter;
import org.sebbas.android.helper.Utils;
import org.sebbas.android.interfaces.AdapterCallback;
import org.sebbas.android.viewpager.DepthPageTransformer;
import org.sebbas.android.viewpager.ZoomOutPageTransformer;
import org.sebbas.android.views.NavigationDrawerItem;

import uk.co.senab.photoview.PhotoViewAttacher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
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

public class MainActivity extends ActionBarActivity implements AdapterCallback<String> {

    private static final String TAG = "main_fragment";
    private static final int CAMERA_FRAGMENT_NUMBER = 1;
    private static final int FOLDER_FRAGMENT_NUMBER = 0;
    private static final String PREFS_NAME = "FlippyCameraPrefsFile";
        
    private MainPagerAdapter mPagerAdapter;
    private FragmentManager mFragmentManager;
    private int mPosition;
    private ActionBar mActionBar;
    private ViewPager mViewPager;
    private ArrayList<List <String>> mImagePaths = new ArrayList<List <String>>();
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
    private View mDrawerLeft;
    
    private ListView mDrawerRightList;
    private String[] mDrawerRightContent;
    private LinearLayout mDrawerRightLayout;
    private View mDrawerRight;
    
    private String[] mNavigationMenuTextLeft;
    private TypedArray mNavigationMenuIconsLeft;
    
    private String[] mNavigationMenuTextRight;
    private TypedArray mNavigationMenuIconsRight;
    
    private ArrayList<NavigationDrawerItem> mNavigationDrawerItemsLeft;
    private NavigationDrawerListAdapter mNavigationDrawerListAdapterLeft;
    private ArrayList<NavigationDrawerItem> mNavigationDrawerItemsRight;
    private NavigationDrawerListAdapter mNavigationDrawerListAdapterRight;
    
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
        fragmentList.add(mFolderFragment);
        fragmentList.add(mCameraFragment);
        
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
        mNavigationMenuTextLeft = getResources().getStringArray(R.array.drawer_left_array);
        mNavigationMenuIconsLeft = getResources().obtainTypedArray(R.array.drawer_left_icons);
        
        mNavigationMenuTextRight = getResources().getStringArray(R.array.drawer_right_array);
        mNavigationMenuIconsRight = getResources().obtainTypedArray(R.array.drawer_right_icons);
        
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLeftList = (ListView) findViewById(R.id.left_drawer);
        mDrawerRightList = (ListView) findViewById(R.id.right_drawer);
        mDrawerLeftLayout = (LinearLayout) findViewById(R.id.left_drawer_layout);
        mDrawerRightLayout = (LinearLayout) findViewById(R.id.right_drawer_layout);
 
        mNavigationDrawerItemsLeft = new ArrayList<NavigationDrawerItem>();
        mNavigationDrawerItemsLeft.add(new NavigationDrawerItem(mNavigationMenuTextLeft[0], mNavigationMenuIconsLeft.getResourceId(0, -1)));
        mNavigationDrawerItemsLeft.add(new NavigationDrawerItem(mNavigationMenuTextLeft[1], mNavigationMenuIconsLeft.getResourceId(1, -1)));
        mNavigationDrawerItemsLeft.add(new NavigationDrawerItem(mNavigationMenuTextLeft[2], mNavigationMenuIconsLeft.getResourceId(2, -1)));
        mNavigationDrawerItemsLeft.add(new NavigationDrawerItem(mNavigationMenuTextLeft[3], mNavigationMenuIconsLeft.getResourceId(3, -1), true, "22"));
        mNavigationDrawerItemsLeft.add(new NavigationDrawerItem(mNavigationMenuTextLeft[4], mNavigationMenuIconsLeft.getResourceId(4, -1)));
        
        mNavigationDrawerItemsRight = new ArrayList<NavigationDrawerItem>();
        mNavigationDrawerItemsRight.add(new NavigationDrawerItem(mNavigationMenuTextRight[0], mNavigationMenuIconsRight.getResourceId(0, -1)));
        mNavigationDrawerItemsRight.add(new NavigationDrawerItem(mNavigationMenuTextRight[1], mNavigationMenuIconsRight.getResourceId(1, -1)));
        mNavigationDrawerItemsRight.add(new NavigationDrawerItem(mNavigationMenuTextRight[2], mNavigationMenuIconsRight.getResourceId(2, -1)));
        mNavigationDrawerItemsRight.add(new NavigationDrawerItem(mNavigationMenuTextRight[3], mNavigationMenuIconsRight.getResourceId(3, -1), true, "22"));
        mNavigationDrawerItemsRight.add(new NavigationDrawerItem(mNavigationMenuTextRight[4], mNavigationMenuIconsRight.getResourceId(4, -1)));
         
        // Recycle the typed array
        mNavigationMenuIconsLeft.recycle();
        mNavigationMenuIconsRight.recycle();
 
        mDrawerLeftList.setOnItemClickListener(new SlideMenuClickListener());
        mDrawerRightList.setOnItemClickListener(new SlideMenuClickListener());
 
        // setting the nav drawer list adapter
        mNavigationDrawerListAdapterLeft = new NavigationDrawerListAdapter(getApplicationContext(), mNavigationDrawerItemsLeft);
        mNavigationDrawerListAdapterRight = new NavigationDrawerListAdapter(getApplicationContext(), mNavigationDrawerItemsRight);
        
        mDrawerLeftList.setAdapter(mNavigationDrawerListAdapterLeft);
        mDrawerRightList.setAdapter(mNavigationDrawerListAdapterRight);
 
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, //nav menu toggle icon
                R.string.app_name, // nav drawer open - description for accessibility
                R.string.app_name // nav drawer close - description for accessibility
        ) {
            public void onDrawerClosed(View view) {
                //getSupportActionBar().setTitle(mTitle);
                // calling onPrepareOptionsMenu() to show action bar icons
                //invalidateOptionsMenu();
            	setActionBarArrowDependingOnAdapterMode();
            }
 
            public void onDrawerOpened(View drawerView) {
                //getSupportActionBar().setTitle(mDrawerTitle);
                // calling onPrepareOptionsMenu() to hide action bar icons
                //invalidateOptionsMenu();
            	mDrawerToggle.setDrawerIndicatorEnabled(true);
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
 
        if (savedInstanceState == null) {
            // on first time display view for first nav item
            displayView(0);
        }
        
        lockDrawerForFragments();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	if (mFolderFragment.getAdapterMode() == mFolderFragment.FOLDER_MODE) {
    		getMenuInflater().inflate(R.menu.main, menu);
            mMenuShowHidden = menu.findItem(R.id.show_hidden);
            mMenuHideHidden = menu.findItem(R.id.hide_hidden);
    	} else if (mFolderFragment.getAdapterMode() == mFolderFragment.GALLERY_MODE) {
    		
    	} else if (mFolderFragment.getAdapterMode() == mFolderFragment.IMAGE_MODE) {
    		// TODO
    	}
         
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
         // Restore the preferences from the previous session
        restoreSharedPreferences(); 
        
        // enabling action bar app icon and behaving it as toggle button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        saveSharePreferences();
    }
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
    
    @Override
    public void onBackPressed() {
        
        if (navigationDrawerIsOpen(true)) {
            closeNavigationDrawer(true); // Close the left drawer
        } else if (navigationDrawerIsOpen(false)) {
            closeNavigationDrawer(false); // Close the right drawer
        } else if (isBackNavigable()) {
        	mFolderFragment.navigateBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        super.onSupportNavigateUp();
        if (navigationDrawerIsOpen(true)) {
            closeNavigationDrawer(true);
        } else if (isBackNavigable()) {
        	mFolderFragment.navigateBack();
        } else {
        	openNavigationDrawer(true);
        }
        return true;
    }
    
    private boolean isBackNavigable() {
        return mFolderFragment.getAdapterMode() > mFolderFragment.FOLDER_MODE;
    }
    
    private void openNavigationDrawer(boolean openLeftDrawer) {
        if (openLeftDrawer) {
            mDrawerLayout.openDrawer(mDrawerLeftLayout);
        } else {
            mDrawerLayout.openDrawer(mDrawerRightLayout);
        }
    }
    
    private void closeNavigationDrawer(boolean closeLeftDrawer) {
        if (closeLeftDrawer) {
            mDrawerLayout.closeDrawer(mDrawerLeftLayout);
        } else {
            mDrawerLayout.closeDrawer(mDrawerRightLayout);
        }
    }
    
    private boolean navigationDrawerIsOpen(boolean leftDrawer) {
        if (leftDrawer) {
            return mDrawerLayout.isDrawerOpen(mDrawerLeftLayout);
        } else {
            return mDrawerLayout.isDrawerOpen(mDrawerRightLayout);
        }
    }

    private int getPageMargin() {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10*2, getResources().getDisplayMetrics());
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
            getSupportActionBar().hide();
            mFolderFragment.finishActionMode(); // Also handles finish action mode of any child fragments
        } else if (mPosition == FOLDER_FRAGMENT_NUMBER) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getSupportActionBar().show();
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
    
    private boolean getHiddenFoldersMode() {
        return mHiddenFolders;
    }
    
    public ArrayList<List <String>> getImagePaths() {
        return mImagePaths;
    }
    
    public ArrayList<String> getImagePathsAt(int position) {
        return (ArrayList<String>) mImagePaths.get(position);
    }
    
    public FolderFragment getFolderFragment() {
        return mFolderFragment;
    }
    
    private class ImagePathLoader extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mIsRefreshing = true;
            MainActivity.this.supportInvalidateOptionsMenu();
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
            MainActivity.this.supportInvalidateOptionsMenu();
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
    
    private void lockDrawerForFragments() {
        if (mPosition == FOLDER_FRAGMENT_NUMBER) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, findViewById(R.id.right_drawer_layout));
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, findViewById(R.id.left_drawer_layout));
        } else if (mPosition == CAMERA_FRAGMENT_NUMBER) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, findViewById(R.id.left_drawer_layout));
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, findViewById(R.id.right_drawer_layout));
        }
    }
    
     void setActionBarArrowDependingOnAdapterMode() {
        int modeCount = mFolderFragment.getAdapterMode();
        mDrawerToggle.setDrawerIndicatorEnabled(modeCount == 0);
    }
    
    private class SlideMenuClickListener implements ListView.OnItemClickListener {
        
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // display view for selected nav drawer item
            displayView(position);
        }
    }
    
    private void displayView(int position) {
        // update the main content by replacing fragments
        Fragment fragment = null;
        switch (position) {
        case 0:
            //fragment = new HomeFragment();
            break;
        case 1:
            //fragment = new FindPeopleFragment();
            break;
        case 2:
            //fragment = new PhotosFragment();
            break;
        case 3:
            //fragment = new CommunityFragment();
            break;
        case 4:
            //fragment = new PagesFragment();
            break;
        case 5:
            //fragment = new WhatsHotFragment();
            break;
 
        default:
            break;
        }
 
        if (fragment != null) {
            /*FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.frame_container, fragment).commit();
 
            // update selected item and title, then close the drawer
            mDrawerList.setItemChecked(position, true);
            mDrawerList.setSelection(position);
            setTitle(navMenuTitles[position]);
            mDrawerLayout.closeDrawer(mDrawerList);*/
        } else {
            // error in creating fragment
            Log.e("MainActivity", "Error in creating fragment");
        }
    }
}
