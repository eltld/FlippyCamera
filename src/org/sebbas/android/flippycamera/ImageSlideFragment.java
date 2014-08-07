package org.sebbas.android.flippycamera;

import java.io.File;

import org.sebbas.android.helper.Utils;
import org.sebbas.android.views.TouchImageView;

import com.squareup.picasso.Picasso;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ImageSlideFragment extends Fragment {

    private String mImagePath;
    private Context mContext;
    private Utils mUtils;
    private MainFragmentActivity mMainFragment;
    
    public static ImageSlideFragment newInstance(String imagePath) {
        ImageSlideFragment imageSlideFragment = new ImageSlideFragment();
        
        Bundle args = new Bundle();
        args.putString("imagePath", imagePath);
        imageSlideFragment.setArguments(args);
        
        return imageSlideFragment;
        
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getActivity();
        mImagePath = this.getArguments().getString("imagePath");
        mUtils = new Utils(mContext);
        mMainFragment = (MainFragmentActivity) this.getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.layout_fullscreen_image, container, false);
        TouchImageView touchImageView = (TouchImageView) rootView.findViewById(R.id.imgDisplay);
        
        Picasso.with(mContext) 
            .load(new File(mImagePath)) 
            .noFade()
            .centerInside()
            .placeholder(R.drawable.ic_action_camera)
            .error(R.drawable.ic_action_camera)
            .fit()
            .into(touchImageView);
        
        setupActionBarTitle();
        return rootView;
    }
    
    public void setupActionBarTitle() {
    	mMainFragment.getSupportActionBar().setTitle(mUtils.getFileName(mImagePath));
    }
}