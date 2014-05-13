package org.sebbas.android.flickcam;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.sebbas.android.adapter.GridViewImageAdapter;
import org.sebbas.android.helper.AppConstant;
import org.sebbas.android.helper.DeviceInfo;
import org.sebbas.android.helper.Utils;
import org.sebbas.android.interfaces.AdapterCallback;
import org.sebbas.android.views.DrawInsetsFrameLayout;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.view.ActionMode;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class GalleryFragment extends Fragment implements AdapterCallback<String> {

    public static final String TAG = "gallery_fragment";
    private static final String SELECT_IMAGES = "Select images";
    private volatile ArrayList<Integer> mSelectedItemsList = new ArrayList<Integer>();
    
    private Utils mUtils;
    private GridViewImageAdapter mAdapter;
    private GridView mGridView;
    private int mColumnWidth;
    
    private Context mContext;
    private MainFragmentActivity mMainFragment;
    private FullScreenImageSliderFragment mFullScreenImageSliderFragment;
    
    private ImageView expandedImageView;
    private FrameLayout mFrameLayout;
    private ProgressBar mLoadingImageSpinner;
    private ArrayList<String> mImagePaths = new ArrayList<String>();
    private BitmapLoader mBitmapLoader;
    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    private boolean mIsLoadingBitmap;
    private Animator mCurrentAnimator;
    private int mShortAnimationDuration;
    private ActionMode mActionMode;
    private int mFolderPosition;
    
    // Static factory method that returns a new fragment instance to the client
    public static GalleryFragment newInstance(int position) {
        GalleryFragment galleryFragment = new GalleryFragment();
        
        Bundle args = new Bundle();
        args.putInt("folderPosition", position);
        galleryFragment.setArguments(args);
        
        return galleryFragment;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getActivity();
        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        mMainFragment = (MainFragmentActivity) this.getActivity();
        
        mFolderPosition = this.getArguments().getInt("folderPosition");
        mImagePaths = (ArrayList<String>) mMainFragment.getImagePaths().get(mFolderPosition);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    
        mFrameLayout = (FrameLayout) inflater.inflate(R.layout.gallery_grid_view, container, false);
        mGridView = (GridView) mFrameLayout.findViewById(R.id.grid_view);
        expandedImageView = (ImageView) mFrameLayout.findViewById(R.id.expanded_image);
        mLoadingImageSpinner = (ProgressBar) mFrameLayout.findViewById(R.id.loading_image_spinner);
        mUtils = new Utils(this.getActivity());
        mDrawInsetsFrameLayout = (DrawInsetsFrameLayout) mFrameLayout.findViewById(R.id.draw_insets_framelayout);
        
        // Turn on the "up" back navigation option
        mMainFragment.getSupportActionBar().setHomeButtonEnabled(true);
        mMainFragment.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        setupGridView();
        setupActionBarTitle();
        return mFrameLayout;
    }

    @Override
    public void onResume() {
        super.onResume();
        setupActionBarTitle();
    }

    @Override
    public void onPause() {
        super.onPause();
        // The fragment is getting out of focus so we pop it from the stack and reset the up navigation
        mMainFragment.getSupportFragmentManager().popBackStack();
        mMainFragment.getSupportActionBar().setHomeButtonEnabled(false);
        mMainFragment.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }
    
    public void setupActionBarTitle() {
        mMainFragment.getSupportActionBar().setTitle(mUtils.getFolderName(mImagePaths) + " (" + mImagePaths.size() + ")");
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
            
                // Only enlarge the image if we are not in action mode
                if (mActionMode == null) {
                    /*if (!mIsLoadingBitmap) {
                        mIsLoadingBitmap = true;
                        mBitmapLoader = new BitmapLoader();
                        mBitmapLoader.execute(position);
                    } else {
                        mLoadingImageSpinner.setVisibility(View.INVISIBLE);
                        mIsLoadingBitmap = false;
                        mBitmapLoader.cancel(true);
                    }*/
                    
                    FragmentManager manager = mMainFragment.getSupportFragmentManager(); 
                    FragmentTransaction transaction = manager.beginTransaction();
                    mFullScreenImageSliderFragment = FullScreenImageSliderFragment.newInstance(mImagePaths, position);
                    transaction.replace(R.id.container, mFullScreenImageSliderFragment);
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    transaction.addToBackStack("fullscreen_fragment");
                    transaction.commit();
                    
                } else {
                    // Keep track of which items are selected. Then notify the adapter
                    manageSelectedItemsList(position); 
                    
                }
            }
        });
        
        mGridView.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                    int position, long id) {
                if (mActionMode != null) {
                    return false;
                }
                // Start the CAB using the ActionMode.Callback defined above
                mActionMode = mMainFragment.startSupportActionMode(mActionModeCallback);
                
                // Keep track of which items are selected. Then notify the adapter
                manageSelectedItemsList(position); 
                
                return true;
            }
            
        });
    }
    
    private void manageSelectedItemsList(int itemPosition) {
        // If the item was already added then the item is in selected state. We remove the item since the item is not selected now
        if (mSelectedItemsList.contains(itemPosition)) {
            mSelectedItemsList.remove(Integer.valueOf(itemPosition));
        // else we add it to our list since it just got selected
        } else {
            mSelectedItemsList.add(itemPosition);
        }
        refreshAdapter();
        
        // Show the number of selected items in the subtitle of the action mode
        mActionMode.setSubtitle(mSelectedItemsList.size() + "/" + mAdapter.getCount());
        Log.d(TAG, "selected items are: " + mSelectedItemsList);
        // Remove action mode bar if no image is selected
        if (mSelectedItemsList.size() == 0) {
            finishActionMode();
        }
    }

    private void setGridViewAdapter() {
        // Gridview adapter
        mAdapter = new GridViewImageAdapter(this, mImagePaths);
 
        // setting grid view adapter
        mGridView.setAdapter(mAdapter);
    }
    
    private void initializeGridLayout() {
        Log.d(TAG, "INITIALIZE GRIDLAYOUT");
        Resources r = getResources();
        final float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, AppConstant.GRID_PADDING, r.getDisplayMetrics());
 
        mColumnWidth = (int) ((mUtils.getScreenWidth() - ((AppConstant.NUM_OF_COLUMNS_GALLERYVIEW + 1) * padding)) / AppConstant.NUM_OF_COLUMNS_GALLERYVIEW);
 
        mGridView.setNumColumns(AppConstant.NUM_OF_COLUMNS_GALLERYVIEW);
        mGridView.setColumnWidth(mColumnWidth);
        mGridView.setStretchMode(GridView.NO_STRETCH);
        mGridView.setHorizontalSpacing((int) padding);
        mGridView.setVerticalSpacing((int) padding);
        mGridView.setPadding((int) padding, 150, (int) padding, 150);
        
        mDrawInsetsFrameLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {
            @Override
            public void onInsetsChanged(Rect insets) {
                // Update the padding
                mGridView.setPadding((int) padding, insets.top, (int) padding, insets.bottom);
            }
        });
    }
    
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
    
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.actionmode_images, menu);
            mode.setTitle(SELECT_IMAGES);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.discard_image:
                    MediaDeleterThread deleter = new MediaDeleterThread(
                            mContext, new ArrayList<Integer>(mSelectedItemsList), GalleryFragment.this, mFolderPosition, 1);
                    deleter.execute();
                    
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mSelectedItemsList.clear(); // Clear list since nothing is supposed to be selected at this point
            refreshAdapter(); // Refresh the adapter so that the pending borders get reset
            mActionMode = null;
        }
    };
    
    public ActionMode getActionMode() {
        return mActionMode;
    }
    
    public void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }
    
    public ArrayList<Integer> getSelectedItemsList() {
        return mSelectedItemsList;
    }
    
    public int getFolderPosition() {
        return mFolderPosition;
    }
    
    public FullScreenImageSliderFragment getFullScreenImageSliderFragment() {
        return mFullScreenImageSliderFragment;
    }

    @Override
    public void refreshAdapter() {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void updateAdapterContent(ArrayList<String> imagePaths) {
        mAdapter.updateImagePaths(imagePaths);
    }
}
