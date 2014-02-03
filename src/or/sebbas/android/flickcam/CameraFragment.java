package or.sebbas.android.flickcam;

import java.io.File;
import java.io.IOException;

import or.sebbas.android.flickcam.CameraPreview.CameraPreviewListener;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class CameraFragment extends Fragment implements CameraPreviewListener {

    private static final String TAG = "camera_fragment";
    private static final int MAX_VIDEO_DURATION = 60000;
    private static final long MAX_FILE_SIZE = 500000;
    private static final String VIDEO_PATH_NAME = "/Pictures/test.3gp";
    
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
    private ViewGroup mRelativeLayoutMask;
    private CameraPreview mCameraPreview;
    private ImageButton mShutterButton;
    private ImageButton mSwitchCameraButton;
    private ImageButton mSwitchFlashButton;
    private LayoutInflater mControlInflater;
    private OnClickListener mSwitchFlashListener;
    private OnClickListener mSwitchCameraListener;
    private OnClickListener mShutterListener;
    private MediaRecorder mMediaRecorder;
    private CameraPreviewListener mCameraPreviewListener;
    private View mRootView;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.camera_layout, container, false);
        initParameters();
        initializeCamera();
        startPreview();
        
        return mRootView;
    }
    
    private void initParameters() {
        mContext = this.getActivity();
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
        mRelativeLayoutMask.removeAllViews();
        mCameraPreview = null;
        Log.d(TAG, "Preview Stopped");
    }
    
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
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
        
        // Set the current zoom value if we didnt use smooth zoom
        if (!mSmoothZoomSupported) {
            mParameters.setZoom(mZoomValue);
        }
        Size preferedSize = mParameters.getPreferredPreviewSizeForVideo();
        mParameters.setPreviewSize(preferedSize.width, preferedSize.height);
        
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
    
    private void startPreview() {
        if (mCamera != null) {
            if (mCameraPreview == null) {
                
                FrameLayout preview = (FrameLayout) mRootView.findViewById(R.id.camera_preview);
                mRelativeLayoutMask = new RelativeLayout(mContext);
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                        new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                
                // Add RL to FL
                preview.addView(mRelativeLayoutMask, lp);
                
                // Add the SurfaceView to the RL
                mCameraPreview = new CameraPreview(mContext, mCameraPreviewListener, mCamera);
                mRelativeLayoutMask.addView(mCameraPreview, lp);
                
                mControlInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View viewcontrol = mControlInflater.inflate(R.layout.camera_control, null);
                mRelativeLayoutMask.addView(viewcontrol, lp);
                
                mShutterButton = (ImageButton) viewcontrol.findViewById(R.id.shutter_button);
                mShutterButton.setOnClickListener(getShutterListener());
                
                mSwitchCameraButton = (ImageButton) viewcontrol.findViewById(R.id.switch_camera);
                mSwitchCameraButton.setOnClickListener(getSwitchCameraListener());
                
                mSwitchFlashButton = (ImageButton) viewcontrol.findViewById(R.id.switch_flash);
                mSwitchFlashButton.setOnClickListener(getSwitchFlashListener());
                
                Log.d(TAG, "New Preview Started");
            } else {
                mCamera.startPreview();
            }
        }
    }
    
    private OnClickListener getShutterListener() {
        if (mShutterListener == null) {
            mShutterListener = new OnClickListener() {

                @Override
                public void onClick(View view) {
                    Log.d(TAG, "Shutter Button Clicked.");
                    /*if (mPictureTaken) {
                        //retakePicture();
                    } else {
                        //takePicture();
                    }*/
                }
            };
        }
        return mShutterListener;
    }
    
    private OnClickListener getSwitchCameraListener() {
        mSwitchCameraListener = new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Camera switched");
                releaseMediaRecorder();
                deinitializeCamera();
                switchCamera();
                initializeCamera();
                startPreview();
            }
        };
        return mSwitchCameraListener;
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
    
    @Override
    public void prepareMediaRecorder() {
        Log.d(TAG, "Called prepare media recorder");
        System.out.println(mCamera + " / " + mCameraPreview);
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
        mMediaRecorder.setMaxDuration(MAX_VIDEO_DURATION);
        mMediaRecorder.setMaxFileSize(MAX_FILE_SIZE);
        
        mMediaRecorder.setPreviewDisplay(mCameraPreview.getHolder().getSurface());
        
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
    
    @Override
    public void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.lock();
        }
    }
    
    @Override
    public void startMediaRecorder() {
        Log.d(TAG, "Called start media recorder");
        mMediaRecorder.start();
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
