package org.sebbas.android.adapter;

import static android.widget.ImageView.ScaleType.CENTER_CROP;

import java.io.File;
import java.util.ArrayList;

import org.sebbas.android.flickcam.R;
import org.sebbas.android.views.SquaredImageView;

import com.squareup.picasso.Picasso;
 
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
 
public class GridViewImageAdapter extends BaseAdapter {
 
    private Context mContext;
    private ArrayList<String> mFilePaths = new ArrayList<String>();
 
    public GridViewImageAdapter(Activity activity, ArrayList<String> filePaths,
            int imageWidth) {
        mContext = activity;
        mFilePaths = filePaths;
    }
 
    @Override
    public int getCount() {
        return mFilePaths.size();
    }
 
    @Override
    public String getItem(int position) {
        return mFilePaths.get(position);
    }
 
    @Override
    public long getItemId(int position) {
        return position;
    }
 
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SquaredImageView view = (SquaredImageView) convertView;
        if (view == null) {
            view = new SquaredImageView(mContext);
            view.setScaleType(CENTER_CROP);
        }

        // Get the image URL for the current position.
        String url = getItem(position);

        // Trigger the download of the URL asynchronously into the image view.
        Picasso.with(mContext) //
            .load(new File(url)) //
            .placeholder(R.drawable.ic_action_camera) //
            .error(R.drawable.ic_action_camera) //
            .fit() //
            .into(view);
        return view;
    }
}
