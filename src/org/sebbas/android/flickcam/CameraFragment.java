package org.sebbas.android.flickcam;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.sebbas.android.interfaces.CameraFragmentListener;
import org.sebbas.android.interfaces.CameraPreviewListener;
import org.sebbas.android.views.CameraPreview;
import org.sebbas.android.views.CameraPreviewAdvanced;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class CameraFragment extends Fragment implements CameraPreviewListener {

    public static final String TAG = "camera_fragment";
    private static final int MAX_VIDEO_DURATION = 60000;
    private static final long MAX_FILE_SIZE = 500000;
    private static final String VIDEO_PATH_NAME = "/FlickCam.mp4/";
    
    private Camera mCamera;
    private int mCurrentCameraId;
    private Context mContext;
    private Parameters mParameters;
    private int mZoomMax;
    private boolean mSmoothZoomSupported;
    private int mZoomValue;
    private boolean mVideoStabilizationSupported;
    private boolean mFlashEnabled;
    private ErrorCallback mErrorCallback;
    private CameraPreviewAdvanced mCameraPreviewAdvanced;
    private CameraPreview mCameraPreview;
    private ImageButton mShutterButton;
    private ImageButton mSwitchCameraButton;
    private ImageButton mSwitchFlashButton;
    private OnClickListener mSwitchFlashListener;
    private OnClickListener mSwitchCameraListener;
    private OnClickListener mShutterListener;
    private MediaRecorder mMediaRecorder;
    private View mRootView;
    protected boolean mPictureTaken = false;
    private ShutterCallback mShutterCallback;
    private PictureCallback mRawCallback;
    private PictureCallback mPostViewCallback;
    private PictureCallback mJpegCallback;
    private ImageButton mCancelButton;
    private ImageButton mAcceptButton;
    private byte[] mPictureData;
    private RelativeLayout mPreviewLayout;
    private OnClickListener mAcceptListener;
    private CameraFragmentListener mCameraFragmentListener;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "ONCREATEVIEW");
        initParameters(inflater, container);
        hideSystemNavigation();
        return mRootView;
    }

    @Override
    public void onStart() {
        Log.d(TAG, "ONSTART");
        super.onStart();
    }

    
    @SuppressLint("NewApi")
    @Override
    public void onResume() {
        Log.d(TAG, "ONRESUME");
        super.onResume();
        
        initializeCamera();
        startPreview();
        
        if(!supportsSDK(14)) {
            mCameraPreview.setVisibility(View.VISIBLE);
        } else {
            //mCameraPreviewAdvanced.setVisibility(View.VISIBLE);
        }
        
    }
    
    @Override
    public void onPause() {
        Log.d(TAG, "ONPAUSE");
        super.onPause();
        
        releaseMediaRecorder();
        deinitializeCamera();
        
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "ONDESTROY");
        super.onDestroy();
    }

    @SuppressLint("NewApi")
    @Override
    public void onStop() {
        Log.d(TAG, "ONSTOP");
        super.onStop();
        if(!supportsSDK(14)) {
            mCameraPreview.setVisibility(View.INVISIBLE);
            mCameraPreview = null;
        } else {
            mCameraPreviewAdvanced.setVisibility(View.INVISIBLE);
            mCameraPreviewAdvanced = null;
        }
    }

    private void initParameters(LayoutInflater inflater, ViewGroup container) {
        mContext = this.getActivity();
        mRootView = inflater.inflate(R.layout.camera_layout, container, false);
        mPreviewLayout = (RelativeLayout) mRootView.findViewById(R.id.preview_layout);
        mSwitchCameraButton = (ImageButton) mRootView.findViewById(R.id.switch_camera);
        mShutterButton = (ImageButton) mRootView.findViewById(R.id.shutter_button);
        mSwitchFlashButton = (ImageButton) mRootView.findViewById(R.id.switch_flash);
        mAcceptButton = (ImageButton) mRootView.findViewById(R.id.accept_image);
        mCancelButton = (ImageButton) mRootView.findViewById(R.id.discard_image);
    }
    
    @SuppressLint("NewApi")
    private void hideSystemNavigation() {
        if (supportsSDK(11)) {
            mRootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }
    
    private void initializeCamera() {
        if (hasCamera(mContext) && getCameraInstance() != null) {
            initCameraProperties();
            setCameraParameters(); // Add parameter for picture size (makes preview smoother), continuous autofocus, pinch to zoom feature
            setCameraDisplayOrientation(mContext, mCurrentCameraId, mCamera);
            Log.d(TAG, "Camera Initialization has Been Completed");
        }
    }
    
    private void deinitializeCamera() {
        stopPreview();
        releaseCamera();
        
        Log.d(TAG, "Camera Initialization has Been Undone");
    }
    
    private void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
        //removeCameraPreviewView();
        //mCameraPreview = null;
        Log.d(TAG, "Preview Stopped");
    }
    
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            //mCamera = null;
            Log.d(TAG, "Camera Released");
        }
    }
    
    private Camera getCameraInstance() {
        try {
            // Try to get a camera instance. By default this is the back facing camera (if present at all)
            mCamera = Camera.open(mCurrentCameraId);
            // Need to handle the Media Server Dying
            mCamera.setErrorCallback(getErrorCallback());
            
            Log.d(TAG, "Camera Opened Successfully");
        } catch (RuntimeException e) {
            Log.d(TAG, "Failed to Open Camera - " + e.getMessage());
            // Camera is not available (in use or does not exist)
        }
        return mCamera; // returns null if camera is unavailable
    }
    
    private boolean hasCamera(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Log.d(TAG, "At Least one Camera Detected!");
            return true;
        } else {
            Log.d(TAG, "No Cameras detected.");
            return false;
        }
    }
    
    @SuppressLint("NewApi")
    private void initCameraProperties() {
        mParameters = mCamera.getParameters();
        mZoomMax = mParameters.getMaxZoom();
        mSmoothZoomSupported = mParameters.isSmoothZoomSupported();
        if (supportsSDK(15)) {
            mVideoStabilizationSupported = mParameters.isVideoStabilizationSupported();
        }
        mZoomValue = 0;
    }
    
    // Check to see if the device supports the indicated SDK
    private static boolean supportsSDK(int sdk) {
        if (android.os.Build.VERSION.SDK_INT >= sdk) {
            return true;
        } 
        return false;
    }
    
    @SuppressLint("NewApi")
    private void setCameraParameters() {
        System.out.println("setCameraParameters called");
        // Adds continuous auto focus (only if API is high enough) to the parameters.
        if (supportsSDK(14)) {
            mParameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else {
            mParameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } 
        
        // Enable auto white balance. This makes preview look smoother
        mParameters.setWhiteBalance(Parameters.WHITE_BALANCE_AUTO);
        
        if (mFlashEnabled) {
            mParameters.setFlashMode(Parameters.FLASH_MODE_ON);
        }
        
        // This will only get called if video stabilization is supported and if the API is high enough. All handled in initCameraProperties.
        if (mVideoStabilizationSupported && supportsSDK(15)) {
            mParameters.setVideoStabilization(true);
        }
        
        // Set the current zoom value if we didn't use smooth zoom
        if (!mSmoothZoomSupported) {
            mParameters.setZoom(mZoomValue);
        }
        // TODO Does this work with other devices ???
        //List<Size> supportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
        //mParameters.setPreviewSize(supportedPreviewSizes.get(2).width, supportedPreviewSizes.get(2).height);
        
        // Finally, add the parameters to the camera
        mCamera.setParameters(mParameters);
    }
    
    public static void setCameraDisplayOrientation(Context context,
        int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
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
    
    @Override
    public void performZoom(float scaleFactor) {
        mZoomValue = (int) ((scaleFactor - 1) * (mZoomMax + 1));
        if (mZoomValue > 0 && mZoomValue < mZoomMax) {
            if (mSmoothZoomSupported) {
                mCamera.startSmoothZoom(mZoomValue);
            } else {
                //mParameters.setZoom(zoomValue);
                //mCamera.setParameters(mParameters);
                setCameraParameters(); // Just update the camera parameters. This will also set the new zoom level
            }
        }
    }
    
    @SuppressLint("NewApi")
    private void startPreview() {
        if (mCamera != null) {
            //if (mCameraPreview == null) {

                // If device supports API 14 then add a TextureView (better performance) to the RL, else add a SurfaceView (no other choice)
                if(supportsSDK(14)) {
                    mCameraPreviewAdvanced = new CameraPreviewAdvanced(mContext, this, mCamera);
                    mPreviewLayout.addView(mCameraPreviewAdvanced);
                } else {
                    mCameraPreview = new CameraPreview(mContext, this, mCamera);
                    mPreviewLayout.addView(mCameraPreview);
                }
                
                
                mShutterButton.setOnClickListener(getShutterListener());
                mShutterButton.bringToFront();
                mShutterButton.invalidate();
                
                mSwitchCameraButton.setOnClickListener(getSwitchCameraListener());
                mSwitchCameraButton.bringToFront();
                mSwitchCameraButton.invalidate();
                
                mSwitchFlashButton.setOnClickListener(getSwitchFlashListener());
                mSwitchFlashButton.bringToFront();
                mSwitchFlashButton.invalidate();
                
                if(supportsSDK(14)) {
                    mCameraPreviewAdvanced.invalidate();
                } else {
                    mCameraPreview.invalidate();
                }
                
                Log.d(TAG, "New Preview Started");
            /*} else {
                mCamera.startPreview();
            }*/
        }
    }
    
    private OnClickListener getShutterListener() {
        if (mShutterListener == null) {
            mShutterListener = new OnClickListener() {

                @Override
                public void onClick(View view) {
                    Log.d(TAG, "Shutter Button Clicked.");
                    if (mPictureTaken) {
                        retakePicture();
                    } else {
                        takePicture();
                    }
                }
            };
        }
        return mShutterListener;
    }
    
    private void takePicture() {
        mCamera.takePicture(getShutterCallback(), getRawCallback(), getPostViewCallback(), getJpegCallback());
    }

    private void retakePicture() {
        resetShutter();
        startPreview();
    }
    
    private OnClickListener getAcceptListener() {
        if (mAcceptListener == null) {
            mAcceptListener = new OnClickListener() {

                @Override
                public void onClick(View view) {
                    Toast toast = Toast.makeText(mContext, "Saving Image...", Toast.LENGTH_LONG);
                    toast.show();

                    Log.d(TAG, "Accept Button Clicked.");
                    if (writeBytesToFile(mPictureData, getDefaultSavePath() + getDefaultFilename())) {
                        toast = Toast.makeText(mContext, "Image Saved Successfully.", Toast.LENGTH_LONG);
                    }
                    else {
                        toast = Toast.makeText(mContext, "Failed to Save Image. See Log for Details.", Toast.LENGTH_LONG);
                    }
                    toast.show();
                }
            };
        }
        return mAcceptListener;
    }
    
    private boolean writeBytesToFile(byte[] data, String filename) {
        if (data == null) {
            Log.d(TAG, "Data Was Empty, Not Writing to File");
            return false;
        }
        Log.d(TAG, "Filename is" + filename);
        try {
            FileOutputStream output = new FileOutputStream(filename);
            output.write(data);
            output.close();

            Log.d(TAG, "Image Saved Successfully");
            return true;
        } catch (IOException e) {
            Log.d(TAG, "Saving Image Failed!");
            return false;
        }
    }
    
    private String getDefaultSavePath() {
        createSavingDirectory();
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + mContext.getPackageName() + "/";
    }
    
    private String getDefaultFilename() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return "IMG_" + timeStamp + ".jpeg";
    }
    
    private void createSavingDirectory() {
        File sdcard = Environment.getExternalStorageDirectory();
        File dir = new File(sdcard.getAbsoluteFile() + "/Android/data/" + mContext.getPackageName() + "/");
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
    }
    
    /*
     * This functionality has not been added to this project, so this always returns null.
     */
    private ShutterCallback getShutterCallback() {
        return mShutterCallback;
    }

    private PictureCallback getRawCallback() {
        return mRawCallback;
    }

    private PictureCallback getPostViewCallback() {
        return mPostViewCallback;
    }

    private PictureCallback getJpegCallback() {
        Log.d(TAG, "JPEG Callback");
        if (mJpegCallback == null) {
            mJpegCallback = new PictureCallback() {

                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    Log.d(TAG, "On picture taken");
                    mPictureData = data;
                    setShutterRetake();
                }
            };
        }
        return mJpegCallback;
    }
    
    private void setShutterRetake() {
        mPictureTaken = true;
        mShutterButton.setImageDrawable(getResources().getDrawable(R.drawable.btn_shutter_retake));
        mAcceptButton.setVisibility(View.VISIBLE);
        mCancelButton.setVisibility(View.VISIBLE);
        mAcceptButton.bringToFront();
        mCancelButton.bringToFront();
        mSwitchCameraButton.setVisibility(View.GONE);
        mSwitchFlashButton.setVisibility(View.GONE);
    }
    
    private void resetShutter() {
        mPictureTaken = false;
        mPictureData = null;
        mShutterButton.setImageDrawable(getResources().getDrawable(R.drawable.btn_shutter));
        mAcceptButton.setVisibility(View.GONE);
        mCancelButton.setVisibility(View.GONE);
        
        mSwitchCameraButton.setVisibility(View.VISIBLE);
        mSwitchFlashButton.setVisibility(View.VISIBLE);
    }
    
    private OnClickListener getSwitchCameraListener() {
        mSwitchCameraListener = new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Camera switched");
                removeCameraPreviewView();
                releaseMediaRecorder();
                deinitializeCamera();
                
                switchCamera();
                CameraInitializer ci = new CameraInitializer();
                ci.execute();
            }
        };
        return mSwitchCameraListener;
    }
    
    private void removeCameraPreviewView() {
        if(supportsSDK(14)) {
            mPreviewLayout.removeView(mCameraPreviewAdvanced);
        } else {
            mPreviewLayout.removeView(mCameraPreview);
        }
        
    }
    
    private void switchCamera() {
        if (isBackCamera()) {
            mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
    }
    
    private boolean isBackCamera() {
        return (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK);
    }
    
    private boolean isFrontCamera() {
        return (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK);
    }
    
    private OnClickListener getSwitchFlashListener() {
        mSwitchFlashListener = new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Flash toggled");
                mFlashEnabled = !mFlashEnabled;
                setCameraParameters();
            }
        };
        return mSwitchFlashListener;
    }
    
    public void prepareMediaRecorder() {
        Log.d(TAG, "Called prepare media recorder");
        System.out.println(mCamera + " / " + mCameraPreviewAdvanced);
        mMediaRecorder = new MediaRecorder();
        if(mCamera == null) {
            mCamera = getCameraInstance();
        }
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        
        mMediaRecorder.setProfile(CamcorderProfile.get(mCurrentCameraId, CamcorderProfile.QUALITY_HIGH));
        //mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        //mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
        //mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        
        mMediaRecorder.setOutputFile(getFile().getAbsolutePath());
        //mMediaRecorder.setMaxDuration(MAX_VIDEO_DURATION);
        //mMediaRecorder.setMaxFileSize(MAX_FILE_SIZE);
        
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
    
    public void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.lock();
        }
    }
    
    private void startMediaRecorder() {
        Log.d(TAG, "Called start media recorder");
        if (mMediaRecorder != null) {
            mMediaRecorder.start();
        }
        
        /*if (mIsRecording) {
            mMediaRecorder.stop();
            releaseMediaRecorder();
            finish();
        } else {
            Log.d(TAG, "Called start media recorder");
            mMediaRecorder.start();
            mIsRecording = true;
        }*/
    }
    
    private void stopMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
        }
    }
    
    @Override
    public void startRecorder() {
        MediarRecorderInitializer mri = new MediarRecorderInitializer();
        mri.execute();
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
    
    // This initializes the camera and starts a preview. This is used to switch the camera (back/front) faster
    private class CameraInitializer extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            initializeCamera();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            startPreview();
        }
    }
    
    // This sets up and starts the media recorder
    private class MediarRecorderInitializer extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            prepareMediaRecorder();
            startMediaRecorder();
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            stopMediaRecorder(); // We immediately stop the recorder because there is no need to record, we just need the preview
            
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
                CameraFragmentListener listener = (CameraFragmentListener) mContext; 
                listener.startupComplete();
        }
    }
}
