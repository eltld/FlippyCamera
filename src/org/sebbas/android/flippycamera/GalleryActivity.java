package org.sebbas.android.flippycamera;

import java.util.ArrayList;
import java.util.List;

import org.sebbas.android.adapter.GridViewImageAdapter;
import org.sebbas.android.helper.AppConstant;
import org.sebbas.android.helper.Utils;
import org.sebbas.android.interfaces.AdapterCallback;
import org.sebbas.android.threads.MediaDeleterThread;
import org.sebbas.android.views.DrawInsetsFrameLayout;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class GalleryActivity extends ActionBarActivity implements AdapterCallback<String> {

    private static final String TAG = "gallery_activity";
    
    private Utils mUtils;
    private int mFolderPosition;
    private ArrayList<List<String>> mImagePaths;
    private GridViewImageAdapter mAdapter;
    private int mColumnWidth;
    private volatile ArrayList<Integer> mSelectedItemsList = new ArrayList<Integer>();
    private ActionMode mActionMode;
    
    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    private GridView mGridView;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery_grid_view);
        
        mUtils = new Utils(this);
        mFolderPosition = this.getIntent().getIntExtra("folderPosition", 0);
        mImagePaths = (ArrayList<List<String>>) this.getIntent().getSerializableExtra("imagePaths");
        
        // Variables for the UI
        mDrawInsetsFrameLayout = (DrawInsetsFrameLayout) findViewById(R.id.draw_insets_framelayout);
        mGridView = (GridView) findViewById(R.id.grid_view);
        
        this.getSupportActionBar().setHomeButtonEnabled(true);
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        setupGridView();
        setupActionBarTitle();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gallery, menu);
        //mSpinnerIcon = menu.findItem(R.id.spinner_icon);
        //MenuItemCompat.setActionView(mSpinnerIcon, R.layout.actionbar_indeterminate_progress);
        //setSpinnerVisibility();
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default: 
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handleNavigationBack();
    }

    @Override
    public boolean onSupportNavigateUp() {
        super.onSupportNavigateUp();
        handleNavigationBack();
        return true;
    }
    
    public void startReturnIntent() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", mImagePaths);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    public void setupGridView() {
        initializeGridLayout();
        setGridViewAdapter();
        setGridViewClickListener();
    }
    
    private void handleNavigationBack() {
        FragmentManager fm  = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() == 0) {
            startReturnIntent();
        } else {
            fm.popBackStack();
        }
    }
    
    private void initializeGridLayout() {
        final float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, AppConstant.GRID_PADDING, getResources().getDisplayMetrics());
 
        mColumnWidth = (int) ((mUtils.getScreenWidth() - ((AppConstant.NUM_OF_COLUMNS_GALLERYVIEW + 1) * padding)) / AppConstant.NUM_OF_COLUMNS_GALLERYVIEW);
 
        mGridView.setNumColumns(AppConstant.NUM_OF_COLUMNS_GALLERYVIEW);
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
    
    private void setGridViewAdapter() {
        // Gridview adapter
        ArrayList<String> imagePaths = (ArrayList<String>) mImagePaths.get(mFolderPosition);
        mAdapter = new GridViewImageAdapter(this, imagePaths);
 
        // setting grid view adapter
        mGridView.setAdapter(mAdapter);
    }
    
    private void setGridViewClickListener() {
        
        mGridView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position,
                    long id) {
            
                // Only enlarge the image if we are not in action mode
                if (mActionMode == null) {
                    // TODO --> Start single image view with intent
                    
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
                mActionMode = GalleryActivity.this.startSupportActionMode(mActionModeCallback);
                
                // Keep track of which items are selected. Then notify the adapter
                manageSelectedItemsList(position); 
                
                return true;
            }
            
        });
    }
    
    public ArrayList<Integer> getSelectedItemsList() {
        return mSelectedItemsList;
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
    
    public void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }
    
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.actionmode_images, menu);
            mode.setTitle(R.string.select_images);
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
                            GalleryActivity.this, new ArrayList<Integer>(mSelectedItemsList), GalleryActivity.this.getImagePaths(), mFolderPosition, 1);
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
            mActionMode = null;
            refreshAdapter();
        }
    };

    @Override
    public void updateImagePaths(ArrayList<List<String>> imagePaths) {
        mImagePaths = imagePaths;
    }
    
    @Override
    public void refreshAdapter() {
        mAdapter.notifyDataSetChanged();
    }
    
    @Override
    public void updateAdapterContent(ArrayList<String> imagePaths) {
        mAdapter.updateImagePaths(imagePaths);
    }
    
    public ArrayList<List <String>> getImagePaths() {
        return mImagePaths;
    }

    public void setupActionBarTitle() {
        List<String> imageList = mImagePaths.get(mFolderPosition);
        this.getSupportActionBar().setTitle(mUtils.getFolderName(imageList) + " (" + imageList.size() + ")");
    }
    
    public DrawInsetsFrameLayout getDrawInsetsFrameLayout() {
        return mDrawInsetsFrameLayout;
    }
}
