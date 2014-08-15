package org.sebbas.android.threads;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sebbas.android.flippycamera.FolderFragment;
import org.sebbas.android.flippycamera.MainActivity;
import org.sebbas.android.flippycamera.GalleryActivity;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
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
    private ArrayList<List<String>> mImagePaths;
    private Activity mMainActivity;
    private int mDeleteModeId; // 0 -> delete folders; 1 -> delete image only
    private int mFolderPosition;
    
    public MediaDeleterThread(Activity mainActivity, ArrayList<Integer> selectedItemsList, ArrayList<List<String>> imagePath, int folderPosition, int deleteModeId) {
        mContext = mainActivity.getApplicationContext();
        mMainActivity = mainActivity;
        mSelectedItemsList = selectedItemsList;
        mImagePaths = imagePath;
        
        // We have to sort the selected items list (makes it in ascending order) and then reverse the list so that we get a list of the selected 
        //items in descending order. This is important when deleting the images later. If this is not done, bad things (like IndexOutOfBounds 
        //exceptions) can occur.
        Collections.sort(mSelectedItemsList);
        Collections.reverse(mSelectedItemsList);
        
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
            if (mImagePaths.size() == mSelectedItemsList.size()) {
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
        
        @SuppressWarnings("unchecked")
        ArrayList<List <String>> newFolders = (ArrayList<List <String>>) result;
        
        // Also: reload the folder fragment since it does not know when the media deleter has finished
        if (mDeleteModeId == 0) {
            toastDeletedItems();
            ((MainActivity) mMainActivity).updateImagePaths(newFolders);
            FolderFragment folderFragment = ((MainActivity) mMainActivity).getFolderFragment();
            folderFragment.updateAdapterContent(newFolders);
            folderFragment.refreshAdapter();
        } else if (mDeleteModeId == 1) {
            toastDeletedImages();
            
            // 
            if (newFolders.get(mFolderPosition).isEmpty()) {
                newFolders.remove(mFolderPosition);
                ((GalleryActivity) mMainActivity).updateImagePaths(newFolders);
                ((GalleryActivity) mMainActivity).startReturnIntent();
            } else {
                ((GalleryActivity) mMainActivity).updateImagePaths(newFolders);
                ((GalleryActivity) mMainActivity).updateAdapterContent((ArrayList<String>) newFolders.get(mFolderPosition));
                ((GalleryActivity) mMainActivity).refreshAdapter();
                ((GalleryActivity) mMainActivity).setupActionBarTitle();
            }
            
        }
    }

    private ArrayList<List <String>> deleteSelectedFolders() {
        ArrayList<List <String>> currentFolders = new ArrayList<List <String>>(mImagePaths);
        
        for (int folderPosition : mSelectedItemsList) {
            List<String> imagePathList = mImagePaths.get(folderPosition);
            File folderToDelete = new File(imagePathList.get(0)).getParentFile();
            for (int i = 0; i < imagePathList.size(); i++) { // Delete all images in folder
                boolean deleteSuccess = new File(imagePathList.get(i)).delete();
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
        ArrayList<List <String>> currentFolders = new ArrayList<List <String>>(mImagePaths);
        
        for (int imagePosition : mSelectedItemsList) {
            File imageToDelete = new File(mImagePaths.get(mFolderPosition).get(imagePosition));
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