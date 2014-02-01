package org.sebbas.android.flickcam;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.ErrorCallback;
import android.util.Log;
import android.view.Surface;

public class CameraObject {

    private static final String TAG = "Camera Object";
    
	private int mCurrentCameraID;
    private Camera mCamera;
	private ErrorCallback mErrorCallback;
    
    public CameraObject(int currentCameraID) {
        mCurrentCameraID = currentCameraID;
        //mCamera = getCameraInstance();
    }/*
    
    private Camera getCameraInstance() {
        try {
            // Try to get a camera instance. By default this is the back facing camera (if present at all)
            mCamera = Camera.open(mCurrentCameraID);
            // Need to handle the Media Server Dying
            mCamera.setErrorCallback(getErrorCallback());
            
            Log.d(TAG, "Camera Opened Successfully");
        } catch (RuntimeException e) {
            Log.d(TAG, "Failed to Open Camera - " + e.getMessage());
            // Camera is not available (in use or does not exist)
        }
        return mCamera; // returns null if camera is unavailable
    }
    
    private void initializeCamera() {
        if (hasCamera(this) && getCameraInstance() != null) {
            initCameraProperties();
            setCameraParameters(); // Add parameter for picture size (makes preview smoother), continuous autofocus, pinch to zoom feature
            setCameraDisplayOrientation(this, mCurrentCameraId, mCamera);
            Log.d(TAG, "Camera Initialization has Been Completed");
        }
    }
    
    private void deinitializeCamera() {
        stopPreview();
        releaseCamera();
        Log.d(TAG, "Camera Initialization has Been Undone");
    }
    
    private Camera.ErrorCallback getErrorCallback() {
        if (mErrorCallback == null) {
            mErrorCallback = new Camera.ErrorCallback() {

                @Override
                public void onError(int error, Camera camera) {
                    if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
                        deinitializeCamera();
                        initializeCamera();
                    }
                }
            };
        }
        return mErrorCallback;
    }
    */
    public static void setCameraDisplayOrientation(Activity activity,
        int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

}
