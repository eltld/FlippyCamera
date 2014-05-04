package org.sebbas.android.adapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.sebbas.android.flickcam.FolderFragment;
import org.sebbas.android.flickcam.MainFragmentActivity;
import org.sebbas.android.flickcam.R;
import org.sebbas.android.helper.Utils;

import com.squareup.picasso.Picasso;
 
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
 
public class FolderViewImageAdapter extends BaseAdapter {
 
    private Context mContext;
    private ArrayList<List<String>> mImagePaths = new ArrayList<List<String>>();
    private Utils mUtils;
    private MainFragmentActivity mMainFragment;
    private FolderFragment mFolderFragment;
    private static final int[] previewIds = {R.id.folder_image_1, R.id.folder_image_2, R.id.folder_image_3, R.id.folder_image_4};
 
    public FolderViewImageAdapter(FolderFragment folderFragment, boolean alsoHiddenImages) {
        mContext = folderFragment.getActivity();
        mMainFragment = (MainFragmentActivity) folderFragment.getActivity();
        mFolderFragment = folderFragment;
        mUtils = new Utils(mContext);
        
        // Start loading the image paths
        ImagePathLoader loader = new ImagePathLoader();
        loader.execute(alsoHiddenImages);
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
    
    public ArrayList<List<String>> getImagePaths() {
        return mImagePaths;
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
        folderView.setBackgroundResource(R.drawable.square_image_selector);

        // Get the image URL for the current position.
        List<String> imagePaths = getItem(position);
        
        // Set a border for the view if it is in selected state
        boolean isSelected = mFolderFragment.getSelectedItemsList().contains(position);
        if (isSelected) {
            folderView.setSelected(true);
            folderView.setBackgroundResource(R.drawable.image_border);
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
        TextView folderName = (TextView)folderView.findViewById(R.id.folder_name);
        folderName.setText((new File(imagePaths.get(0)).getParentFile().getName()));
        
        // Setup TextView for folder. Shows the folder name
        TextView folderSize = (TextView)folderView.findViewById(R.id.folder_size);
        folderSize.setText("" + imagePaths.size());
        
        return folderView;
    }
    
    private void loadImageIntoView(int viewPosition, View parentView, String imagePath) {
        ImageView previewImage = (ImageView)parentView.findViewById(previewIds[viewPosition]);
        previewImage.setVisibility(View.VISIBLE);
        Picasso.with(mContext)
            .load(new File(imagePath)) 
            .noFade()
            .centerCrop()
            .placeholder(R.drawable.ic_action_picture)
            .error(R.drawable.ic_action_picture)
            .fit() //
            .into(previewImage);
        
    }
    
    private void hideImageView(int viewPosition, View parentView) {
        // Set the background color of the image view to the same color as the app background -> makes it invisible 
        ImageView previewImage = (ImageView)parentView.findViewById(previewIds[viewPosition]);
        previewImage.setVisibility(View.GONE);
    }
    
    public void loadAdapterContent(boolean alsoHiddenImages) {
        ImagePathLoader loader = new ImagePathLoader();
        loader.execute(alsoHiddenImages);
    }
    
    private class ImagePathLoader extends AsyncTask<Boolean, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mMainFragment.setSpinnerIconInProgress(true);
        }

        @Override
        protected Void doInBackground(Boolean... params) {
            mImagePaths = mUtils.getImagePaths(params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            notifyDataSetChanged();
            mMainFragment.setSpinnerIconInProgress(false);
        }
        
    }
}
