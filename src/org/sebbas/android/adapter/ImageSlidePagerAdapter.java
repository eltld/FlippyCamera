package org.sebbas.android.adapter;

import java.util.ArrayList;

import org.sebbas.android.flippycamera.ImageSlideFragment;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class ImageSlidePagerAdapter extends FragmentStatePagerAdapter {
	
	private ArrayList<String> mImagePaths;
	
    public ImageSlidePagerAdapter(FragmentManager fm, ArrayList<String> imagePaths) {
        super(fm);
        mImagePaths = imagePaths;
    }

    @Override
    public Fragment getItem(int position) {
        return ImageSlideFragment.newInstance(mImagePaths.get(position));
    }

    @Override
    public int getCount() {
        return mImagePaths.size();
    }
    
    public void updateImagePaths(ArrayList<String> imagePaths) {
        mImagePaths = imagePaths;
        notifyDataSetChanged();
    }
}
