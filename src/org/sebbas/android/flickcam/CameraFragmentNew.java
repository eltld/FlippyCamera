package org.sebbas.android.flickcam;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.sebbas.android.interfaces.CameraFragmentListener;
import org.sebbas.android.interfaces.CameraPreviewListener;
import org.sebbas.android.listener.DeviceOrientationListener;
import org.sebbas.android.views.CameraPreview;
import org.sebbas.android.views.CameraPreviewAdvanced;
import org.sebbas.android.views.OrientationImageButton;

import com.tekle.oss.android.animation.AnimationFactory;
import com.tekle.oss.android.animation.AnimationFactory.FlipDirection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class CameraFragmentNew extends Fragment implements CameraPreviewListener {

    private static final String VIDEO_PATH_NAME = "/FlickCam.mp4/";
    
    // Camera constants
    private static int CAMERA_ID_FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private static int CAMERA_ID_BACK = Camera.CameraInfo.CAMERA_FACING_BACK;
    private static final String NO_CAMERA_AVAILABLE = "No Camera found on your device";
    private static final int GALLERY_FRAGMENT_NUMBER = 2;
    private static final String ALBUM_NAME = "FlickCam";
    private static final int FLIP_PREVIEW_ANIMATION_DURATION = 300;
    public static final String TAG = "camera_fragment";
    
    // General instance variables
    private Context mContext;
    private Camera mCamera;
    private int mNumberOfCamerasSupported;
    private boolean mFlashEnabled;
    private int mCurrentCameraID;
    protected boolean mPictureTaken = false;
    private byte[] mPictureData;
    private int mDeviceRotation;
    private boolean mCameraWasSwapped;
    private boolean mIsRecording;
    
    // Camera Properties
    private int mZoomMax;
    private boolean mSmoothZoomSupported;
    private int mZoomValue;
    private boolean mVideoStabilizationSupported;
    
    // Media recorder
    private MediaRecorder mMediaRecorder;
    
    // UI instance variables
    private View mRootView;
    private ViewFlipper mCameraViewFlipper;
    private ViewPager mViewPager;
    private ImageButton mShutterButton;
    private OrientationImageButton mSwitchCameraButton;
    private OrientationImageButton mSwitchFlashButton;
    private OrientationImageButton mGalleryButton;
    private OrientationImageButton mSettingsButton;
    private OrientationImageButton mCancelButton;
    private OrientationImageButton mAcceptButton;
    private FrameLayout mControlLayout;
    private CameraPreviewAdvanced mCameraOnePreviewAdvanced;
    private CameraPreviewAdvanced mCameraTwoPreviewAdvanced;
    private CameraPreview mCameraOnePreview;
    private CameraPreview mCameraTwoPreview;
    
    // Listeners
    private CameraFragmentListener mCameraFragmentListener;
    private DeviceOrientationListener mOrientationEventListener;
    private OnClickListener mSwitchFlashListener;
    private OnClickListener mSwitchCameraListener;
    private OnClickListener mShutterListener;
    private OnClickListener mGalleryListener;
    private OnClickListener mSettingsListener;
    private OnClickListener mAcceptListener;
    private OnClickListener mCancelListener;
    
    // Callbacks
    private ErrorCallback mErrorCallback;
    private ShutterCallback mShutterCallback;
    private PictureCallback mRawCallback;
    private PictureCallback mPostViewCallback;
    private PictureCallback mJpegCallback;
    
    // Static factory method that returns a new fragment instance to the client
    public static CameraFragmentNew newInstance() {
        CameraFragmentNew cf = new CameraFragmentNew();
        return cf;
    }

    /* 
     * Methods for the Fragment life-cycle
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "ON CREATE VIEW");
        initializeInstanceVariables(inflater, container);
        setViewListeners(); // Maybe move this stuff to other place so that onCreateView becomes lighter and faster
        return mRootView;
    }

    @SuppressLint("NewApi")
    @Override
    public void onResume() {
        Log.d(TAG, "ON RESUME");
        super.onResume();
        
        CameraInitializer ci = new CameraInitializer();
        ci.execute();
    }
    
    @Override
    public void onPause() {
        Log.d(TAG, "ON PAUSE");
        super.onPause();
        
        deinitializeCamera();
        removeCameraPreviewView();
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "ON DESTROY");
        super.onDestroy();
        mOrientationEventListener.disable();
    }
    
    private boolean initializeCamera(int cameraID) {
        if (mNumberOfCamerasSupported == 0) {
            alertNoCamera();
            return false;
        } else {
            mCamera = getCameraInstance(cameraID);
            return true;
        }
    }

    private void alertNoCamera() {
        Toast.makeText(mContext, NO_CAMERA_AVAILABLE, Toast.LENGTH_LONG).show();
    }
    
    private Camera getCameraInstance(int cameraID) {
        Camera camera = null;
        try {
            camera = Camera.open(cameraID);
            camera.setErrorCallback(getErrorCallback());
            Log.d(TAG, "Camera Opened Successfully");
        } catch (RuntimeException e) {
            Log.d(TAG, "Failed to Open Camera - " + e.getMessage());
        }
        return camera; // returns null if camera is unavailable
    }
    
    private void initializeInstanceVariables(LayoutInflater inflater, ViewGroup container) {
        mContext = this.getActivity();
        // If the device has on screen buttons/ a navigation bar, then we use a layout with an extra bottom margin
        if (hasSoftButtons(mContext)) {
            mRootView = inflater.inflate(R.layout.camera_layout_with_navigationbar, container, false);
        } else {
            mRootView = inflater.inflate(R.layout.camera_layout_without_navigationbar, container, false);
        }
        
        mControlLayout = (FrameLayout) mRootView.findViewById(R.id.control_mask);
        
        mShutterButton = (ImageButton) mRootView.findViewById(R.id.shutter_button);
        mSwitchCameraButton = (OrientationImageButton) mRootView.findViewById(R.id.switch_camera);
        mSwitchFlashButton = (OrientationImageButton) mRootView.findViewById(R.id.switch_flash);
        mAcceptButton = (OrientationImageButton) mRootView.findViewById(R.id.accept_image);
        mCancelButton = (OrientationImageButton) mRootView.findViewById(R.id.discard_image);
        mGalleryButton = (OrientationImageButton) mRootView.findViewById(R.id.goto_gallery);
        mSettingsButton = (OrientationImageButton) mRootView.findViewById(R.id.settings_button);
        mViewPager = (ViewPager) getActivity().findViewById(R.id.viewpager);
        
        mCameraFragmentListener = (CameraFragmentListener) mContext;
        mNumberOfCamerasSupported = Camera.getNumberOfCameras();
        mCurrentCameraID = CAMERA_ID_BACK; // This is our default setup
        
        mOrientationEventListener = new DeviceOrientationListener(mContext);
        mOrientationEventListener.enable();
        
        mCameraViewFlipper = (ViewFlipper) mRootView.findViewById(R.id.camera_view_flipper);
        mDeviceRotation = getDeviceRotation(mContext);
        mCameraWasSwapped = false;
        mIsRecording = false;
    }
    
    @SuppressLint("NewApi")
    private void setCameraParameters() {
        Parameters parameters = mCamera.getParameters();
        // Adds continuous auto focus (only if API is high enough) to the parameters.
        if (supportsSDK(14)) {
            parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else {
            parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } 
        
        // Enable auto white balance. This makes preview look smoother
        parameters.setWhiteBalance(Parameters.WHITE_BALANCE_AUTO);
        
        if (mFlashEnabled) {
            parameters.setFlashMode(Parameters.FLASH_MODE_ON);
        } else {
            parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
        }
        
        // This will only get called if video stabilization is supported and if the API is high enough. All handled in initCameraProperties.
        if (mVideoStabilizationSupported && supportsSDK(15)) {
            parameters.setVideoStabilization(true);
        }
        
        // Set the current zoom value if we didn't use smooth zoom
        if (!mSmoothZoomSupported) {
            parameters.setZoom(mZoomValue);
        }
        
        parameters.setRotation(mDeviceRotation);
        // Finally, add the parameters to the camera
        mCamera.setParameters(parameters);
    }
    
    @SuppressLint("NewApi")
    private void initializeCameraProperties() {
        Parameters parameters = mCamera.getParameters();
        mZoomMax = parameters.getMaxZoom();
        mSmoothZoomSupported = parameters.isSmoothZoomSupported();
        if (supportsSDK(15)) {
            mVideoStabilizationSupported = parameters.isVideoStabilizationSupported();
        }
        mZoomValue = 0;
    }
    
    private void setViewListeners() {
        mShutterButton.setOnClickListener(getShutterListener());
        mSwitchCameraButton.setOnClickListener(getSwitchCameraListener());
        mSwitchFlashButton.setOnClickListener(getSwitchFlashListener());
        mGalleryButton.setOnClickListener(getSwitchToGalleryListener());
        mAcceptButton.setOnClickListener(getAcceptListener());
        mCancelButton.setOnClickListener(getCancelListener());
    }
    
    private void reorganizeUI() {
        mControlLayout.bringToFront();
        mControlLayout.invalidate();
    }
    
    @SuppressLint("NewApi")
    private void startPreview() {
        // If device supports API 14 then add a TextureView (better performance) to the RL, else add a SurfaceView (no other choice)
        if (mCurrentCameraID == CAMERA_ID_BACK) {
            System.out.println("Entered camera back");
            if (supportsSDK(14)) {
                mCameraOnePreviewAdvanced = new CameraPreviewAdvanced(mContext, this, mCamera);
                mCameraViewFlipper.addView(mCameraOnePreviewAdvanced);
            } else {
                mCameraOnePreview = new CameraPreview(mContext, this);
                mCameraViewFlipper.addView(mCameraOnePreview);
            }
        }
        if (mCurrentCameraID == CAMERA_ID_FRONT) {
            System.out.println("Entered camera front");
            if (supportsSDK(14)) {
                mCameraTwoPreviewAdvanced = new CameraPreviewAdvanced(mContext, this, mCamera);
                mCameraViewFlipper.addView(mCameraTwoPreviewAdvanced);
            } else {
                mCameraTwoPreview = new CameraPreview(mContext, this);
                mCameraViewFlipper.addView(mCameraTwoPreview);
            }
        }    
    }
    
    @Override
    public void performZoom(float scaleFactor) {
        mZoomValue = (int) ((scaleFactor - 1) * (mZoomMax + 1));
        if (mZoomValue > 0 && mZoomValue < mZoomMax) {
            if (mSmoothZoomSupported) {
                mCamera.startSmoothZoom(mZoomValue);
            } else {
                setCameraParameters(); // Just update the camera parameters. This will also set the new zoom level
            }
        }
    }
    
    /*
     * Setter and Getters
     */
    private void setCurrentCameraID(int cameraID) {
        mCurrentCameraID = cameraID;
    }
    
    private int getCurrentCameraID() {
        return mCurrentCameraID;
    }
    
    /*
     * Methods to stop and release the cameras
     */
    private void deinitializeCamera() {
        stopPreview();
        releaseCamera();
        
        Log.d(TAG, "Camera Initialization has Been Undone");
    }
    
    private void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }
    
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    /*
     * Listeners
     */
    private OnClickListener getSwitchFlashListener() {
        mSwitchFlashListener = new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                mFlashEnabled = !mFlashEnabled;
                setCameraParameters();
                setFlashIcon();
            }
        };
        return mSwitchFlashListener;
    }
    
    private OnClickListener getSwitchCameraListener() {
        mSwitchCameraListener = new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Camera switched");
                switchCamera();
            }
        };
        return mSwitchCameraListener;
    }
    
    private OnClickListener getSwitchToGalleryListener() {
        mGalleryListener = new OnClickListener() {

            @Override
            public void onClick(View view) {
                mViewPager.setCurrentItem(GALLERY_FRAGMENT_NUMBER);
            }
        };
        return mGalleryListener;
    }
    
    private OnClickListener getSettingsListener() {
       mSettingsListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                
            }
           
       };
       return mSettingsListener;
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
    
    private OnClickListener getAcceptListener() {
        if (mAcceptListener == null) {
            mAcceptListener = new OnClickListener() {

                @Override
                public void onClick(View view) {
                    Toast toast;
                    if (!isExternalStorageWritable()) {
                        toast = Toast.makeText(mContext, "Cannot save image. No storage available", Toast.LENGTH_LONG);
                        toast.show();
                        return;
                    }
                    
                    toast = Toast.makeText(mContext, "Saving Image...", Toast.LENGTH_LONG);
                    toast.show();

                    Log.d(TAG, "Accept Button Clicked.");
                    if (writeBytesToFile(mPictureData, getAlbumStorageDir() + "/" + getDefaultFilename())) {
                        toast = Toast.makeText(mContext, "Image Saved Successfully.", Toast.LENGTH_LONG);
                        // We have to refresh the grid view UI to make the new photo show up
                        refreshUI();
                        resetShutter();
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
    
    private OnClickListener getCancelListener() {
        if (mCancelListener == null) {
            mCancelListener = new OnClickListener() {

                @Override
                public void onClick(View v) {
                    resetShutter();
                }
            };
        }
        return mCancelListener;
    }
    
    /*
     * Callbacks
     */
    private Camera.ErrorCallback getErrorCallback() {
        if (mErrorCallback == null) {
            mErrorCallback = new Camera.ErrorCallback() {
                
                @Override
                public void onError(int error, Camera camera) {
                    if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
                        deinitializeCamera();
                        initializeCamera(mCurrentCameraID);
                    }
                }
            };
        }
        return mErrorCallback;
    }
    
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
    
    /*
     * Methods that handle UI events
     */
    private void refreshUI() {
        mCameraFragmentListener.refreshAdapter();
    }
    
    private void setFlashIcon() {
        if (mFlashEnabled) {
            mSwitchFlashButton.setImageResource(R.drawable.ic_action_flash_on);
        } else {
            mSwitchFlashButton.setImageResource(R.drawable.ic_action_flash_off);
        }
    }
    
    private void setShutterRetake() {
        mPictureTaken = true;
        mAcceptButton.setVisibility(View.VISIBLE);
        mCancelButton.setVisibility(View.VISIBLE);
        mAcceptButton.bringToFront();
        mCancelButton.bringToFront();
        mSettingsButton.setVisibility(View.GONE);
        mGalleryButton.setVisibility(View.GONE);
    }
    
    private void resetShutter() {
        mPictureTaken = false;
        mPictureData = null;
        mAcceptButton.setVisibility(View.GONE);
        mCancelButton.setVisibility(View.GONE);
        mSettingsButton.setVisibility(View.VISIBLE);
        mGalleryButton.setVisibility(View.VISIBLE);
    }
    
    private void removeCameraPreviewView() {
        if(supportsSDK(14)) {
            mCameraViewFlipper.removeView(mCameraOnePreviewAdvanced);
            mCameraViewFlipper.removeView(mCameraTwoPreviewAdvanced);
        } else {
            mCameraViewFlipper.removeView(mCameraOnePreview);
            mCameraViewFlipper.removeView(mCameraTwoPreview);
        }
    }
    
    private void startAnimation() {
        AnimationFactory.flipTransition(mCameraViewFlipper, FlipDirection.LEFT_RIGHT, FLIP_PREVIEW_ANIMATION_DURATION);
    }
    
    /*
     * Methods that handle the media recorder
     */
    @Override
    public void startRecorder() {
        MediarRecorderInitializer mri = new MediarRecorderInitializer();
        mri.execute();
        
    }
    
    private void prepareMediaRecorder() {
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        
        mMediaRecorder.setProfile(CamcorderProfile.get(mCurrentCameraID, CamcorderProfile.QUALITY_HIGH));
        //mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        //mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        //mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        //mMediaRecorder.setVideoSize(1280, 720);
        //mMediaRecorder.setVideoFrameRate(30);
        
        mMediaRecorder.setOutputFile(getPipeFD()/*getFile().getAbsolutePath()*/);
        //mMediaRecorder.setMaxDuration(MAX_VIDEO_DURATION);
        //mMediaRecorder.setMaxFileSize(MAX_FILE_SIZE);
        
        // We have to set the preview display for devices that use a SurfaceView
        if(!supportsSDK(14)) {
            setPreviewDisplayForMediaRecorder();
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
    
    private void setPreviewDisplayForMediaRecorder() {
        if (mCurrentCameraID == CAMERA_ID_BACK) {
            mMediaRecorder.setPreviewDisplay(mCameraOnePreview.getHolder().getSurface());
        } else if (mCurrentCameraID == CAMERA_ID_FRONT){
            mMediaRecorder.setPreviewDisplay(mCameraTwoPreview.getHolder().getSurface());
        }
    }
    
    private void startMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.start();
            mIsRecording = true;
            Log.d(TAG, "Start media recorder successful");
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
    
    private void stopMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mIsRecording = false;
        }
    }
    
    private void resetMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
        }
    }
    
    /*
     * Methods that handle camera functionalities
     */
    private void takePicture() {
        mCamera.takePicture(getShutterCallback(), getRawCallback(), getPostViewCallback(), getJpegCallback());
    }

    private void retakePicture() {
        resetShutter();
        removeCameraPreviewView();
        startPreview();
    }
    
    private boolean writeBytesToFile(byte[] data, String filename) {
        if (data == null) {
            Log.d(TAG, "Data Was Empty, Not Writing to File");
            return false;
        }
        Log.d(TAG, "Filename is " + filename);
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
    
    private String getDefaultFilename() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return "IMG_" + timeStamp + ".jpeg";
    }
    
    private File getAlbumStorageDir() {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), ALBUM_NAME);
        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }
    
    private void switchCamera() {
        deinitializeCamera();
        if (mCurrentCameraID == CAMERA_ID_BACK) {
            mCurrentCameraID = CAMERA_ID_FRONT;
        } else if (mCurrentCameraID == CAMERA_ID_FRONT) {
            mCurrentCameraID = CAMERA_ID_BACK;
        }
        mCameraWasSwapped = true; // TODO Using a boolean for this is very hacky. Maybe later a better implementation?
        CameraInitializer ci = new CameraInitializer();
        ci.execute();
        
    }
    
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
    
    private FileDescriptor getPipeFD() {
        FileDescriptor outputPipe = null;
        try {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            outputPipe = pipe[1].getFileDescriptor();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return outputPipe;
    }
    
    /*
     * Async Helper classes
     */
    // This initializes the camera and starts a preview.
    private class CameraInitializer extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            if (initializeCamera(mCurrentCameraID)) {
                
                initializeCameraProperties();
                setCameraParameters();
                setCameraDisplayOrientation(mContext, mCurrentCameraID, mCamera);
                System.out.println("done initializing");
            } 
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            startPreview();
            reorganizeUI();
            if (mCameraWasSwapped) {
                startAnimation();
                mCameraWasSwapped = false;
            }
        }
    }
    
    // This sets up and starts the media recorder
    private class MediarRecorderInitializer extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            
            prepareMediaRecorder();
            startMediaRecorder();
            resetMediaRecorder(); // Since there is no need to record now
            /*try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            stopMediaRecorder(); // We immediately stop the recorder because there is no need to record, we just need the preview
            */
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
                mCameraFragmentListener.startupComplete();
        }
    }
    
    /*
     * Static methods
     */
    private static void setCameraDisplayOrientation(Context context,
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
        
    // Check to see if the device supports the indicated SDK
    private static boolean supportsSDK(int sdk) {
        if (android.os.Build.VERSION.SDK_INT >= sdk) {
            return true;
        } 
        return false;
    }
    
     // Check if the device has soft buttons
    @SuppressLint("NewApi")
    private static boolean hasSoftButtons(Context context) {
        if (supportsSDK(14)) {
            return !ViewConfiguration.get(context).hasPermanentMenuKey();
        }
        return false;
    }
    
    private static int getDeviceRotation(Context context) {
        int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 90; break;
            case Surface.ROTATION_90: degrees = 180; break;
            case Surface.ROTATION_180: degrees = 270; break;
            case Surface.ROTATION_270: degrees = 0; break;
        }
        return degrees;
    }
    
    
    /* 
     * Very likely to be used later
     */
    
    private String getDefaultSavePath() {
        createSavingDirectory();
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/FlickCam/" + mContext.getPackageName() + "/";
    }
    
    private void createSavingDirectory() {
        File sdcard = Environment.getExternalStorageDirectory();
        File dir = new File(sdcard.getAbsoluteFile() + "/FlickCam/" + mContext.getPackageName() + "/");
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
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