package org.sebbas.android.adapter;

import java.util.List;

import org.sebbas.android.flippycamera.FolderFragment;
import org.sebbas.android.flippycamera.GalleryFragment;
import org.sebbas.android.flippycamera.MainFragmentActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class MainPagerAdapter extends FragmentStatePagerAdapter {

    private List<Fragment> mFragments;
    
    public MainPagerAdapter(MainFragmentActivity mainFragment, FragmentManager fragmentManager, List<Fragment> fragments) {
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
    	System.out.println("Entered");
        if (object instanceof GalleryFragment) {
        	System.out.println("Entered gallery");
        	((GalleryFragment) object).refreshAdapter();
        }
        if (object instanceof FolderFragment) {
        	System.out.println("Entered folder");
        	((FolderFragment) object).refreshAdapter();
        }
        return super.getItemPosition(object);
    }
}
