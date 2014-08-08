package org.sebbas.android.adapter;

import java.util.List;

import org.sebbas.android.flippycamera.FolderFragment;
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
        if (object instanceof FolderFragment) {
            ((FolderFragment) object).refreshAdapter();
        }
        return super.getItemPosition(object);
    }
}
