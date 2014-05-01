package org.sebbas.android.flickcam;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.sebbas.android.adapter.FolderViewImageAdapter;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class MediaDeleterThread extends AsyncTask<Void, Void, Void> {

    private int successfulDeleteFolder = 0;
    private int unsuccessfulDeleteFolder = 0;
    private int successfulDeleteImage = 0;
    private int unsuccessfulDeleteImage = 0;
    private Context mContext;
    private ArrayList<Integer> mSelectedItemsList = new ArrayList<Integer>();
    private FolderViewImageAdapter mAdapter;
    private int mDeleteModeId; // 0 -> delete folders; 1 -> delete image only
    
    public MediaDeleterThread(Context context, ArrayList<Integer> selectedItemsList, FolderViewImageAdapter adapter, int deleteModeId) {
        mContext = context;
        mSelectedItemsList = selectedItemsList;
        mAdapter = adapter;
        mDeleteModeId = deleteModeId;
    }
    
    @Override
    protected Void doInBackground(Void... params) {
        // Check what type of media we are going to delete 
        if (mDeleteModeId == 0) {
            deleteSelectedFolders();
        } else if (mDeleteModeId == 1) {
            // TODO
        }
        return null;
    }
    
    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        if (mDeleteModeId == 0) {
            notifyDeletedFolders();
        } else if (mDeleteModeId == 1) {
            // TODO
        }
    }

    private void deleteSelectedFolders() {
        for (int folderPosition : mSelectedItemsList) {
            File folderToDelete = new File(mAdapter.getImagePaths().get(folderPosition).get(0)).getParentFile();
            List<String> imagePathsFromFolder = mAdapter.getImagePaths().get(folderPosition);
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
    
    private void notifyDeletedFolders() {
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
}
