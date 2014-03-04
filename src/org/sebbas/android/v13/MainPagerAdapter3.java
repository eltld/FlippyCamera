package org.sebbas.android.v13;

import org.sebbas.android.flickcam.R;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.hardware.Camera;
import android.support.v13.app.FragmentPagerAdapter;

public class MainPagerAdapter3 extends FragmentPagerAdapter {

    private static final int NUM_ITEMS = 2;

    private FragmentManager mFragmentManager;
    private CameraFragment mCameraFragment;
    private int mCurrentCameraId;
    
    public MainPagerAdapter3(FragmentManager fragmentManager) {
        super(fragmentManager);
        mFragmentManager = fragmentManager;
        // By default, the back camera is chosen for first initialization
        mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK; 
    }
    
    @SuppressLint("NewApi")
    @Override
    public Fragment getItem(int position) {
        System.out.println("Called getitem");
        System.out.println(mFragmentManager.findFragmentByTag(CameraFragment.TAG));
        switch(position) {
        case 3:
               if (mCameraFragment == null) {
                    mCameraFragment = CameraFragment.newInstance(mCurrentCameraId);
                } 
                return mCameraFragment;
                
            default: 
                return GalleryFragment.newInstance();
        }
    }

    @Override
    public int getCount() {
        return NUM_ITEMS;
    }
    
    @Override
    public int getItemPosition(Object object) {
    	System.out.println("Called");
        if (object instanceof GalleryFragment) {
            ((GalleryFragment) object).setupGridView();
        }
        if (object instanceof CameraFragment) {
            return POSITION_NONE;
        }
        return POSITION_UNCHANGED;
    }

    @SuppressLint("NewApi")
	public void switchCameraFragment() {
        switchCamera();
        
        mFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                    R.animator.card_flip_right_in, R.animator.card_flip_right_out,
                    R.animator.card_flip_left_in, R.animator.card_flip_left_out)
            .replace(R.id.viewpager, CameraFragment.newInstance(mCurrentCameraId))
            .addToBackStack(null)
            .commit();
        //mCameraFragment = CameraFragment.newInstance(mCurrentCameraId);
        System.out.println("notfiying");
        notifyDataSetChanged();
    }
    
    private void switchCamera() {
        if (isBackCamera()) {
            mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
    }
    
    public boolean isBackCamera() {
        return (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK);
    }
    
    public boolean isFrontCamera() {
        return (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT);
    }
}
