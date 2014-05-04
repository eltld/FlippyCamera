package org.sebbas.android.flickcam;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.sebbas.android.helper.DeviceInfo;
import org.sebbas.android.interfaces.CameraUICommunicator;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class PictureWriterThread extends Thread {

    private static final String NO_STORAGE_AVAILABLE = "No storage available on this device";
    private static final String FAILED_TO_SAVE_PICTURE = "Failed to save picture";
    private static final String IS_SAVING_PICTURE = "Saving your picture ...";
    private static final String SAVED_PICTURE_SUCCESSFULLY = "Picture saved successfully!";
    private static final String TAG = "picture_writer_thread";
    private static final String ALBUM_NAME = "FlickCam";
    
    private Handler mHandler;
    private CameraUICommunicator mCameraUICommunicator;
    private Context mContext;
    private MainFragmentActivity mMainFragment;
    
    public PictureWriterThread(Context context, CameraUICommunicator cameraThreadListener) {
        mContext = context;
        mMainFragment = (MainFragmentActivity) context;
        mCameraUICommunicator = cameraThreadListener; // For communication with the UI
    }
    
    @Override
    public void run() {
        Looper.prepare();
        synchronized(this) {
            super.run();
            try {
                
                mHandler = new Handler();
                this.notifyAll();

            } catch (Throwable t) {
                Log.e(TAG, "Picture writer thread halted due to an error", t);
            }
        }
        Looper.loop();
    }
    
    private synchronized Handler getHandler() {
        return mHandler;
    }
    
    public void removeAllCallbacks() {
    	mHandler.removeCallbacks(null);
    }
    
    public synchronized void writeDataToFile(final byte[] data) {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                String filename = getAlbumStorageDir() + "/" + getDefaultFilename();
                if (!DeviceInfo.isExternalStorageWritable()) {
                    mCameraUICommunicator.alertCameraThread(NO_STORAGE_AVAILABLE);
                } else if (data == null) {
                    mCameraUICommunicator.alertCameraThread(FAILED_TO_SAVE_PICTURE);
                    Log.d(TAG, "Data Was Empty, Not Writing to File");
                } else {
                    mCameraUICommunicator.alertCameraThread(IS_SAVING_PICTURE);
                    
                    try {
                        FileOutputStream output = new FileOutputStream(filename);
                        output.write(data);
                        output.flush();
                        output.close();
                        mCameraUICommunicator.alertCameraThread(SAVED_PICTURE_SUCCESSFULLY);
                        Log.d(TAG, "Image Saved Successfully");
                    } catch (IOException e) {
                        mCameraUICommunicator.alertCameraThread(FAILED_TO_SAVE_PICTURE);
                        Log.d(TAG, "Saving Image Failed!");
                    } finally {
                        // We have to refresh the grid view UI to make the new photo show up
                        //mMainFragment.reloadFolderGallery();
                        mMainFragment.getAdapter().notifyDataSetChanged();
                    }
                }
            }
            
        });
    }
    
    private String getDefaultFilename() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return "IMG_" + timeStamp + ".jpeg";
    }
    
    private File getAlbumStorageDir() {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ALBUM_NAME);
        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }
}
