package org.sebbas.android.flickcam;

import java.util.ArrayList;

import org.sebbas.android.adapter.ImageSlidePagerAdapter;
import org.sebbas.android.interfaces.AdapterCallback;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class FullScreenImageSliderFragment extends Fragment implements AdapterCallback<String> {

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

    @Override
    public void refreshAdapter() {
        mPagerAdapter.notifyDataSetChanged();
    }

    @Override
    public void updateAdapterContent(ArrayList<String> imagePaths) {
        mPagerAdapter.updateImagePaths(imagePaths);
    }
}