package org.sebbas.android.adapter;

import java.util.List;

import org.sebbas.android.flickcam.CameraFragment;
import org.sebbas.android.flickcam.GalleryFragment;
import org.sebbas.android.flickcam.R;
import org.sebbas.android.interfaces.CameraFragmentListener;
import org.sebbas.android.listener.FragmentListener;

import android.hardware.Camera;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;

public class MainPagerAdapter extends FragmentPagerAdapter {

    private static final int NUM_ITEMS = 2;

    private List<Fragment> mFragments;
    private FragmentManager mFragmentManager;
    private CameraFragment mCameraFragment;
    private int mCurrentCameraId;
    
    public MainPagerAdapter(FragmentManager fragmentManager/*, List<Fragment> fragments*/) {
        super(fragmentManager);
        mFragmentManager = fragmentManager;
        //mFragments = fragments;
        // By default, the back camera is chosen for first initialization
        mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK; 
    }
    
    @Override
    public Fragment getItem(int position) {
        System.out.println("Called getitem");
        System.out.println(mFragmentManager.findFragmentByTag(CameraFragment.TAG));
        switch(position) {
            default:
                if (mCameraFragment == null) {
                    mCameraFragment = CameraFragment.newInstance(mCurrentCameraId);
                } 
                return mCameraFragment;
                
            case 1: 
                return GalleryFragment.newInstance();
        }
        //return fragments.get(position);
    }

    @Override
    public int getCount() {
        return NUM_ITEMS; //mFragments.size();
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

    public void switchCameraFragment() {
        switchCamera();
        
        mFragmentManager
            .beginTransaction()
            /*.setCustomAnimations(
                    R.animator.card_flip_right_in, R.animator.card_flip_right_out,
                    R.animator.card_flip_left_in, R.animator.card_flip_left_out)*/
            .remove(mCameraFragment)
            //.addToBackStack(null)
            .commit();
        mCameraFragment = CameraFragment.newInstance(mCurrentCameraId);
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
