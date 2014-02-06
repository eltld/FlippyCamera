package org.sebbas.android.adapter;

import java.util.ArrayList;

import org.sebbas.android.flickcam.R;
import org.sebbas.android.helper.TouchImageView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
 
public class FullScreenImageAdapter extends PagerAdapter {
 
    private Activity mActivity;
    private ArrayList<String> mImagePaths;
    private LayoutInflater inflater;
 
    // constructor
    public FullScreenImageAdapter(Activity activity,
            ArrayList<String> imagePaths) {
        this.mActivity = activity;
        this.mImagePaths = imagePaths;
    }
 
    @Override
    public int getCount() {
        return this.mImagePaths.size();
    }
 
    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((RelativeLayout) object);
    }
     
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        ImageView imgDisplay;
        Button btnClose;
  
        inflater = (LayoutInflater) mActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewLayout = inflater.inflate(R.layout.layout_fullscreen_image, container,
                false);
  
        imgDisplay = (TouchImageView) viewLayout.findViewById(R.id.imgDisplay);
        btnClose = (Button) viewLayout.findViewById(R.id.btnClose);
         
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(mImagePaths.get(position), options);
        imgDisplay.setImageBitmap(bitmap);
         
        // close button click event
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.finish();
            }
        });
  
        ((ViewPager) container).addView(viewLayout);
  
        return viewLayout;
    }
     
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ((ViewPager) container).removeView((RelativeLayout) object);
    }
}