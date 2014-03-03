package org.sebbas.android.adapter;

import java.util.List;

import org.sebbas.android.flickcam.GalleryFragment;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class MainPagerAdapter extends FragmentStatePagerAdapter {

    private List<Fragment> fragments;
    
    public MainPagerAdapter(FragmentManager fm, List<Fragment> fragments) {
        super(fm);
        this.fragments = fragments;
    }
    
    @Override
    public Fragment getItem(int position) {
        /*switch(position) {
            default: return CameraFragment.newInstance();
            case 1: return GalleryFragment.newInstance();
        }*/
        return fragments.get(position);

    }

    @Override
    public int getCount() {
        return fragments.size();
    }
    
    @Override
    public int getItemPosition(Object object) {
        if (object instanceof GalleryFragment) {
            ((GalleryFragment) object).setupGridView();
        }
        return super.getItemPosition(object);
    }
}
