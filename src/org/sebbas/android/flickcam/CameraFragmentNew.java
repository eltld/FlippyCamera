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
    private static final int FLIP_PREVIEW_ANIMATION_DURATION = 250;
    public static final String TAG = "camera_fragment";
    
    // General instance variables
    private Context mContext;
    private Camera mCameraOne;
    private Camera mCameraTwo;
    private int mNumberOfCamerasSupported;
    private int mNumberOfCamerasFound;
    private boolean mFlashEnabled;
    private Camera mCurrentCamera;
    private int mCurrentCameraID;
    protected boolean mPictureTaken = false;
    private byte[] mPictureData;
    private int mDeviceRotation;
    
    // Camera Properties
    private int mZoomMax;
    private boolean mSmoothZoomSupported;
    private int mZoomValue;
    private boolean mVideoStabilizationSupported;
    
    // Media recorders
    private MediaRecorder mMediaRecorderOne;
    private MediaRecorder mMediaRecorderTwo;
    
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
    
    private boolean initializeCamerasAndMediaRecorders() {
        if (mNumberOfCamerasSupported == 0) {
            alertNoCamera();
        }
        if (mNumberOfCamerasSupported >= 1) {
            mCameraOne = getCameraInstance(CAMERA_ID_BACK);
            if (mCameraOne != null) mNumberOfCamerasFound++; // Increase # cameras because camera was found
            mMediaRecorderOne = new MediaRecorder(); // Instantiate according MediaRecorder
        }
        if (mNumberOfCamerasSupported >= 2) {
            mCameraTwo = getCameraInstance(CAMERA_ID_FRONT);
            if (mCameraTwo != null) mNumberOfCamerasFound++; // Increase # cameras because camera was found
            mMediaRecorderTwo = new MediaRecorder(); // Instantiate according MediaRecorder
        }
        if (mNumberOfCamerasFound > 0) {
            return true;
        }
        return false;
    }
    
    private void setDefaultCamera() {
        // The default camera is the camera whose preview is shown first
        mCurrentCamera = mCameraOne; 
        mCurrentCameraID = CAMERA_ID_BACK;
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
        
        mOrientationEventListener = new DeviceOrientationListener(mContext);
        mOrientationEventListener.enable();
        
        mCameraViewFlipper = (ViewFlipper) mRootView.findViewById(R.id.camera_view_flipper);
        mDeviceRotation = getDeviceRotation(mContext);
    }
    
    @SuppressLint("NewApi")
    private void setCameraParameters(Camera camera) {
        System.out.println(mCameraOne + " / " + mCameraTwo);
        Parameters parameters = camera.getParameters();
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
        camera.setParameters(parameters);
    }
    
    @SuppressLint("NewApi")
    private void initializeCameraProperties(Camera camera) {
        Parameters parameters = camera.getParameters();
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
    private void startPreviews() {
        // If device supports API 14 then add a TextureView (better performance) to the RL, else add a SurfaceView (no other choice)
        if (mCameraOne != null) {
            if (supportsSDK(14)) {
                mCameraOnePreviewAdvanced = new CameraPreviewAdvanced(mContext, this, mCameraOne);
                mCameraViewFlipper.addView(mCameraOnePreviewAdvanced);
            } else {
                mCameraOnePreview = new CameraPreview(mContext, this, mCameraOne);
                mCameraViewFlipper.addView(mCameraOnePreview);
            }
        }
        if (mCameraOne != null) {
            if (supportsSDK(14)) {
                mCameraTwoPreviewAdvanced = new CameraPreviewAdvanced(mContext, this, mCameraOne);
                mCameraViewFlipper.addView(mCameraTwoPreviewAdvanced);
            } else {
                mCameraTwoPreview = new CameraPreview(mContext, this, mCameraTwo);
                mCameraViewFlipper.addView(mCameraTwoPreview);
            }
        }    
    }
    
    @Override
    public void performZoom(Camera camera, float scaleFactor) {
        mZoomValue = (int) ((scaleFactor - 1) * (mZoomMax + 1));
        if (mZoomValue > 0 && mZoomValue < mZoomMax) {
            if (mSmoothZoomSupported) {
                camera.startSmoothZoom(mZoomValue);
            } else {
                setCameraParameters(camera); // Just update the camera parameters. This will also set the new zoom level
            }
        }
    }

    /*
     *  Setters and Getters
     */
    private void setCurrentCamera(Camera camera) {
        mCurrentCamera = camera;
    }
    
    private Camera getCurrentCamera() {
        return mCurrentCamera;
    }
    
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
        stopPreviews();
        releaseCamera();
        
        Log.d(TAG, "Camera Initialization has Been Undone");
    }
    
    private void stopPreviews() {
        if (mCameraOne != null) {
            mCameraOne.stopPreview();
        }
        if (mCameraTwo != null) {
            mCameraTwo.stopPreview();
        }
    }
    
    private void releaseCamera() {
        if (mCameraOne != null) {
            mCameraOne.release();
            mCameraOne = null;
        }
        if (mCameraTwo != null) {
            mCameraTwo.release();
            mCameraTwo = null;
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
                setCameraParameters(mCurrentCamera);
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
                switchCameras();
                AnimationFactory.flipTransition(mCameraViewFlipper, FlipDirection.LEFT_RIGHT, FLIP_PREVIEW_ANIMATION_DURATION);
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
                        takePicture(mCurrentCamera);
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
                        initializeCamerasAndMediaRecorders();
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
    
    /*
     * Methods that handle the media recorder
     */
    @Override
    public void startRecorder(Camera camera) {
        MediarRecorderInitializer mri = new MediarRecorderInitializer();
        MediaRecorder recorderForCamera = mapCameraToPreview(camera);
        mri.execute(recorderForCamera, camera);
    }
    
    private void prepareMediaRecorder(MediaRecorder mediaRecorder, Camera camera) {
        camera.unlock();
        mediaRecorder.setCamera(camera);
        
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        
        mediaRecorder.setProfile(CamcorderProfile.get(mCurrentCameraID, CamcorderProfile.QUALITY_HIGH));
        //mMediaRecorder.setVideoSize(1280, 720);
        //mMediaRecorder.setVideoFrameRate(30);
        
        mediaRecorder.setOutputFile(getPipeFD()/*getFile().getAbsolutePath()*/);
        //mMediaRecorder.setMaxDuration(MAX_VIDEO_DURATION);
        //mMediaRecorder.setMaxFileSize(MAX_FILE_SIZE);
        
        // We have to set the preview display for devices that use a SurfaceView
        if(!supportsSDK(14)) {
            setPreviewDisplayForMediaRecorder(mediaRecorder);
        }
        
        try {
            mediaRecorder.prepare();
            Log.d(TAG, "Prepare media recorder successful");
        } catch (IllegalStateException e) {
            releaseMediaRecorder(mediaRecorder, camera);
            Log.d(TAG, "Failed to prepare media recorder- IllegalStateException");
        } catch (IOException e) {
            releaseMediaRecorder(mediaRecorder, camera);
            Log.d(TAG, "Failed to prepare media recorder. IOException");
        }
    }
    
    private void setPreviewDisplayForMediaRecorder(MediaRecorder mediaRecorder) {
        if (mCurrentCamera == mCameraOne) {
            mediaRecorder.setPreviewDisplay(mCameraOnePreview.getHolder().getSurface());
        } else {
            mediaRecorder.setPreviewDisplay(mCameraTwoPreview.getHolder().getSurface());
        }
    }
    
    private void startMediaRecorder(MediaRecorder mediaRecorder) {
        if (mediaRecorder != null) {
            mediaRecorder.start();
            Log.d(TAG, "Started media recorder successfully");
        }
    }
    
    public void releaseMediaRecorder(MediaRecorder mediaRecorder, Camera camera) {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            camera.lock();
        }
    }
    
    private void stopMediaRecorder(MediaRecorder mediaRecorder) {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
        }
    }
    
    /*
     * Methods that handle camera functionalities
     */
    private void takePicture(Camera camera) {
        camera.takePicture(getShutterCallback(), getRawCallback(), getPostViewCallback(), getJpegCallback());
    }

    private void retakePicture() {
        resetShutter();
        removeCameraPreviewView();
        startPreviews();
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
    
    public File getAlbumStorageDir() {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), ALBUM_NAME);
        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
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
            // TODO Auto-generated catch block
            Log.e(TAG, e.getMessage());
        }
        return outputPipe;
    }
    
    private void switchCameras() {
        if (mCurrentCamera == mCameraOne) {
            setCurrentCamera(mCameraTwo);
            setCurrentCameraID(CAMERA_ID_FRONT);
        } else if (mCurrentCamera == mCameraTwo) {
            setCurrentCamera(mCameraOne);
            setCurrentCameraID(CAMERA_ID_BACK);
        }
    }

    private MediaRecorder mapCameraToPreview(Camera camera) {
        if (camera == mCameraOne) {
            return mMediaRecorderOne;
        } else if (camera == mCameraTwo) {
            return mMediaRecorderTwo;
        }
        Log.d(TAG, "No media recorder found for camera");
        return null;
    }
    
    /*
     * Async Helper classes
     */
    // This initializes the camera and starts a preview.
    private class CameraInitializer extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            if (initializeCamerasAndMediaRecorders()) {
                
                setDefaultCamera();
                // We have to call this every time we switch the camera
                initializeCameraProperties(getCurrentCamera());
                
                if (mNumberOfCamerasFound >= 1) {
                    setCameraParameters(mCameraOne);
                    setCameraDisplayOrientation(mContext, CAMERA_ID_BACK, mCameraOne);
                }
                if (mNumberOfCamerasFound >= 2) {
                    setCameraParameters(mCameraTwo);
                    setCameraDisplayOrientation(mContext, CAMERA_ID_FRONT, mCameraTwo);
                }
            } 
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            startPreviews();
            reorganizeUI();
        }
    }
    
    // This sets up and starts the media recorder
    private class MediarRecorderInitializer extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            MediaRecorder mediaRecorder = (MediaRecorder) params[0];
            Camera camera = (Camera) params[1];
            
            prepareMediaRecorder(mediaRecorder, camera);
            System.out.println("mediarecorder is " + mMediaRecorderOne + " / camera is " + camera);
            startMediaRecorder(mediaRecorder);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            stopMediaRecorder(mediaRecorder); // We immediately stop the recorder because there is no need to record, we just need the preview
            
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
                mCameraFragmentListener.startupComplete();
        }
    }
    
    /*
     * Static class methods
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
