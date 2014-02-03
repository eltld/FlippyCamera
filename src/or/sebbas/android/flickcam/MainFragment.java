package or.sebbas.android.flickcam;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import or.sebbas.android.flickcam.CameraLoaderFragment.ProgressListener;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.Menu;

public class MainFragment extends FragmentActivity implements ProgressListener {

    private static final String TAG_SPLASH_SCREEN = "splash_screen";
    private static final String TAG_CAMERA_LOADER = "camera_loader";
    
    private PagerAdapter mPagerAdapter;
    private CameraLoaderFragment mCameraLoaderFragment;
    private SplashScreenFragment mSplashScreenFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialiseStartup();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void initialisePaging(ArrayList<Fragment>fragmentList) {
        /*List<Fragment> fragments = new Vector<Fragment>();
        fragments.add(Fragment.instantiate(this, CameraFragment.class.getName()));
        fragments.add(Fragment.instantiate(this, GalleryFragment.class.getName()));*/
        
        mPagerAdapter = new PagerAdapter(super.getSupportFragmentManager(), fragmentList);
        ViewPager pager = (ViewPager)super.findViewById(R.id.viewpager);
        pager.setAdapter(mPagerAdapter);
    }
    
    private void initialiseStartup() {
        final FragmentManager fm = getSupportFragmentManager();
        
        mCameraLoaderFragment = (CameraLoaderFragment) fm.findFragmentByTag(TAG_CAMERA_LOADER);
        if (mCameraLoaderFragment == null) {
            mCameraLoaderFragment = new CameraLoaderFragment();
            mCameraLoaderFragment.setProgressListener(this);
            mCameraLoaderFragment.startLoading(this.getApplicationContext());
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
            //onCompletion(mCameraLoaderFragment.hasLoaded());
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

    @Override
    public void onCompletion(ArrayList<Fragment> fragmentList) {
        if (fragmentList != null) {
            setContentView(R.layout.viewpager_layout);
            initialisePaging(fragmentList);
        }
    }

    @Override
    public void onProgressUpdate(int value) {
        // TODO Auto-generated method stub
        
    }
}
