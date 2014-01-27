package org.sebbas.android.flickcam;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class CameraActivity extends Activity {
    
    private static final String TAG = "FlickCam";
    private static final String CAMERA_THREAD = "Camera Thread";
    private static final int PHOTO_HEIGHT_THRESHOLD = 1280;
    private static final String VIDEO_PATH_NAME = "/Pictures/test.3gp";
    private static final int MAX_VIDEO_DURATION = 60000;
    private static final int MAX_FILE_SIZE = 500000;
    
    private Camera mCamera;
    private CameraPreview mCameraPreview;
    
    // Default shutter sound
    private final ShutterCallback mShutterCallback = new ShutterCallback() {
        public void onShutter() {
            AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            mgr.playSoundEffect(AudioManager.FLAG_PLAY_SOUND);
        }
    };
    
    private PictureCallback mRawCallback;
    private PictureCallback mPostViewCallback;
    private PictureCallback mJpegCallback;
    
    private OnClickListener mShutterListener;
    private OnClickListener mCancelListener;
    private OnClickListener mAcceptListener;
    private OnClickListener mSwitchCameraListener;
    private OnClickListener mSwitchFlashListener;
    
    // Instance variables for the UI
    private ImageButton mAcceptButton;
    private ImageButton mShutterButton;
    private ImageButton mCancelButton;
    private ImageButton mSwitchCameraButton;
    private ImageButton mSwitchFlashButton;
    private RelativeLayout mRelativeLayoutMask;
    
    private ErrorCallback mErrorCallback;
    
    private boolean mPictureTaken = false;
    private byte[] mPictureData;
    
    private LayoutInflater mControlInflater = null;
    
    // Camera attributes
    private int mZoomValue; // The current zoom level
    private boolean mSmoothZoomSupported = false;
    private int mZoomMax;    private Parameters mParameters;
    private boolean mVideoStabilizationSupported = false;
    private int mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private boolean mFlashEnabled = false;
    
    // Members for the UI Handling
    private CameraHandlerThread mCameraThread = null;
    
    // Instance variables for the MediaRecorder
    private MediaRecorder mMediaRecorder;
    private boolean mIsRecording = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(TAG, "ON CREATE");
        setContentView(R.layout.activity_camera);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();
        deinitializeCamera();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.w(TAG, "ON RESUME");
        //startCameraThread();
        
        //initializeCamera();
        //mCamera = getCameraInstance();
        //startPreview();
        //startRecorder();
    }
    
    @Override
	protected void onStart() {
        super.onStart();
        Log.w(TAG, "ON START");
        initializeCamera();
        startPreview();
	}

    private void startCameraThread() {
        if (mCameraThread == null) {
            Log.d(TAG, "Started Camera Thread");
            mCameraThread = new CameraHandlerThread(CAMERA_THREAD);
        }
        synchronized(mCameraThread) {
            mCameraThread.openCamera();
            startPreview();
        }
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
    
    @SuppressLint("NewApi")
    private void setCameraParameters() {
        System.out.println("setCameraParameters called");
        // Set custom picture size. Makes camera preview run smoother
        //float defaultCameraRatio = (float) mParameters.getPictureSize().width / (float) mParameters.getPictureSize().height;
        //Size preferedPicturesSize = getPreferredPictureSize(defaultCameraRatio);
        //System.out.println(mParameters.getPictureSize().width +"/"+ mParameters.getPictureSize().height);
        //mParameters.setPictureSize(preferedPicturesSize.width, preferedPicturesSize.height);
        //mParameters.setPictureSize(640, 480);
        
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
        System.out.println(listToString(mParameters.getSupportedPreviewFpsRange()));
        Size preferedSize = mParameters.getPreferredPreviewSizeForVideo();
        mParameters.setPreviewSize(preferedSize.width, preferedSize.height);
        
        // Finally, add the parameters to the camera
        mCamera.setParameters(mParameters);  
    }
    
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
    
    private Size getPreferredPictureSize(float defaultCameraRatio) {
        Size res = null;
        List<Size> sizes = mCamera.getParameters().getSupportedPictureSizes();
                
        //System.out.println("Sizes are: " + listToString(sizes));
        for (Size s : sizes) {
            float ratio = (float) s.width / (float) s.height;
            if (ratio == defaultCameraRatio && s.height <= PHOTO_HEIGHT_THRESHOLD) {
                res = s;
                break;
            }
        }
        return res;
    }
    
    /*public static String listToString(List<Size> sizes) {
        String result = "[";
        for (int i = 0; i < sizes.size(); i++) {
            Size  a = sizes.get(i);
            int width = a.width;
            int height = a.height;
            result += "(" + width + " / " + height + ")";
            
        } 
        return result += "]";
    }*/
    
    public static String listToString(List<int[]> sizes) {
        String result = "[";
        for (int[] a : sizes) {
            result += "(";
            for (int i = 0; i < a.length; i++) {
                result += a[i];
                if (i != a.length-1) result += ", ";
            }
            result += ")";
        }
        return result += "]";
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
    
    private void startPreview() {
        if (mCamera != null) {
            if (mCameraPreview == null) {
                // Create Layouts and params
                FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
                mRelativeLayoutMask = new RelativeLayout(this);
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                        new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                
                // Add RL to FL
                preview.addView(mRelativeLayoutMask, lp);
                
                // Add the SurfaceView to the RL
                mCameraPreview = new CameraPreview(this, mCamera);
                mRelativeLayoutMask.addView(mCameraPreview, lp);
                
                mControlInflater = LayoutInflater.from(getBaseContext());
                View viewcontrol = mControlInflater.inflate(R.layout.camera_control, null);
                mRelativeLayoutMask.addView(viewcontrol, lp);
                
                mShutterButton = (ImageButton)findViewById(R.id.shutter_button);
                mShutterButton.setOnClickListener(getShutterListener());
                
                mSwitchCameraButton = (ImageButton)findViewById(R.id.switch_camera);
                mSwitchCameraButton.setOnClickListener(getSwitchCameraListener());
                
                mSwitchFlashButton = (ImageButton)findViewById(R.id.switch_flash);
                mSwitchFlashButton.setOnClickListener(getSwitchFlashListener());
                
                Log.d(TAG, "New Preview Started");
            } else {
                mCamera.startPreview();
            }
        }
    }

    public void startRecorder() {
        if (mIsRecording) {
            mMediaRecorder.stop();
            releaseMediaRecorder();
            finish();
        } else {
            Log.d(TAG, "Called start media recorder");
            mMediaRecorder.start();
            mIsRecording = true;
        }
    }
    
    private void sleep() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
        mRelativeLayoutMask.removeAllViews();
        mCameraPreview = null;
        mCameraThread = null;
        Log.d(TAG, "Preview Stopped");
    }

    private OnClickListener getAcceptListener() {
        if (mAcceptListener == null) {
            mAcceptListener = new OnClickListener() {

                @Override
                public void onClick(View view) {
                    Toast toast = Toast.makeText(CameraActivity.this, "Saving Image...", Toast.LENGTH_LONG);
                    toast.show();

                    Log.d(TAG, "Accept Button Clicked.");
                    if (writeBytesToFile(mPictureData, getDefaultSavePath() + getDefaultFilename())) {
                        toast = Toast.makeText(CameraActivity.this, "Image Saved Successfully.", Toast.LENGTH_LONG);
                    }
                    else {
                        toast = Toast.makeText(CameraActivity.this, "Failed to Save Image. See Log for Details.", Toast.LENGTH_LONG);
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
                public void onClick(View view) {
                    Log.d(TAG, "Cancel Button Clicked.");
                    retakePicture();
                }
            };
        }
        return mCancelListener;
    }

    private OnClickListener getShutterListener() {
        if (mShutterListener == null) {
            mShutterListener = new OnClickListener() {

                @Override
                public void onClick(View view) {
                    Log.d(TAG, "Shutter Button Clicked.");
                    Toast.makeText(getApplicationContext(), "Clicked shutter", Toast.LENGTH_SHORT).show();
                    if (mPictureTaken) {
                        //retakePicture();
                    } else {
                        //takePicture();
                    }
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
                deinitializeCamera();
                if (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                } else {
                    mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                }
                startCameraThread();
            }
        };
        return mSwitchCameraListener;
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

    private void resetShutter() {
        mPictureTaken = false;
        mPictureData = null;
        mShutterButton.setImageDrawable(getResources().getDrawable(R.drawable.btn_shutter));
        mAcceptButton.setVisibility(View.GONE);
        mCancelButton.setVisibility(View.GONE);
    }

    private void setShutterRetake() {
        mPictureTaken = true;
        mShutterButton.setImageDrawable(getResources().getDrawable(R.drawable.btn_shutter_retake));
        mAcceptButton.setVisibility(View.VISIBLE);
        mCancelButton.setVisibility(View.VISIBLE);
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
        if (mJpegCallback == null) {
            mJpegCallback = new PictureCallback() {

                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    mPictureData = data;
                    setShutterRetake();
                }
            };
        }
        return mJpegCallback;
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

    private String getDefaultFilename() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return "IMG_" + timeStamp + ".jpeg";
    }

    private String getDefaultSavePath() {
        createSavingDirectory();
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + getPackageName() + "/";
    }

    private void createSavingDirectory() {
        File sdcard = Environment.getExternalStorageDirectory();
        File dir = new File(sdcard.getAbsoluteFile() + "/Android/data/" + getPackageName() + "/");
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
    }
    
    private void takePicture() {
        mCamera.takePicture(getShutterCallback(), getRawCallback(), getPostViewCallback(), getJpegCallback());
    }

    private void retakePicture() {
        resetShutter();
        startPreview();
    }

    private boolean hasCamera(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            Log.d(TAG, "At Least one Camera Detected!");
            return true;
        } else {
            // no camera on this device
            Log.d(TAG, "No Cameras detected.");
            return false;
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
            Log.d(TAG, "Camera Released");
        }
    }
    
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
        
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        mMediaRecorder.setOutputFile(getFile().getAbsolutePath());
        mMediaRecorder.setMaxDuration(MAX_VIDEO_DURATION);
        mMediaRecorder.setMaxFileSize(MAX_FILE_SIZE);
        //mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        
        //mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        //mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        mMediaRecorder.setPreviewDisplay(mCameraPreview.getHolder().getSurface());
        
        
        try {
            mMediaRecorder.prepare();
            Log.d(TAG, "Started recording");
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
    
    private class CameraHandlerThread extends HandlerThread {
        private Handler mHandler = null;
        
        public CameraHandlerThread(String name) {
            super(name);
            start();
            mHandler = new Handler(getLooper());
        }
        
        synchronized void notifyCameraOpened() {
            notify();
        }
        
        void openCamera() {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    initializeCamera();
                    notifyCameraOpened();
                }
                
            });
            try {
                wait();
            } catch (InterruptedException ie){
                Log.w(TAG, "Wait was interrutpted");
            }
        }
    }
    
    private class CameraSetup extends AsyncTask<Void, Void, Void> {
    	
        @Override
        protected Void doInBackground(Void... arg0) {
            
            
            return null;
        }
        
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        
        
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            
        }
    }
}
