package org.sebbas.android.flickcam;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class FullScreenImageSliderFragment extends Fragment {

    private ViewPager mPager;
    private ImageSlidePagerAdapter mPagerAdapter;
    private ArrayList<String> mImagePaths;
    private int mFolderPosition;

    public static FullScreenImageSliderFragment newInstance(ArrayList<String> imagePaths, int position) {
    	FullScreenImageSliderFragment fullScreenImageSliderFragment = new FullScreenImageSliderFragment();
    	
    	Bundle args = new Bundle();
    	args.putStringArrayList("imagePaths", imagePaths);
    	args.putInt("position", position);
    	fullScreenImageSliderFragment.setArguments(args);
    	
    	return fullScreenImageSliderFragment;
    }
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mImagePaths = this.getArguments().getStringArrayList("imagePaths");
		mFolderPosition = this.getArguments().getInt("position");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout rootLayout = (LinearLayout)inflater.inflate(R.layout.image_fullscreen_view, container, false);
        
        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) rootLayout.findViewById(R.id.pager);
        mPagerAdapter = new ImageSlidePagerAdapter(this.getChildFragmentManager(), mImagePaths);
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(mFolderPosition);
        
        return rootLayout;
    }
	
	public ImageSlideFragment getImageSlideFragment() {
		return (ImageSlideFragment) mPagerAdapter.getItem(mFolderPosition);
	}

	
    private class ImageSlidePagerAdapter extends FragmentStatePagerAdapter {
    	
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
    }
}