package org.sebbas.android.adapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import org.sebbas.android.flickcam.FullScreenViewActivity;
 
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
 
public class GridViewImageAdapter extends BaseAdapter {
 
    private Activity mActivity;
    private ArrayList<String> mFilePaths = new ArrayList<String>();
    private int mImageWidth;
 
    public GridViewImageAdapter(Activity activity, ArrayList<String> filePaths,
            int imageWidth) {
        this.mActivity = activity;
        this.mFilePaths = filePaths;
        this.mImageWidth = imageWidth;
    }
 
    @Override
    public int getCount() {
        return this.mFilePaths.size();
    }
 
    @Override
    public Object getItem(int position) {
        return this.mFilePaths.get(position);
    }
 
    @Override
    public long getItemId(int position) {
        return position;
    }
 
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(mActivity);
        } else {
            imageView = (ImageView) convertView;
        }
 
        // get screen dimensions
        Bitmap image = decodeFile(mFilePaths.get(position), mImageWidth,
                mImageWidth);
 
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setLayoutParams(new GridView.LayoutParams(mImageWidth,
                mImageWidth));
        imageView.setImageBitmap(image);
  
        return imageView;
    }

    /*
     * Resizing image size
     */
    public static Bitmap decodeFile(String filePath, int WIDTH, int HIGHT) {
        try {
 
            File f = new File(filePath);
 
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);
 
            final int REQUIRED_WIDTH = WIDTH;
            final int REQUIRED_HIGHT = HIGHT;
            int scale = 1;
            while (o.outWidth / scale / 2 >= REQUIRED_WIDTH
                    && o.outHeight / scale / 2 >= REQUIRED_HIGHT)
                scale *= 2;
 
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
 
}
