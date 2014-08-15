package org.sebbas.android.adapter;

import org.sebbas.android.flippycamera.FolderFragment;
import org.sebbas.android.flippycamera.MainActivity;

import uk.co.senab.photoview.PhotoView;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

public class ImagePagerAdapter extends PagerAdapter {

    private Context mContext;
    private int mFolderPosition;
    
    public ImagePagerAdapter(FolderFragment folderFragment, int folderPosition) {
        mContext = folderFragment.getActivity();
        mFolderPosition = folderPosition;
    }
    @Override
    public int getCount() {
        return ((MainActivity) mContext).getImagePathsAt(mFolderPosition).size();
    }

    @Override
    public View instantiateItem(ViewGroup container, int position) {
        PhotoView photoView = new PhotoView(container.getContext());
        
        String imagePath = ((MainActivity) mContext).getImagePathsAt(mFolderPosition).get(position);
        Drawable imageDrawable = Drawable.createFromPath(imagePath);
        photoView.setImageDrawable(imageDrawable);

        // Now just add PhotoView to ViewPager and return it
        container.addView(photoView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        return photoView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }
}
