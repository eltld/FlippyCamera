package org.sebbas.android.adapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.sebbas.android.flippycamera.FolderFragment;
import org.sebbas.android.flippycamera.R;
import org.sebbas.android.helper.AppConstant;
import org.sebbas.android.helper.Utils;

import com.squareup.picasso.Picasso;
 
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
 
public class FolderViewImageAdapter extends BaseAdapter {
 
    private Context mContext;
    private ArrayList<List<String>> mImagePaths = new ArrayList<List<String>>();
    private FolderFragment mFolderFragment;
    private Utils mUtils;
    private static final int[] previewIds = {R.id.folder_image_1, R.id.folder_image_2, R.id.folder_image_3, R.id.folder_image_4};
 
    public FolderViewImageAdapter(FolderFragment folderFragment) {
        mContext = folderFragment.getActivity();
        mFolderFragment = folderFragment;
        mUtils = new Utils(mContext);
    }
 
    @Override
    public int getCount() {
        return mImagePaths.size();
    }
 
    @Override
    public List<String> getItem(int position) {
        return mImagePaths.get(position);
    }
 
    @Override
    public long getItemId(int position) {
        return position;
    }
 
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View folderView = null;
        if (convertView == null) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            folderView = inflater.inflate(R.layout.folder_view, null);
        } else {
            folderView = convertView;
        }
        folderView.setBackgroundResource(R.drawable.gallery_image_selector);

        // Get the image URL for the current position.
        List<String> imagePaths = getItem(position);
        
        // Set a border for the view if it is in selected state
        boolean isSelected = mFolderFragment.getSelectedItemsList().contains(position);
        if (isSelected) {
            folderView.setSelected(true);
            folderView.setBackgroundResource(R.drawable.gallery_image_border_selected);
        } else {
            folderView.setSelected(false);
        }

        for (int i = 0; i < 4; i++) {
            if (i < imagePaths.size()) {
                loadImageIntoView(i, folderView, imagePaths.get(i));
            } else {
                hideImageView(i, folderView);
            }
        }
        
        // Setup TextView for folder. Shows the folder name
        TextView folderNameView = (TextView)folderView.findViewById(R.id.folder_name);
        String folderName = mUtils.getFolderName(imagePaths);
        // Only if there is a valid string for the folder name, we set it. Otherwise we use the default name for folders
        if (folderName.equals("")) {
            folderNameView.setText(AppConstant.DEFAULT_FOLDER_NAME);
        } else {
            folderNameView.setText(folderName);
        }
        
        // Setup TextView for folder. Shows the folder name
        TextView folderSizeView = (TextView)folderView.findViewById(R.id.folder_size);
        folderSizeView.setText("" + imagePaths.size());
        
        return folderView;
    }
    
    private void loadImageIntoView(int viewPosition, View parentView, String imagePath) {
        ImageView previewImage = (ImageView)parentView.findViewById(previewIds[viewPosition]);
        previewImage.setVisibility(View.VISIBLE);
        Picasso.with(mContext)
            .load(new File(imagePath)) 
            .noFade()
            .centerCrop()
            .placeholder(R.color.image_placeholder)
            .error(R.color.image_error)
            .fit() //
            .into(previewImage);
        
    }
    
    private void hideImageView(int viewPosition, View parentView) {
        ImageView previewImage = (ImageView)parentView.findViewById(previewIds[viewPosition]);
        previewImage.setVisibility(View.GONE);
    }
    
    public void updateImagePaths(ArrayList<List <String>> imagePaths) {
        mImagePaths = imagePaths;
    }
}
