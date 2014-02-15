package org.sebbas.android.flickcam;

import java.io.File;
import java.io.IOException;

import org.sebbas.android.views.CameraPreview;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class MediaRecorderSetup extends AsyncTask<Void, Void, Void> {

    private static final String TAG = "media_recorder_setup";
    private static final int MAX_VIDEO_DURATION = 60000;
    private static final long MAX_FILE_SIZE = 500000;
    private static final String VIDEO_PATH_NAME = "/FlickCam/";
    
    private Camera mCamera;
    private MediaRecorder mMediaRecorder;
    private int mCurrentCameraId;
    private CameraPreview mCameraPreview;

    public MediaRecorderSetup(Camera camera, int cameraId) {
        super();
        mCamera = camera;
        mCurrentCameraId = cameraId;
    }
    
    public MediaRecorderSetup(Camera camera, int cameraId, CameraPreview cameraPreview) {
        super();
        mCamera = camera;
        mCurrentCameraId = cameraId;
        mCameraPreview = cameraPreview;
    }
    
    @Override
    protected Void doInBackground(Void... params) {
        stopPreview();
        startPreview();
        return null;
    }
    
    private void stopPreview() {
        // stop preview before making changes
        try {
            mCamera.stopPreview();
            releaseMediaRecorder();
            
            Log.d(TAG, "Camera Stopped Successfully");
        } catch (Exception e) {
            Log.d(TAG, "Error Stopping Camera");
        }
    }
    
    private void startPreview() {
        // start preview with new settings
        try {
            mCamera.startPreview();
            
            prepareMediaRecorder();
            startMediaRecorder();
            
            Log.d(TAG, "Preview Started Successfully");
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }
    
    
    private void prepareMediaRecorder() {
        Log.d(TAG, "Called prepare media recorder");
        mMediaRecorder = new MediaRecorder();
        
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        
        mMediaRecorder.setProfile(CamcorderProfile.get(mCurrentCameraId, CamcorderProfile.QUALITY_HIGH));
        
        mMediaRecorder.setOutputFile(getFile().getAbsolutePath());
        mMediaRecorder.setMaxDuration(MAX_VIDEO_DURATION);
        mMediaRecorder.setMaxFileSize(MAX_FILE_SIZE);
        
        // We have to set the preview display for devices that use a SurfaceView
        if(!supportsSDK(14)) {
            mMediaRecorder.setPreviewDisplay(mCameraPreview.getHolder().getSurface());
        }
        
        try {
            mMediaRecorder.prepare();
            Log.d(TAG, "Prepare media recorder successful");
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            Log.d(TAG, "Failed to prepare media recorder- IllegalStateException");
        } catch (IOException e) {
            releaseMediaRecorder();
            Log.d(TAG, "Failed to prepare media recorder. IOException");
        }
    }
    
    
    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.lock();
        }
    }
    
    private void startMediaRecorder() {
        Log.d(TAG, "Called start media recorder");
        mMediaRecorder.start();
    }
    
    // Check to see if the device supports the indicated SDK
    private static boolean supportsSDK(int sdk) {
        if (android.os.Build.VERSION.SDK_INT >= sdk) {
            return true;
        } 
        return false;
    }

    private File getFile() {
        File file = new File(Environment.getExternalStorageDirectory(), VIDEO_PATH_NAME);
        // "touch" the file
        if(!file.exists()) {
            File parent = file.getParentFile();
            if(parent != null) 
                if(!parent.exists())
                    if(!parent.mkdirs())
                        try {
                            throw new IOException("Cannot create " +
                                    "parent directories for file: " + file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return file;
    }
}
