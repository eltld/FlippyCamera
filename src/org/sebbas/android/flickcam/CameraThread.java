package org.sebbas.android.flickcam;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.sebbas.android.helper.DeviceInfo;
import org.sebbas.android.listener.CameraThreadListener;
import org.sebbas.android.views.CameraPreview;
import org.sebbas.android.views.Flasher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

public class CameraThread extends Thread {

    // Private constants
    private static final String TAG = "camera_thread";
    public static final int CAMERA_ID_BACK = Camera.CameraInfo.CAMERA_FACING_BACK;
    public static final int CAMERA_ID_FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT;
    
    protected static final String COULD_NOT_INITIALIZE_CAMERA = "Could not initialize camera";
    protected static final String NO_CAMERAS_FOUND = "No cameras found on device";
    protected static final String CANNOT_CONNECT_TO_CAMERA = "Cannot connect to camera";
    protected static final String NO_STORAGE_AVAILABLE = "No storage available on this device";
    protected static final String FAILED_TO_SAVE_PICTURE = "Failed to save picture";
    protected static final String IS_SAVING_PICTURE = "Saving your picture ...";
    protected static final String SAVED_PICTURE_SUCCESSFULLY = "Picture saved successfully!";
    private static final String ALBUM_NAME = "FlickCam";
    private static String VIDEO_PATH_NAME = "/FlickCam.mp4";
    
    // Private instance variables
    private Context mContext;
    private Handler mHandler;
    private Camera mCamera;
    private CameraThreadListener mCameraThreadListener;
    protected int mNumberOfCamerasSupported;
    protected int mCurrentCameraID;
    private boolean mVideoStabilizationSupported;
    private boolean mSmoothZoomSupported;
    protected boolean mAutoFocusSupported;
    protected boolean mFlashSupported;
    protected boolean mWhiteBalanceSupported;
    protected int mZoomMax;
    private int mZoomValue;
    protected boolean mFlashEnabled;
    private MediaRecorder mMediaRecorder;
    private List<Camera.Size> mSupportedPreviewSizes;
    private Camera.Size mPreviewSize;
    protected byte[] mPictureData;
    private ArrayList<Camera.Area> mFocusList;
    private String mCurrentEffect;
    private List<String> mSupportedColorEffects;
    
    // Callbacks
    private ErrorCallback mErrorCallback;
    private ShutterCallback mShutterCallback;
    private PictureCallback mRawCallback;
    private PictureCallback mPostViewCallback;
    private PictureCallback mJpegCallback;
    private PreviewCallback mPreviewCallback;
    
    private PictureTakerThread mPictureTakerThread;
    private PictureWriterThread mPictureWriterThread;
    
    // Sound variables
    private boolean mSoundLoaded;
    private SoundPool mSoundPool;
    private int mSoundId;
    
    public CameraThread(CameraFragmentUI cameraFragment, Context context) {
        mContext = context;
        mCameraThreadListener = (CameraThreadListener) cameraFragment;
        mNumberOfCamerasSupported = Camera.getNumberOfCameras();
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
                Log.e(TAG, "Camera thread halted due to an error", t);
            }
        }
        Looper.loop();
    }
    
    private synchronized Handler getHandler() {
        while (mHandler == null) {
            try {
                this.wait();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
        return mHandler;
    }
    
    // Public methods
    public synchronized void quitThread() {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                Log.i(TAG, "Camera thread loop quitting by request");
                //releasePreview(); // Is this really needed
                deinitializeCamera();
                
                Looper.myLooper().quit();
            }
            
        });
    }
    
    public synchronized void stopCamera() {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                deinitializeCamera();
            }
            
        });
    }
    
    public synchronized void initializeCamera(final int cameraID) {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                try {
                    if (mNumberOfCamerasSupported == 0) {
                        Log.e(TAG, NO_CAMERAS_FOUND);
                        mCameraThreadListener.alertCameraThread(NO_CAMERAS_FOUND);
                    } else {
                        mCamera = getCameraInstance(cameraID); // Setup camera object
                        //setCameraSound(); // Initialize camera sound
                        
                        // If camera not null the set the camera id
                        if (mCamera == null) {
                            Log.e(TAG, CANNOT_CONNECT_TO_CAMERA);
                            mCameraThreadListener.alertCameraThread(CANNOT_CONNECT_TO_CAMERA);
                        } else {
                            mCurrentCameraID = cameraID;
                        }
                    }
                } catch (Throwable t) {
                    Log.e(TAG, COULD_NOT_INITIALIZE_CAMERA, t);
                    mCameraThreadListener.alertCameraThread(COULD_NOT_INITIALIZE_CAMERA);
                }
            }
            
        });
    }
    
    public synchronized void initializeCameraProperties() {
        getHandler().post(new Runnable() {

            @SuppressLint("NewApi")
            @Override
            public void run() {
                if (mCamera != null) {
                    Parameters parameters = mCamera.getParameters();
                    mZoomMax = parameters.getMaxZoom();
                    mZoomValue = 0;
                    mSmoothZoomSupported = parameters.isSmoothZoomSupported();
                    mAutoFocusSupported = DeviceInfo.supportsAutoFocus(parameters);
                    mFlashSupported = DeviceInfo.supportsFlash(parameters);
                    mWhiteBalanceSupported = DeviceInfo.supportsWhiteBalance(parameters);
                    if (DeviceInfo.supportsSDK(15)) {
                        mVideoStabilizationSupported = parameters.isVideoStabilizationSupported();
                    }
                    mSupportedColorEffects = parameters.getSupportedColorEffects(); // Filter out some effects (for specific devices only)
                    filterDeviceSpecificEffects();
                    
                    // Initialize the writer thread that writes picture data to the storage
                    mPictureWriterThread = new PictureWriterThread(mCameraThreadListener, parameters.getPreviewSize().width, parameters.getPreviewSize().height);
                    mPictureWriterThread.start();
                    
                    Log.d(TAG, "initializeCameraProperties finished");
                }
            } 
        });
    }
    
    // Parameters will be set when setCameraPreviewSize() is called, when zoom changes and in onResume() in the UI
    public synchronized void setCameraParameters(final boolean flashEnabled, final ArrayList<Camera.Area> focusList) {
        getHandler().post(new Runnable() {

            @SuppressLint("NewApi")
            @Override
            public void run() {
                // TODO refactor this method
                if (mCamera != null) {
                    Parameters parameters = mCamera.getParameters();
                    // Adds continuous auto focus (only if API is high enough) to the parameters.
                    if (mAutoFocusSupported) {
                        if (DeviceInfo.supportsSDK(14)) {
                            parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        } else {
                            parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                        } 
                    }
                    
                    if (mWhiteBalanceSupported) {
                        parameters.setWhiteBalance(Parameters.WHITE_BALANCE_AUTO);
                    }
                    
                    if (mFlashSupported) {
                        if (flashEnabled) {
                            parameters.setFlashMode(Parameters.FLASH_MODE_ON);
                        } else {
                            parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                        }
                    }
                    
                    // This will only get called if video stabilization is supported and if the API is high enough. All handled in initCameraProperties.
                    if (mVideoStabilizationSupported && DeviceInfo.supportsSDK(15)) {
                        parameters.setVideoStabilization(true);
                    }
                    
                    // Set the current zoom value if we didn't use smooth zoom
                    if (!mSmoothZoomSupported) {
                        parameters.setZoom(mZoomValue);
                    }
                    // TODO Set the picture size according to device capabilities
                    parameters.setPictureSize(DeviceInfo.getRealScreenHeight(mContext), DeviceInfo.getRealScreenWidth(mContext));
                    
                    /*String result = "[";
                    List<Size> sizes = parameters.getSupportedPictureSizes();
                    for (Size s : sizes) {
                        result += " (" + s.width + " / " + s.height + ") ";
                    }
                    result += "]";
                    
                    System.out.println(result);*/
                    
                    // This makes the pictures stay full screen in gallery
                    if (mCurrentCameraID == CAMERA_ID_BACK) {
                        parameters.setRotation(90); 
                    } else {
                        parameters.setRotation(270);
                    }
                    
                    if (mPreviewSize != null) {
                        System.out.println("in parameters size is: " + mPreviewSize.width + " / " + mPreviewSize.height);
                        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
                    }
                    // Set the current effect of the camera (the will be visible in the camera preview)
                    if (mCurrentEffect != null && mZoomValue == 0) {
                        parameters.setColorEffect(mCurrentEffect);
                    }
                    
                    /*if (mFocusList != null) {
                        parameters.setFocusAreas(focusList);
                        parameters.setMeteringAreas(focusList);
                    }*/
                    //parameters.setPreviewFormat(ImageFormat.NV21);
                    /*List<Integer> list = parameters.getSupportedPreviewFormats();
                    for (int i = 0; i < list.size(); i++) {
                        System.out.println(list.get(i));
                    }*/
                    
                    // Finally, add the parameters to the camera
                    mCamera.setParameters(parameters);
                    
                    mFlashEnabled = flashEnabled; // Keep track of the flash settings
                    
                    Log.d(TAG, "setCameraParameters finished");
                }
            }
            
        });
    }
    
    public synchronized void setCameraDisplayOrientation() {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                if (mCamera != null) {
                    setCameraDisplayOrientation(mContext, mCurrentCameraID, mCamera);
                    mCameraThreadListener.cameraSetupComplete(mCurrentCameraID);
                    Log.d(TAG, "setCameraDisplayOrientation finished");
                }
                
            }
            
        });
    }
    
    public synchronized void performZoom(final float scaleFactor) {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                mZoomValue = (int) ((scaleFactor - 1) * (mZoomMax + 1));
                if (mZoomValue > 0 && mZoomValue < mZoomMax) {
                    if (mSmoothZoomSupported) {
                        mCamera.startSmoothZoom(mZoomValue);
                    } else {
                        setCameraParameters(mFlashEnabled, mFocusList); // Just update the camera parameters. This will also set the new zoom level
                    }
                }
            }
        });
    }
    
    public synchronized void startCameraPreview() {
        getHandler().post(new Runnable() {
            
            @Override
            public void run() {
                //prepareMediaRecorder();
                //startMediaRecorder();
                //resetMediaRecorder();
                startPreview();
            }
            
        });
    }
        
    // Overloaded method
    public synchronized void startCameraPreview(final CameraPreview cameraPreview) {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                //prepareMediaRecorder();
                //setPreviewDisplayForMediaRecorder(cameraPreview);
                //startMediaRecorder();
                //resetMediaRecorder();*/
                try {
                    mCamera.setPreviewDisplay(cameraPreview.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startPreview();
            }
            
        });
    }
    
    public synchronized void switchCamera() {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                deinitializeCamera();
                if (mCurrentCameraID == CAMERA_ID_BACK) {
                    mCurrentCameraID = CAMERA_ID_FRONT;
                } else if (mCurrentCameraID == CAMERA_ID_FRONT) {
                    mCurrentCameraID = CAMERA_ID_BACK;
                }
                
                initializeCamera(mCurrentCameraID);
                initializeCameraProperties();
                setCameraParameters(mFlashEnabled, mFocusList);
                setCameraDisplayOrientation();
            }
        });
    }
    
    public synchronized void takePicture() {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                mCamera.takePicture(getShutterCallback(), getRawCallback(), getPostViewCallback(), getJpegCallback());
            }
            
        });
    }
    
    public synchronized void startCapturing() {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                System.out.println("started capturing");
                //mPictureTakerThread = new PictureTakerThread(mCamera);
                //mPictureTakerThread.start();
                
                //mPictureTakerThread.allocateBufferForCamera();
                
                
                //deinitializeFrameCallback();
                //initializeFrameCallback();
                //allocateNewFrameBuffer();
                mCamera.takePicture(getShutterCallback(), getRawCallback(), getJpegCallback());
            }
            
        });
        
    }
    
    public synchronized void stopCapturing() {
        getHandler().postAtFrontOfQueue(new Runnable() {

            @Override
            public void run() {
                //deinitializeFrameCallback();
                System.out.println("Stopped capturing");
            }
            
        });
    }
    
    public synchronized void writePictureData() {
        if (pictureDataIsAvailable())
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                String filename = getAlbumStorageDir() + "/" + getDefaultFilename();
                
                if (!DeviceInfo.isExternalStorageWritable()) {
                    mCameraThreadListener.alertCameraThread(NO_STORAGE_AVAILABLE);
                } else if (mPictureData == null) {
                    mCameraThreadListener.alertCameraThread(FAILED_TO_SAVE_PICTURE);
                    Log.d(TAG, "Data Was Empty, Not Writing to File");
                } else {
                    mCameraThreadListener.alertCameraThread(IS_SAVING_PICTURE);
                    
                    try {
                        FileOutputStream output = new FileOutputStream(filename);
                        output.write(mPictureData);
                        output.close();
                        mCameraThreadListener.alertCameraThread(SAVED_PICTURE_SUCCESSFULLY);
                        Log.d(TAG, "Image Saved Successfully");
                    } catch (IOException e) {
                        mCameraThreadListener.alertCameraThread(FAILED_TO_SAVE_PICTURE);
                        Log.d(TAG, "Saving Image Failed!");
                    } finally {
                        // We have to refresh the grid view UI to make the new photo show up
                        mCameraThreadListener.newPictureAddedToGallery();
                    }
                }
            }
            
        });
    }
    
    // This method is only called in onSurfaceTextureAvailable from the Camera preview that uses a TextureView
    public synchronized void setPreviewTexture(final SurfaceTexture surface) {
        getHandler().post(new Runnable() {

            @SuppressLint("NewApi")
            @Override
            public void run() {
                if (mCamera != null) {
                    try {
                        mCamera.setPreviewTexture(surface);
                    } catch (IOException e) {
                        Log.e(TAG, "Could not set preview texture to camera");
                        e.printStackTrace();
                    }
                }
                
            }
            
        });
    }
    
    // This is called from onMeasure in the Camera Preview View class
    public synchronized void setCameraPreviewSize(final int width, final int height) {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                if (mCamera != null) {
                    mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
                    if (mSupportedPreviewSizes != null) {
                        mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
                    }
                    setCameraParameters(mFlashEnabled, mFocusList);
                }
            }
            
        });
    }
    
    public synchronized void setupFrameCallback() {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                
                mCamera.setPreviewCallbackWithBuffer(null);
                mCamera.setPreviewCallbackWithBuffer(getPreviewCallback());
            }
            
        });
        
    }
    
    private void initializeFrameCallback() {
        mCamera.setPreviewCallbackWithBuffer(getPreviewCallback());
    }
    
    private void deinitializeFrameCallback() {
        mCamera.setPreviewCallbackWithBuffer(null);
    }
    
    private void allocateNewFrameBuffer() {
        mCamera.addCallbackBuffer(new byte[getFrameByteSize()]);
    }
    
    public synchronized void touchFocus(final Rect touchRect) {
        getHandler().post(new Runnable() {

            // TODO Fix API
            @SuppressLint("NewApi")
            @Override
            public void run() {
                final Rect targetFocusRect = new Rect(
                    touchRect.left * 2000/DeviceInfo.getScreenWidth(mContext) - 1000,
                    touchRect.top * 2000/DeviceInfo.getScreenHeight(mContext) - 1000,
                    touchRect.right * 2000/DeviceInfo.getScreenWidth(mContext) - 1000,
                    touchRect.bottom * 2000/DeviceInfo.getScreenHeight(mContext)  - 1000);
                
                Camera.Area focusArea = new Camera.Area(targetFocusRect, 1000);
                mFocusList = new ArrayList<Camera.Area>();
                mFocusList.add(focusArea);
                
                setCameraParameters(mFlashEnabled, mFocusList);
                mCameraThreadListener.setTouchFocusView(touchRect); // This is disabled 
            }
        });
    }
    
    public synchronized void setCameraEffect(final int location) {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                mCurrentEffect = mSupportedColorEffects.get(location);
                setCameraParameters(mFlashEnabled, mFocusList);
            }
            
        });
    }
    
    public synchronized void setupVideo() {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                prepareMediaRecorder();
            }
            
        });
    }
    
    public synchronized void cancelVideoSetup() {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                releaseMediaRecorder();
            }
            
        });
    }
    
    public synchronized void startVideoRecording() {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                startMediaRecorder();
            }
            
        });
    }
    
    public synchronized void stopVideoRecording() {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                stopMediaRecorder();
            }
            
        });
    }
    
    // Private methods
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
    
    private void deinitializeCamera() {
        stopPreview();
        releaseCamera();
        clearPictureData();
        
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
    
    private void startPreview() {
        if (mCamera != null) {
            mCamera.startPreview();
        }
    }
    
    private void releasePreview() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
        }
    }
    
    private void clearPictureData() {
        mPictureData = null;
        mCurrentEffect = null;
    }
    
    private boolean pictureDataIsAvailable() {
        return (mPictureData != null);
    }

    private void prepareMediaRecorder() {
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        
        mMediaRecorder.setProfile(CamcorderProfile.get(mCurrentCameraID, CamcorderProfile.QUALITY_HIGH));
        mMediaRecorder.setOutputFile(getFile().getAbsolutePath()/*getPipeFD()*/);
        
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
        
    private void setPreviewDisplayForMediaRecorder(CameraPreview cameraPreview) {
        if (!DeviceInfo.supportsSDK(14)) {
            // We have to set the preview display for devices that use a SurfaceView
            mMediaRecorder.setPreviewDisplay(((CameraPreview)cameraPreview).getHolder().getSurface());
        }
    }
    
    private void startMediaRecorder() {
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.start();
                Log.d(TAG, "Start media recorder successful");
            } catch (RuntimeException ie){
                Log.e(TAG, "Failed to start the media recorder");
            }
            
        }
    }
    
    private void stopMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
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
    
    private void resetMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
        }
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
    
    // Callbacks
    private ErrorCallback getErrorCallback() {
        if (mErrorCallback == null) {
            mErrorCallback = new Camera.ErrorCallback() {
                
                @Override
                public void onError(int error, Camera camera) {
                    if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
                        deinitializeCamera();
                        initializeCamera(mCurrentCameraID);
                        initializeCameraProperties();
                        setCameraParameters(mFlashEnabled, mFocusList);
                        setCameraDisplayOrientation();
                    }
                }
            };
        }
        return mErrorCallback;
    }
    
    private ShutterCallback getShutterCallback() {
        if (mShutterCallback == null) {
            mShutterCallback = new ShutterCallback() {

                @Override
                public void onShutter() {
                    mCameraThreadListener.makeFlashAnimation();
                    //playCameraSound();
                }
            };
        }
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
                    mPictureWriterThread.writeDataToFile(data);
                    startPreview();
                }
            };
        }
        return mJpegCallback;
    }
    
    private PreviewCallback getPreviewCallback() {
        if (mPreviewCallback == null) {
            mPreviewCallback = new PreviewCallback() {

                @Override
                public synchronized void onPreviewFrame(byte[] data, Camera camera) {
                    Log.d(TAG, "On preview frame");
                    mPictureWriterThread.writeDataToFile(data);
                    allocateNewFrameBuffer();
                }
                
            };
        }
        return mPreviewCallback;
    }
    
    // Static methods
    private static void setCameraDisplayOrientation(Context context, int cameraId, android.hardware.Camera camera) {
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
    
    private static Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
    
    // Seriously, Google?! How come that the Nexus 4 shows "whiteboard" and "blackboard" as supported effects even though those effects are not supported?
    private synchronized void filterDeviceSpecificEffects() {
        if (DeviceInfo.isNexus4() && mSupportedColorEffects != null) {
            Iterator<String> it = mSupportedColorEffects.iterator();
            while (it.hasNext()) {
                String item = it.next();
                if (item.equals("whiteboard") || item.equals("blackboard")) {
                    it.remove();
                }
            }
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
    
    private int getFrameByteSize() {
        Camera.Parameters parameters = mCamera.getParameters();
        int previewFormat = parameters.getPreviewFormat();
        int bitsPerPixel = ImageFormat.getBitsPerPixel(previewFormat);
        float bytePerPixel = (float) bitsPerPixel / (float) 8.0;
        Camera.Size camerasize = parameters.getPreviewSize();
        int frameByteSize = (int) (((float)camerasize.width * (float)camerasize.height) * bytePerPixel);
        return frameByteSize;
    }
    
    private void setCameraSound() {
    	SoundPool soundPool = new SoundPool(10, AudioManager.STREAM_NOTIFICATION, 0);
		soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId,
					int status) {
				mSoundLoaded = true;
			}
		});
		//mSoundId = soundPool.load(mContext, R.raw.camera_click, 1);
    }
    
    private void playCameraSound() {
    	AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		float volume = actualVolume / maxVolume;
		if (mSoundLoaded) {
			mSoundPool.play(mSoundId, volume, volume, 1, 0, 1f);
			Log.e("Test", "Played camera click sound");
		}
    }
    
    public int getNumberOfColorEffects() {
    	return mSupportedColorEffects.size();
    }
    
    public int getZoomValue() {
    	return mZoomValue;
    }
}
