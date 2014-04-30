package org.sebbas.android.flickcam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.sebbas.android.helper.DeviceInfo;
import org.sebbas.android.interfaces.CameraThreadListener;
import org.sebbas.android.views.CameraPreview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

public class CameraThread extends Thread {

    // Private constants
    private static final String TAG = "camera_thread";
    public static final int CAMERA_ID_BACK = Camera.CameraInfo.CAMERA_FACING_BACK;
    public static final int CAMERA_ID_FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT;
    
    private static final String COULD_NOT_INITIALIZE_CAMERA = "Could not initialize camera";
    private static final String NO_CAMERAS_FOUND = "No cameras found on device";
    private static final String CANNOT_CONNECT_TO_CAMERA = "Cannot connect to camera";
    
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
    private List<Camera.Size> mSupportedPreviewSizes;
    private Camera.Size mPreviewSize;
    protected byte[] mPictureData;
    private ArrayList<Camera.Area> mFocusList;
    private String mCurrentEffect;
    private List<String> mSupportedColorEffects;
    
    // Callbacks
    private ErrorCallback mErrorCallback;
    private PreviewCallback mPreviewCallback;
    
    private PictureTakerThread mPictureTakerThread;
    private PictureWriterThread mPictureWriterThread;
    
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
    
    public void initializeCamera() {
        initializeCameraObject(mCurrentCameraID);
        initializeCameraProperties();
        setCameraParameters(mFlashEnabled, mFocusList);
        setCameraDisplayOrientation();
    }
    
    public synchronized void initializeCameraObject(final int cameraID) {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                try {
                    if (mNumberOfCamerasSupported == 0) {
                        Log.e(TAG, NO_CAMERAS_FOUND);
                        mCameraThreadListener.alertCameraThread(NO_CAMERAS_FOUND);
                    } else {
                        mCamera = getCameraInstance(cameraID); // Setup camera object
                        
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
                    
                    System.out.println(result);
                    */
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
                    if (mCurrentEffect != null) {
                        parameters.setColorEffect(mCurrentEffect);
                    }
                    
                    if (mFocusList != null) {
                        parameters.setFocusAreas(focusList);
                        parameters.setMeteringAreas(focusList);
                    }
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
    
    public synchronized void initializeHelperThreads() {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                // Initialize the writer thread that writes picture data to the storage
                mPictureWriterThread = new PictureWriterThread(mCameraThreadListener, mPreviewSize.width, mPreviewSize.height);
                mPictureWriterThread.start();
                
                mPictureTakerThread = new PictureTakerThread(mCamera, mPictureWriterThread, mCameraThreadListener, CameraThread.this);
                mPictureTakerThread.start();
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
                startPreview();
            }
            
        });
    }
        
    // Overloaded method
    public synchronized void startCameraPreview(final CameraPreview cameraPreview) {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
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
                
                initializeCamera();
            }
        });
    }
    
    public synchronized void startCapturing() {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                Log.d(TAG, "Started taking picture");
                mPictureTakerThread.takePicture();
            }
            
        });
        
    }
    
    public synchronized void stopCapturing() {
        getHandler().postAtFrontOfQueue(new Runnable() {

            @Override
            public void run() {
                Log.d(TAG, "Stopped taking picture");
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
    
    // This is called from onMeasure/ onSurfaceTextureAvailable in the Camera Preview View class
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
    
    private void clearPictureData() {
        mPictureData = null;
        mCurrentEffect = null;
    }
    
    // Callbacks
    private ErrorCallback getErrorCallback() {
        if (mErrorCallback == null) {
            mErrorCallback = new Camera.ErrorCallback() {
                
                @Override
                public void onError(int error, Camera camera) {
                    if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
                        deinitializeCamera();
                        mPictureTakerThread.removeAllCallbacks();
                        mPictureWriterThread.removeAllCallbacks();
                        initializeCamera();
                    }
                }
            };
        }
        return mErrorCallback;
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
    
    private static Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int width, int height) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) height / width;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = height;

        for (Camera.Size size : sizes) {
        	Log.d(TAG, "The size is: " + size.width + "/" + size.height);
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
    
    private int getFrameByteSize() {
        Camera.Parameters parameters = mCamera.getParameters();
        int previewFormat = parameters.getPreviewFormat();
        int bitsPerPixel = ImageFormat.getBitsPerPixel(previewFormat);
        float bytePerPixel = (float) bitsPerPixel / (float) 8.0;
        Camera.Size camerasize = parameters.getPreviewSize();
        int frameByteSize = (int) (((float)camerasize.width * (float)camerasize.height) * bytePerPixel);
        return frameByteSize;
    }
    
    
    public int getNumberOfColorEffects() {
        return mSupportedColorEffects.size();
    }
    
    public int getZoomValue() {
        return mZoomValue;
    }
}
