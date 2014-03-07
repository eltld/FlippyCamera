package org.sebbas.android.adapter;

import java.util.List;

import org.sebbas.android.flickcam.GalleryFragment;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class MainPagerAdapter extends FragmentPagerAdapter {

    private List<Fragment> mFragments;
    
    public MainPagerAdapter(FragmentManager fragmentManager, List<Fragment> fragments) {
        super(fragmentManager);
        mFragments = fragments;
    }
    
    @Override
    public Fragment getItem(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }
    
    @Override
    public int getItemPosition(Object object) {
        if (object instanceof GalleryFragment) {
            ((GalleryFragment) object).setupGridView();
        }
        return POSITION_UNCHANGED;
    }
}
