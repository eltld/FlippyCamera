package org.sebbas.android.flickcam;

import java.util.ArrayList;

import org.sebbas.android.adapter.FolderViewImageAdapter;
import org.sebbas.android.helper.AppConstant;
import org.sebbas.android.helper.Utils;
import org.sebbas.android.interfaces.AdapterCallback;
import org.sebbas.android.views.DrawInsetsFrameLayout;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.support.v7.app.ActionBarActivity;

public class FolderFragment extends Fragment implements AdapterCallback {

    public static final String TAG = "gallery_fragment";
    private static final String SELECT_FOLDERS = "Select folders";
    private volatile ArrayList<Integer> mSelectedItemsList = new ArrayList<Integer>();
    
    private Utils mUtils;
    private FolderViewImageAdapter mAdapter;
    private GridView mGridView;
    private int mColumnWidth;
    
    private Context mContext;
    private MainFragment mMainFragment;
    private FrameLayout mFrameLayout;
    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    private ActionMode mActionMode;
    private boolean mHiddenFolders;
    
    // Static factory method that returns a new fragment instance to the client
    public static FolderFragment newInstance(boolean hiddenFolders) {
        FolderFragment folderFragment = new FolderFragment();
        
        Bundle args = new Bundle();
        args.putBoolean("hiddenFolders", hiddenFolders);
        folderFragment.setArguments(args);
        
        return folderFragment;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getActivity();
        mMainFragment = (MainFragment) this.getActivity();
        mHiddenFolders = this.getArguments().getBoolean("hiddenFolders");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    
        mFrameLayout = (FrameLayout) inflater.inflate(R.layout.gallery_folders, container, false);
        mGridView = (GridView) mFrameLayout.findViewById(R.id.folder_grid_view);
        mUtils = new Utils(this.getActivity());
        mDrawInsetsFrameLayout = (DrawInsetsFrameLayout) mFrameLayout.findViewById(R.id.draw_insets_framelayout);
        
        // Turn off the "up" back navigation option
        mMainFragment.getSupportActionBar().setHomeButtonEnabled(false);
        mMainFragment.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        
        setupGridView();
        return mFrameLayout;
    }
    
    private void setupGridView() {
        initializeGridLayout();
        setGridViewAdapter(mHiddenFolders);
        setGridViewClickListener();
    }

    private void setGridViewClickListener() {
    
        
        mGridView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position,
                    long id) {
            
                // Only enlarge the image if we are not in action mode
                if (mActionMode == null) {
                	FragmentTransaction transaction = mMainFragment.getSupportFragmentManager().beginTransaction();
                	transaction.replace(R.id.root_frame, GalleryFragment.newInstance((ArrayList<String>) mAdapter.getImagePaths().get(position)));
                	transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                	transaction.addToBackStack(null);
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
        // If the item was already added then the item is in selected state. We remove the item since the item is deselected now
        if (mSelectedItemsList.contains(itemPosition)) {
            mSelectedItemsList.remove(Integer.valueOf(itemPosition));
        // else we add it to our list since it just got selected
        } else {
            mSelectedItemsList.add(itemPosition);
        }
        refreshAdapter();
        
        // Show the number of selected items in the subtitle of the action mode
        mActionMode.setSubtitle(mSelectedItemsList.size() + "/" + mAdapter.getCount());
        
        // Remove action mode bar if no image is selected
        if (mSelectedItemsList.size() == 0) {
            finishActionMode();
        }
    }

    private void setGridViewAdapter(boolean hiddenFolders) {
        // Grid View adapter
        if (mAdapter == null) mAdapter = new FolderViewImageAdapter(this, hiddenFolders);
 
        // Setting grid view adapter
        mGridView.setAdapter(mAdapter);
    }
    
    private void initializeGridLayout() {
        Log.d(TAG, "INITIALIZE GRIDLAYOUT");
        Resources r = getResources();
        final float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, AppConstant.GRID_PADDING, r.getDisplayMetrics());
 
        mColumnWidth = (int) ((mUtils.getScreenWidth() - ((AppConstant.NUM_OF_COLUMNS_FOLDERVIEW + 1) * padding)) / AppConstant.NUM_OF_COLUMNS_FOLDERVIEW);
 
        mGridView.setNumColumns(AppConstant.NUM_OF_COLUMNS_FOLDERVIEW);
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
    
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
    
        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.actionmode, menu);
            mode.setTitle(SELECT_FOLDERS);
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
                	MediaDeleterThread deleter = new MediaDeleterThread(mContext, new ArrayList<Integer>(mSelectedItemsList), mAdapter, 0);
                    deleter.execute();
                    reloadAdapterContent(mHiddenFolders);
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mSelectedItemsList.clear();
            mActionMode = null;
            
            refreshAdapter();
        }
    };
    
    public ActionMode getActionMode() {
        return mActionMode;
    }
    
    public ArrayList<Integer> getSelectedItemsList() {
    	return mSelectedItemsList;
    }
    
    public void finishActionMode() {
        mActionMode.finish();
    }

    @Override
    public void refreshAdapter() {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void reloadAdapterContent(boolean hiddenFolders) {
        mAdapter.loadAdapterContent(hiddenFolders);
    }
}
