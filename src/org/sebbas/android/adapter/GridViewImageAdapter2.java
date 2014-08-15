package org.sebbas.android.adapter;

import static android.widget.ImageView.ScaleType.CENTER_CROP;

import java.io.File;

import org.sebbas.android.flippycamera.FolderFragment;
import org.sebbas.android.flippycamera.R;
import org.sebbas.android.views.SquaredImageView;
import org.sebbas.android.flippycamera.MainActivity;

import com.squareup.picasso.Picasso;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
 
public class GridViewImageAdapter2 extends BaseAdapter {
 
    private Context mContext;
    private FolderFragment mFolderFragment;
    private int mFolderPosition;
 
    public GridViewImageAdapter2(FolderFragment folderFragment, int folderPosition) {
        mContext = folderFragment.getActivity();;
        mFolderFragment = folderFragment;
        mFolderPosition = folderPosition;
    }

    @Override
    public int getCount() {
    	return ((MainActivity) mContext).getImagePathsAt(mFolderPosition).size();
    }
 
    @Override
    public String getItem(int position) {
        return ((MainActivity) mContext).getImagePathsAt(mFolderPosition).get(position);
    }
 
    @Override
    public long getItemId(int position) {
        return position;
    }
 
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SquaredImageView squaredView = null;
        if (convertView == null) {
            squaredView = new SquaredImageView(mContext);
            squaredView.setScaleType(CENTER_CROP);
        } else {
        	squaredView = (SquaredImageView) convertView;
        }

        squaredView.setBackgroundResource(R.drawable.gallery_image_selector);
        
        // Get the image URL for the current position.
        String url = getItem(position);
        
        // Set a border for the view if it is in selected state
        boolean isSelected = mFolderFragment.getSelectedItemsList().contains(position);
        if (isSelected) {
            squaredView.setSelected(true);
            squaredView.setBackgroundResource(R.drawable.gallery_image_border_selected);
        } else {
            squaredView.setSelected(false);
        }
        
        // Trigger the download of the URL asynchronously into the image view.
       Picasso.with(mContext)
            .load(new File(url)) 
            .centerCrop()
            .placeholder(R.color.app_background)
            .error(R.color.image_error)
            .fit()
            .into(squaredView);
        //squaredView.setImageBitmap(mUtils.getThumbnailBitmap(url, 128, 128));
        return squaredView;
    }
}
