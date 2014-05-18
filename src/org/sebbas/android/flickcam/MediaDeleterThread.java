package org.sebbas.android.flickcam;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

public class MediaDeleterThread extends AsyncTask<Void, Void, Object> {

    private static final String TAG = "media_deleter_thread";
    
    private int successfulDeleteFolder;
    private int unsuccessfulDeleteFolder;
    private int successfulDeleteImage;
    private int unsuccessfulDeleteImage;
    private Context mContext;
    private ArrayList<Integer> mSelectedItemsList;
    private Fragment mFragment;
    private MainFragmentActivity mMainFragment;
    private int mDeleteModeId; // 0 -> delete folders; 1 -> delete image only
    private int mFolderPosition;
    
    public MediaDeleterThread(Context context, ArrayList<Integer> selectedItemsList, Fragment fragment, int folderPosition, int deleteModeId) {
        mContext = context;
        mMainFragment = (MainFragmentActivity) fragment.getActivity();
        mSelectedItemsList = selectedItemsList;
        
        // We have to sort the selected items list (makes it in ascending order) and then reverse the list so that we get a list of the selected 
        //items in descending order. This is important when deleting the images later. If this is not done, bad things (like IndexOutOfBounds 
        //exceptions) can occur.
        Collections.sort(mSelectedItemsList);
        Collections.reverse(mSelectedItemsList);
        
        mFragment = fragment;
        mFolderPosition = folderPosition;
        
        mDeleteModeId = deleteModeId;
        successfulDeleteFolder = 0;
        unsuccessfulDeleteFolder = 0;
        successfulDeleteImage = 0;
        unsuccessfulDeleteImage = 0;
    }
    
    @Override
    protected Object doInBackground(Void... params) {
        // Check what type of media we are going to delete 
        if (mDeleteModeId == 0) {
            return deleteSelectedFolders();
        } else if (mDeleteModeId == 1) {
            ArrayList<String> currentImagePaths = (ArrayList<String>) mMainFragment.getImagePaths().get(mFolderPosition);
            if (currentImagePaths.size() == mSelectedItemsList.size()) {
                mSelectedItemsList.clear();
                mSelectedItemsList.add(mFolderPosition);
                return deleteSelectedFolders();
            } else {
                return deleteSelectedImages();
            }
        }
        return null;
    }
    
    @Override
    protected void onPostExecute(Object result) {
        super.onPostExecute(result);
        
        ArrayList<List<String>> oldFolders = mMainFragment.getImagePaths();
        ArrayList<List <String>> newFolders = (ArrayList<List <String>>) result;
        
        mMainFragment.updateImagePaths(newFolders); // Update the "root" image paths 
        if (mDeleteModeId == 0) {
            toastDeletedItems();
            ((FolderFragment) mFragment).updateAdapterContent(newFolders);
            ((FolderFragment) mFragment).refreshAdapter();
        } else if (mDeleteModeId == 1) {
            toastDeletedImages();
            // If there are less folders now (-> newFolders < oldFolders) then an entire folder was deleted and we navigate back to folder view
            if (oldFolders.size() > newFolders.size()) {
            	((MainFragmentActivity) mFragment.getActivity()).handleNavigationBack();
            	((MainFragmentActivity) mFragment.getActivity()).handleHomeUpNavigation();
            } else {
            	((GalleryFragment) mFragment).updateAdapterContent((ArrayList<String>) newFolders.get(mFolderPosition));
                ((GalleryFragment) mFragment).refreshAdapter();
                ((GalleryFragment) mFragment).setupActionBarTitle();
            }
        }
    }

    private ArrayList<List <String>> deleteSelectedFolders() {
    	ArrayList<List <String>> currentFolders = new ArrayList<List <String>>(mMainFragment.getImagePaths());
    	
        for (int folderPosition : mSelectedItemsList) {
            File folderToDelete = new File(mMainFragment.getImagePaths().get(folderPosition).get(0)).getParentFile();
            List<String> imagePathsFromFolder = mMainFragment.getImagePaths().get(folderPosition);
            for (int i = 0; i < imagePathsFromFolder.size(); i++) { // Delete all images in folder
                boolean deleteSuccess = new File(imagePathsFromFolder.get(i)).delete();
                if (deleteSuccess) {
                    successfulDeleteImage++;
                } else {
                    unsuccessfulDeleteImage++;
                }
            }
            if (unsuccessfulDeleteImage != 0) {
                unsuccessfulDeleteFolder++;
            } else {
                successfulDeleteFolder++;
                currentFolders.remove(folderPosition);
                folderToDelete.delete(); // Delete the empty folder
            }
        }
        return currentFolders;
    }
    

    private ArrayList<List <String>> deleteSelectedImages() {
    	ArrayList<List <String>> currentFolders = new ArrayList<List<String>>(mMainFragment.getImagePaths());
        
        for (int imagePosition : mSelectedItemsList) {
        	Log.d(TAG, "position is " + imagePosition);
        	Log.d(TAG, "current folders " + currentFolders.get(mFolderPosition).size());
            File imageToDelete = new File(mMainFragment.getImagePaths().get(mFolderPosition).get(imagePosition));
            boolean deleteSuccess = imageToDelete.delete();
            if (deleteSuccess) {
                successfulDeleteImage++;
                currentFolders.get(mFolderPosition).remove(imagePosition); // Since deletion was successful, remove the deleted image from the list
            } else {
                unsuccessfulDeleteImage++;
            }
        }
        return currentFolders;
    }
    
    private void toastDeletedItems() {
        String messageNumberImages;
        String messageSuccess;
        String messageFail;
        
        if (successfulDeleteImage == 1) {
            messageNumberImages = "(" + successfulDeleteImage + " image)";
        } else {
            messageNumberImages = "(" + successfulDeleteImage + " images)";
        }
        if (successfulDeleteFolder == 1 || unsuccessfulDeleteFolder == 1) {
            messageSuccess = "Deleted " + successfulDeleteFolder + " folder " + messageNumberImages + " successfully";
            messageFail = "Could not delete " + unsuccessfulDeleteFolder + " folder";
        } else {
            messageSuccess = "Deleted " + successfulDeleteFolder + " folders " + messageNumberImages + " successfully";
            messageFail = "Could not delete " + unsuccessfulDeleteFolder + " folders";
        }
        
        // Finally show the constructed strings in a toast
        if (successfulDeleteFolder != 0) {
            Toast.makeText(mContext, messageSuccess, Toast.LENGTH_LONG).show();
        }
        if (unsuccessfulDeleteFolder != 0) {
            Toast.makeText(mContext, messageFail, Toast.LENGTH_LONG).show();
        }
    }
    
    private void toastDeletedImages() {
        String messageSuccess;
        String messageFail;

        if (successfulDeleteImage == 1) {
            messageSuccess = "Deleted " + successfulDeleteImage + " image successfully";
            messageFail = "Could not delete " + unsuccessfulDeleteImage + " image";
        } else {
            messageSuccess = "Deleted " + successfulDeleteImage + " images successfully";
            messageFail = "Could not delete " + unsuccessfulDeleteImage + " images";
        }
        
        // Finally show the constructed strings in a toast
        if (successfulDeleteImage != 0) {
            Toast.makeText(mContext, messageSuccess, Toast.LENGTH_LONG).show();
        }
        if (unsuccessfulDeleteImage != 0) {
            Toast.makeText(mContext, messageFail, Toast.LENGTH_LONG).show();
        }
    }
}