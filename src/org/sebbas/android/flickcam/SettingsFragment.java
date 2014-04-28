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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import android.widget.Toast;
import android.support.v7.app.ActionBarActivity;

public class SettingsFragment extends Fragment {

    public static final String TAG = "settings_fragment";
    private static final String SELECT_IMAGES = "Select images";
    private static volatile ArrayList<Integer> mSelectedItemsList = new ArrayList<Integer>();
    
    private Utils mUtils;
    private GridViewImageAdapter mAdapter;
    private GridView mGridView;
    private int mColumnWidth;
    
    private Context mContext;
    private ImageView expandedImageView;
    private FrameLayout mFrameLayout;
    private ProgressBar mLoadingImageSpinner;
    private ArrayList<String> mImagePaths = new ArrayList<String>();
    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    private boolean mIsLoadingBitmap;
    private Animator mCurrentAnimator;
    private int mShortAnimationDuration;
    private ActionMode mActionMode;
    
    // Static factory method that returns a new fragment instance to the client
    public static SettingsFragment newInstance() {
        SettingsFragment gf = new SettingsFragment();
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
        mGridView = (GridView) mFrameLayout.findViewById(R.id.grid_view);
        expandedImageView = (ImageView) mFrameLayout.findViewById(R.id.expanded_image);
        mLoadingImageSpinner = (ProgressBar) mFrameLayout.findViewById(R.id.loading_image_spinner);
        mUtils = new Utils(this.getActivity());
        mDrawInsetsFrameLayout = (DrawInsetsFrameLayout) mFrameLayout.findViewById(R.id.draw_insets_framelayout);
        
        return mFrameLayout;
    }
    
}
