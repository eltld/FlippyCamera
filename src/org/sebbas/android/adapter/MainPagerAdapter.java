package org.sebbas.android.adapter;

import java.util.List;

import org.sebbas.android.flickcam.FolderFragment;
import org.sebbas.android.flickcam.GalleryFragment;
import org.sebbas.android.flickcam.MainFragmentActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;

public class MainPagerAdapter extends FragmentStatePagerAdapter {

    private List<Fragment> mFragments;
    private MainFragmentActivity mMainFragment;
    
    public MainPagerAdapter(MainFragmentActivity mainFragment, FragmentManager fragmentManager, List<Fragment> fragments) {
        super(fragmentManager);
        mFragments = fragments;
        mMainFragment = mainFragment;
    }
    
    @Override
    public Fragment getItem(int position) {
    	System.out.println("get item ");
        return mFragments.get(position);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }
    
    @Override
    public int getItemPosition(Object object) {
    	System.out.println("get item position");
        if (object instanceof GalleryFragment) {
        }
        if (object instanceof FolderFragment) {
        	mMainFragment.reloadFolderGallery();
        }
        return super.getItemPosition(object);
    }
}
