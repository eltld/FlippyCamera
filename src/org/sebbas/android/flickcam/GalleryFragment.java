package org.sebbas.android.flickcam;

import java.io.File;
import java.util.ArrayList;

import org.sebbas.android.adapter.GridViewImageAdapter;
import org.sebbas.android.helper.AppConstant;
import org.sebbas.android.helper.DeviceInfo;
import org.sebbas.android.helper.Utils;
import org.sebbas.android.views.DrawInsetsFrameLayout;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;

public class GalleryFragment extends Fragment {

    public static final String TAG = "gallery_fragment";
    
    private Utils mUtils;
    private ArrayList<String> mImagePaths = new ArrayList<String>();
    private GridViewImageAdapter mAdapter;
    private GridView mGridView;
    private int mColumnWidth;
    
    private Context mContext;
    private ImageView expandedImageView;
    private FrameLayout mFrameLayout;
    private FrameLayout mGridLayout;
    private ProgressBar mLoadingImageSpinner;
    private BitmapLoader mBitmapLoader;
    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    private boolean mIsLoadingBitmap;
    
    // Static factory method that returns a new fragment instance to the client
    public static GalleryFragment newInstance() {
        GalleryFragment gf = new GalleryFragment();
        return gf;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getActivity();
        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    
        mFrameLayout = (FrameLayout) inflater.inflate(R.layout.gallery_grid_view, container, false);
        //mGridLayout = (FrameLayout) mFrameLayout.findViewById(R.id.gridview_holder);
        mGridView = (GridView) mFrameLayout.findViewById(R.id.grid_view);
        expandedImageView = (ImageView) mFrameLayout.findViewById(R.id.expanded_image);
        mLoadingImageSpinner = (ProgressBar) mFrameLayout.findViewById(R.id.loading_image_spinner);
        mUtils = new Utils(this.getActivity());
        mDrawInsetsFrameLayout = (DrawInsetsFrameLayout) mFrameLayout.findViewById(R.id.draw_insets_framelayout);
        
        setupGridView();
        return mFrameLayout;
    }
    
    public void setupGridView() {
        initializeGridLayout();
        setGridViewAdapter();
        setGridViewClickListener();
    }

    private void setGridViewClickListener() {
        mGridView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position,
                    long id) {
                /*Intent i = new Intent(GalleryFragment.this.getActivity(), FullScreenViewActivity.class);
                i.putExtra("position", position);
                System.out.println("Clicked on position " + position);
                GalleryFragment.this.getActivity().startActivity(i);*/
                
                if (!mIsLoadingBitmap) {
                    mIsLoadingBitmap = true;
                    mBitmapLoader = new BitmapLoader();
                    mBitmapLoader.execute(position);
                } else {
                    mLoadingImageSpinner.setVisibility(View.INVISIBLE);
                    mIsLoadingBitmap = false;
                    mBitmapLoader.cancel(true);
                }
            }
        });
    }

    private void setGridViewAdapter() {
        // loading all image paths from SD card
        mImagePaths = mUtils.getFilePaths();
 
        // Gridview adapter
        mAdapter = new GridViewImageAdapter(this.getActivity(), mImagePaths, mColumnWidth);
 
        // setting grid view adapter
        mGridView.setAdapter(mAdapter);
    }
    
    private void initializeGridLayout() {
        Log.d(TAG, "INITIALIZE GRIDLAYOUT");
        Resources r = getResources();
        final float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, AppConstant.GRID_PADDING, r.getDisplayMetrics());
 
        mColumnWidth = (int) ((mUtils.getScreenWidth() - ((AppConstant.NUM_OF_COLUMNS + 1) * padding)) / AppConstant.NUM_OF_COLUMNS);
 
        mGridView.setNumColumns(AppConstant.NUM_OF_COLUMNS);
        mGridView.setColumnWidth(mColumnWidth);
        mGridView.setStretchMode(GridView.NO_STRETCH);
        mGridView.setHorizontalSpacing((int) padding);
        mGridView.setVerticalSpacing((int) padding);
        mGridView.setPadding((int) padding, 0, (int) padding, 0);
        
        mDrawInsetsFrameLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {
            @Override
            public void onInsetsChanged(Rect insets) {
                // Update the padding
                mGridView.setPadding((int) padding, insets.top, (int) padding, insets.bottom);
            }
        });
    }
    
    private Animator mCurrentAnimator;
    private int mShortAnimationDuration;
    
    @SuppressLint("NewApi")
    private void zoomImageFromThumb(final View thumbView, Bitmap bitmap) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        // Load the high-resolution "zoomed-in" image.
        expandedImageView.setImageBitmap(bitmap);

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBounds);
        mFrameLayout.getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        ViewHelper.setAlpha(thumbView, 0f);
        expandedImageView.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        //expandedImageView.setPivotX(0f);
        //expandedImageView.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
            .play(ObjectAnimator.ofFloat(expandedImageView, "rotationX", startBounds.left, finalBounds.left))
            .with(ObjectAnimator.ofFloat(expandedImageView, "rotationY", startBounds.top, finalBounds.top))
            .with(ObjectAnimator.ofFloat(expandedImageView, "scaleX", startScale, 1f))
            .with(ObjectAnimator.ofFloat(expandedImageView, "scaleY", startScale, 1f));
        
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        final float startScaleFinal = startScale;
        expandedImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCurrentAnimator != null) {
                    mCurrentAnimator.cancel();
                }

                // Animate the four positioning/sizing properties in parallel,
                // back to their original values.
                AnimatorSet set = new AnimatorSet();
                set.play(ObjectAnimator
                            .ofFloat(expandedImageView, "rotationX", startBounds.left))
                            .with(ObjectAnimator.ofFloat(expandedImageView, "rotationY",startBounds.top))
                            .with(ObjectAnimator.ofFloat(expandedImageView, "scaleX", startScaleFinal))
                            .with(ObjectAnimator.ofFloat(expandedImageView, "scaleY", startScaleFinal));

                set.setDuration(mShortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ViewHelper.setAlpha(thumbView, 1f);
                        expandedImageView.setVisibility(View.GONE);
                        mCurrentAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        ViewHelper.setAlpha(thumbView, 1f);
                        expandedImageView.setVisibility(View.GONE);
                        mCurrentAnimator = null;
                    }
                });
                set.start();
                mCurrentAnimator = set;
            }
        });
    }
    
    private void zoomImageWithoutAnimation(final Bitmap bitmap) {
        expandedImageView.setVisibility(View.VISIBLE);
        expandedImageView.setImageBitmap(bitmap);
        expandedImageView.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                expandedImageView.setImageBitmap(null);
                expandedImageView.destroyDrawingCache();
                bitmap.recycle();
                expandedImageView.setVisibility(View.INVISIBLE);
            }
        });
    }
    
    private class BitmapLoader extends AsyncTask<Integer, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingImageSpinner.setVisibility(View.VISIBLE);
        }

        @Override
        protected Bitmap doInBackground(Integer... params) {
            int position = params[0];
            Bitmap myBitmap = null;
            File imgFile = new  File(mImagePaths.get(position));
            if(imgFile.exists()) {
            
                DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
                myBitmap = decodeSampledBitmapFromResource(imgFile, metrics.widthPixels, metrics.heightPixels);

            }
            return myBitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            mLoadingImageSpinner.setVisibility(View.INVISIBLE);
            if (DeviceInfo.supportsSDK(11)) {
                zoomImageFromThumb(new View(mContext), bitmap);
            } else {
                zoomImageWithoutAnimation(bitmap);
            }
            mIsLoadingBitmap = false;
        }

    }
    
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
    
    public static Bitmap decodeSampledBitmapFromResource(File file, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }
}
