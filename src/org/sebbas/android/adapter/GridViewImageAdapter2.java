package org.sebbas.android.adapter;

import static android.widget.ImageView.ScaleType.CENTER_CROP;

import java.io.File;
import java.util.ArrayList;

import org.sebbas.android.flippycamera.GalleryActivity;
import org.sebbas.android.flippycamera.R;
import org.sebbas.android.views.SquaredImageView;

import com.squareup.picasso.Picasso;
 
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
 
public class GridViewImageAdapter2 extends BaseAdapter {
 
    private Context mContext;
    private GalleryActivity mGalleryActivity;
    public ArrayList<String> mImagePaths = new ArrayList<String>();
 
    public GridViewImageAdapter2(GalleryActivity galleryActivity, ArrayList<String> filePaths) {
        mContext = galleryActivity;
        mGalleryActivity = galleryActivity;
        mImagePaths = filePaths;
    }
 
    @Override
    public int getCount() {
        return mImagePaths.size();
    }
 
    @Override
    public String getItem(int position) {
        return mImagePaths.get(position);
    }
 
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    public ArrayList<String> getImagePaths() {
        return mImagePaths;
    }
 
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SquaredImageView squaredView = (SquaredImageView) convertView;
        if (squaredView == null) {
            squaredView = new SquaredImageView(mContext);
            squaredView.setScaleType(CENTER_CROP);
        } else {
        	//squaredView = (SquaredImageView) convertView;
        }
        squaredView.setBackgroundResource(R.drawable.gallery_image_selector);
        
        // Get the image URL for the current position.
        String url = getItem(position);
        
        // Set a border for the view if it is in selected state
        boolean isSelected = mGalleryActivity.getSelectedItemsList().contains(position);
        if (isSelected) {
            squaredView.setSelected(true);
            squaredView.setBackgroundResource(R.drawable.gallery_image_border_selected);
        } else {
            squaredView.setSelected(false);
        }
        
        // Trigger the download of the URL asynchronously into the image view.
        Picasso.with(mContext)
            .load(new File(url)) 
            .noFade()
            .centerCrop()
            .placeholder(R.color.image_placeholder)
            .error(R.color.image_error)
            .fit()
            .into(squaredView);
        return squaredView;
    }

	public void updateImagePaths(ArrayList<String> imagePaths) {
        mImagePaths = imagePaths;
    }
}
