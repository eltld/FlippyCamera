package org.sebbas.android.flippycamera;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.sebbas.android.adapter.FolderViewImageAdapter;
import org.sebbas.android.adapter.GridViewImageAdapter2;
import org.sebbas.android.adapter.ImagePagerAdapter;
import org.sebbas.android.helper.AppConstant;
import org.sebbas.android.helper.Utils;
import org.sebbas.android.interfaces.AdapterCallback;
import org.sebbas.android.threads.MediaDeleterThread;
import org.sebbas.android.views.DrawInsetsFrameLayout;

import com.nhaarman.listviewanimations.swinginadapters.prepared.AlphaInAnimationAdapter;
import com.nhaarman.listviewanimations.swinginadapters.prepared.ScaleInAnimationAdapter;
import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;
import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingRightInAnimationAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
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
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;

public class FolderFragment extends Fragment implements AdapterCallback<List <String>> {

    public static final String TAG = "folder_fragment";
    public static final int FOLDER_MODE = 0;
    public static final int GALLERY_MODE = 1;
    public static final int IMAGE_MODE = 2;
    
    private volatile ArrayList<Integer> mSelectedItemsList = new ArrayList<Integer>();
    
    private Utils mUtils;
    private BaseAdapter mAdapter;
    private GridView mGridView;
    
    private Context mContext;
    private MainActivity mMainActivity;
    private FrameLayout mFrameLayout;
    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    private ActionMode mActionMode;
    private int mAdapterMode;
    private int mFolderPosition;
    private ViewPager mImageViewPager;
    
    // Static factory method that returns a new fragment instance to the client
    public static FolderFragment newInstance() {
        FolderFragment folderFragment = new FolderFragment();
        
        return folderFragment;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getActivity();
        mMainActivity = (MainActivity) this.getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    
        mFrameLayout = (FrameLayout) inflater.inflate(R.layout.gallery_folders, container, false);
        mGridView = (GridView) mFrameLayout.findViewById(R.id.folder_grid_view);
        mImageViewPager = (ViewPager) mFrameLayout.findViewById(R.id.photoview_pager);
        
        mUtils = new Utils(this.getActivity());
        mDrawInsetsFrameLayout = (DrawInsetsFrameLayout) mFrameLayout.findViewById(R.id.draw_insets_framelayout);
        mDrawInsetsFrameLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {
            @Override
            public void onInsetsChanged(Rect insets) {
                // Update the padding
                mGridView.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            }
        });
        
        mMainActivity.reloadFolderPaths(); // Fill the adapter with content
        mAdapterMode = FOLDER_MODE;
        setupGridView();
        return mFrameLayout;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        setupActionBarTitle();
    }
    
    private void setupActionBarTitle() {
        mMainActivity.getSupportActionBar().setTitle(R.string.app_name);
    }
    
    private void setupViewPager() {
    	handleVisibility();
    	mImageViewPager.setAdapter(new ImagePagerAdapter(this, mFolderPosition));
    }

    private void setupGridView() {
    	handleVisibility();
        initializeGridLayout();
        setGridViewAdapter();
        setGridViewClickListener();
    }
    
    private void handleVisibility() {
    	if (mAdapterMode == IMAGE_MODE) {
    		mImageViewPager.setVisibility(View.VISIBLE);
    		mGridView.setVisibility(View.INVISIBLE);
    	} else {
    		mImageViewPager.setVisibility(View.INVISIBLE);
    		mGridView.setVisibility(View.VISIBLE);
    	}
    }
    
    private void initializeGridLayout() {
        final int columnWidth;
        final float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, AppConstant.GRID_PADDING, getResources().getDisplayMetrics());
        
        if (mAdapterMode == FOLDER_MODE) {
            columnWidth = (int) ((mUtils.getScreenWidth() - ((AppConstant.NUM_OF_COLUMNS_FOLDERVIEW + 1) * padding)) / AppConstant.NUM_OF_COLUMNS_FOLDERVIEW);
            mGridView.setColumnWidth(columnWidth);
            mGridView.setNumColumns(AppConstant.NUM_OF_COLUMNS_FOLDERVIEW);
        } else if (mAdapterMode == GALLERY_MODE) {
            columnWidth = (int) ((mUtils.getScreenWidth() - ((AppConstant.NUM_OF_COLUMNS_GALLERYVIEW + 1) * padding)) / AppConstant.NUM_OF_COLUMNS_GALLERYVIEW);
            mGridView.setColumnWidth(columnWidth);
            mGridView.setNumColumns(AppConstant.NUM_OF_COLUMNS_GALLERYVIEW);
        } else if (mAdapterMode == IMAGE_MODE) {
            // TODO
        }
        
        mGridView.setStretchMode(GridView.NO_STRETCH);
        mGridView.setHorizontalSpacing((int) padding);
        mGridView.setVerticalSpacing((int) padding);
        
    }
    
    private void setGridViewAdapter() {
        if (mAdapterMode == FOLDER_MODE) {
        	mAdapter = new FolderViewImageAdapter(this);
        } else if (mAdapterMode == GALLERY_MODE) {
            mAdapter = new GridViewImageAdapter2(this, mFolderPosition);
        } else if (mAdapterMode == IMAGE_MODE) {
            // TODO
        }
        mGridView.setAdapter(mAdapter);
    }
    

    private void setGridViewClickListener() {
    
        mGridView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position,
                    long id) {
            
                // Only enlarge the image if we are not in action mode
                if (mActionMode == null) {
                    
                    /*Intent startGalleryIntent = new Intent(mContext, GalleryActivity.class);
                    startGalleryIntent.putExtra("folderPosition", position);
                    startGalleryIntent.putExtra("imagePaths", getImagePaths());
                    startActivityForResult(startGalleryIntent, 1);*/
                    
                    navigateForward(position);
                    
                } else {
                    
                    // Keep track of which items are selected. Then notify the adapter
                    manageSelectedItemsList(position);
                    // Show and hide certain action mode items
                    manageActionModeItems();
                    
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
                mActionMode = mMainActivity.startSupportActionMode(mActionModeCallback);
                
                // Keep track of which items are selected. Then notify the adapter
                manageSelectedItemsList(position); 
                
                return true;
            }
            
        });
        
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                ArrayList<List<String>> imagePaths = (ArrayList<List<String>>) data.getSerializableExtra("result");
                mMainActivity.updateImagePaths(imagePaths);
                updateAdapterContent(imagePaths);
                refreshAdapter();
            }
        }
    }

    private void manageActionModeItems() {
        // Show and hide the edit item depending on how many items are currently selected
        if (mActionMode != null) {
            MenuItem editItem = mActionMode.getMenu().findItem(R.id.edit_folder);
            Log.d(TAG, "list is: " + mSelectedItemsList);
            if (mSelectedItemsList.size() == 1) {
                editItem.setVisible(true);
            } else {
                editItem.setVisible(false);
            }
        }
    }
    
    private ArrayList<List<String>> getImagePaths() {
        return mMainActivity.getImagePaths();
    }
    
    private ArrayList<String> getImagePathsAt(int position) {
        return (ArrayList<String>) mMainActivity.getImagePaths().get(position);
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
        
        // Remove action mode bar if no image is selected
        if (mSelectedItemsList.size() == 0) {
            finishActionMode();
        }
        
    }
    
    private void selectAll() {
        if (mSelectedItemsList.size() == mAdapter.getCount()) {
            mSelectedItemsList.clear();
        } else {
            mSelectedItemsList.clear();
            for (int i = 0; i < mAdapter.getCount(); i++) {
                mSelectedItemsList.add(i);
            }
        }    
        refreshAdapter();
        mActionMode.setSubtitle(mSelectedItemsList.size() + "/" + mAdapter.getCount());
        manageActionModeItems();
    }

    
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
    
        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.actionmode_folders, menu);
            mode.setTitle(R.string.select_folders);
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
                case R.id.select_all:
                    selectAll(); // Select / deselect all items
                    return true;
                case R.id.discard_folder:
                    MediaDeleterThread deleter = 
                        new MediaDeleterThread(mMainActivity, new ArrayList<Integer>(mSelectedItemsList), mMainActivity.getImagePaths(), 0, 0);
                    deleter.execute();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.edit_folder:
                    startEditFolderName();
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
    
    public ActionMode getActionMode() {
        return mActionMode;
    }
    
    public ArrayList<Integer> getSelectedItemsList() {
        return mSelectedItemsList;
    }
    
    public void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }
    
    private File getSelectedFile() {
        ArrayList<String> imagePathsFromSelectedFolder = getImagePathsAt(mSelectedItemsList.get(0)); // Use get 0 since there is only one element in the list
        File folder =  new File(imagePathsFromSelectedFolder.get(0)).getParentFile();
        return folder;
    }
    
    private void startEditFolderName() {
        LayoutInflater li = LayoutInflater.from(mContext);
        View promptsView = li.inflate(R.layout.alert_edit_folder, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

        // Set view of alert dialog to our view from xml
        alertDialogBuilder.setView(promptsView);
        final File selectedFile = getSelectedFile();
        
        final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);
        userInput.setText(selectedFile.getName());
        alertDialogBuilder
            .setTitle(R.string.rename_folder)
            .setCancelable(false)
            .setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int id) {
                     File updatedFile = new File(selectedFile.getParent() + "/" + userInput.getText().toString());
                     boolean renameSuccess = selectedFile.renameTo(updatedFile);
                     if (renameSuccess) {
                         manageSelectedItemsList(mSelectedItemsList.get(0)); // This deselects the edited folder
                         mMainActivity.reloadFolderPaths(); // Only this makes the new name appear in the UI
                     } else {
                         startRenameErrorAlert();
                     }
                     
                 }
             })
            .setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int id) {
                     dialog.cancel();
                 }
             });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }
    
    private void startRenameErrorAlert() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        
        alertDialogBuilder
            .setTitle(R.string.could_not_rename_folder)
            .setMessage(R.string.folder_is_existing)
            .setCancelable(false)
            .setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int id) {
                     dialog.cancel();
                     startEditFolderName();
                 }
             });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }
    
    void navigateBack() {
        mAdapterMode--;
        if (mAdapterMode == IMAGE_MODE) {
        	setupViewPager();
        } else {
        	setupGridView();
        }
        mMainActivity.setActionBarArrowDependingOnAdapterMode();
    }
    
    private void navigateForward(int itemPosition) {
        mAdapterMode++;
        
        if (mAdapterMode == IMAGE_MODE) {
        	setupViewPager();
        } else {
        	mFolderPosition = itemPosition;
        	setupGridView();
        }
       
        mMainActivity.setActionBarArrowDependingOnAdapterMode();
    }
    
    public int getAdapterMode() {
        return mAdapterMode;
    }
    
    @Override
    public void refreshAdapter() {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void updateAdapterContent(ArrayList<List <String>> imagePaths) {
        /*if (mAdapterMode == FOLDER_MODE) {
            ((FolderViewImageAdapter) mAdapter).updateImagePaths(imagePaths);
        } else if (mAdapterMode == GALLERY_MODE) {
            ((GridViewImageAdapter2) mAdapter).updateImagePaths((ArrayList<String>) imagePaths.get(mFolderPosition));
        } else if (mAdapterMode == IMAGE_MODE) {
            // TODO
        }*/
        
    }

    @Override
    public void updateImagePaths(ArrayList<List<String>> imagePaths) {
        // TODO Auto-generated method stub
        
    }
}
