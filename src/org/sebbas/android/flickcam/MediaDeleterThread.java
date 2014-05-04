package org.sebbas.android.flickcam;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.sebbas.android.adapter.FolderViewImageAdapter;
import org.sebbas.android.adapter.GridViewImageAdapter;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.BaseAdapter;
import android.widget.Toast;

public class MediaDeleterThread extends AsyncTask<Void, Void, Void> {

    private int successfulDeleteFolder;
    private int unsuccessfulDeleteFolder;
    private int successfulDeleteImage;
    private int unsuccessfulDeleteImage;
    private Context mContext;
    private ArrayList<Integer> mSelectedItemsList;
    private BaseAdapter mAdapter;
    private int mDeleteModeId; // 0 -> delete folders; 1 -> delete image only
    
    public MediaDeleterThread(Context context, ArrayList<Integer> selectedItemsList, BaseAdapter adapter, int deleteModeId) {
        mContext = context;
        mSelectedItemsList = selectedItemsList;
        mAdapter = adapter;
        mDeleteModeId = deleteModeId;
        successfulDeleteFolder = 0;
        unsuccessfulDeleteFolder = 0;
        successfulDeleteImage = 0;
        unsuccessfulDeleteImage = 0;
    }
    
    @Override
    protected Void doInBackground(Void... params) {
        // Check what type of media we are going to delete 
        if (mDeleteModeId == 0) {
            deleteSelectedFolders();
        } else if (mDeleteModeId == 1) {
            deleteSelectedImages();
        }
        return null;
    }
    
    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        if (mDeleteModeId == 0) {
            toastDeletedFolders();
        } else if (mDeleteModeId == 1) {
            toastDeletedImages();
        }
    }

    private void deleteSelectedFolders() {
        for (int folderPosition : mSelectedItemsList) {
            File folderToDelete = new File(((FolderViewImageAdapter) mAdapter).getImagePaths().get(folderPosition).get(0)).getParentFile();
            List<String> imagePathsFromFolder = ((FolderViewImageAdapter) mAdapter).getImagePaths().get(folderPosition);
            for (int i = 0; i < imagePathsFromFolder.size(); i++) {
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
                folderToDelete.delete(); // Delete the empty directory
            }
        }
    }
    



	private void deleteSelectedImages() {
        for (int imagePosition : mSelectedItemsList) {
            File imageToDelete = new File(((GridViewImageAdapter) mAdapter).getImagePaths().get(imagePosition));
            boolean deleteSuccess = imageToDelete.delete();
            if (deleteSuccess) {
                successfulDeleteImage++;
            } else {
                unsuccessfulDeleteImage++;
            }
        }
    }
    
    private void toastDeletedFolders() {
        String messageNumberImages;
        String messageSuccess;
        String messageFail;
        
        if (successfulDeleteImage == 1) {
            messageNumberImages = "(" + successfulDeleteImage + " image)";
        } else {
            messageNumberImages = "(" + successfulDeleteImage + " images)";
        }
        if (successfulDeleteFolder == 1) {
            messageSuccess = "Deleted " + successfulDeleteFolder + " folder " + messageNumberImages + " successfully";
            messageFail = "Could not delete " + unsuccessfulDeleteFolder + " folder";
        } else {
            messageSuccess = "Deleted " + successfulDeleteFolder + " folders " + messageNumberImages + " successfully";
            messageFail = "Could not delete " + unsuccessfulDeleteFolder + " folders";
        }
        
        // Finally show the constructed strings in a toast
        Toast.makeText(mContext, messageSuccess, Toast.LENGTH_LONG).show();
        if (unsuccessfulDeleteFolder != 0) {
            Toast.makeText(mContext, messageFail, Toast.LENGTH_LONG).show();
        }
    }
    
    private void toastDeletedImages() {
        String messageSuccess;
        String messageFail;

        if (successfulDeleteFolder == 1) {
            messageSuccess = "Deleted " + successfulDeleteImage + " image successfully";
            messageFail = "Could not delete " + unsuccessfulDeleteImage + " image";
        } else {
            messageSuccess = "Deleted " + successfulDeleteImage + " images successfully";
            messageFail = "Could not delete " + unsuccessfulDeleteImage + " images";
        }
        
        // Finally show the constructed strings in a toast
        Toast.makeText(mContext, messageSuccess, Toast.LENGTH_LONG).show();
        if (unsuccessfulDeleteImage != 0) {
            Toast.makeText(mContext, messageFail, Toast.LENGTH_LONG).show();
        }
    }
}
