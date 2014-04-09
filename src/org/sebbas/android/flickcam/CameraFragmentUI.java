package org.sebbas.android.flickcam;

import java.util.concurrent.atomic.AtomicBoolean;

import org.sebbas.android.helper.DeviceInfo;
import org.sebbas.android.interfaces.CameraFragmentListener;
import org.sebbas.android.listener.CameraThreadListener;
import org.sebbas.android.views.CameraPreviewAdvanced;
import org.sebbas.android.views.CameraPreview;
import org.sebbas.android.views.DrawingView;
import org.sebbas.android.views.Flasher;
import org.sebbas.android.views.OrientationImageButton;

import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.squareup.seismic.ShakeDetector;
import com.tekle.oss.android.animation.AnimationFactory;
import com.tekle.oss.android.animation.AnimationFactory.FlipDirection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class CameraFragmentUI extends Fragment implements CameraThreadListener, ShakeDetector.Listener {

    // Private constants
    protected static final int GALLERY_FRAGMENT_NUMBER = 2;
    private static final int FLIP_PREVIEW_ANIMATION_DURATION = 300;
    private static final String CAMERA_MODE = "Camera Mode";
    private static final String VIDEO_MODE = "Video Mode";
    protected static final String TAG = "camera_fragment";
    
    // Instance variables for the fragment
    private Context mContext;
    private Handler mHandler;
    private int mCurrentPreviewID;
    private CameraFragmentListener mCameraFragmentListener;
    private CameraThread mCameraThread;
    private boolean mCameraWasSwapped;

    // Instance variables for the camera
    private boolean mFlashEnabled;
    protected boolean mPictureTaken;
    private boolean mVideoEnabled;
    private boolean mIsRecording;
    
    // Instance variables for the UI
    private View mRootView;
    private FrameLayout mControlLayout;
    private FrameLayout mPreviewLayout;
    private ImageButton mShutterButton;
    private OrientationImageButton mSwitchCameraButton;
    private OrientationImageButton mSwitchFlashButton;
    private OrientationImageButton mAcceptButton;
    private OrientationImageButton mCancelButton;
    private OrientationImageButton mGalleryButton;
    private OrientationImageButton mSettingsButton;
    private ViewPager mViewPager;
    private ViewFlipper mCameraViewFlipper;
    private View mCameraOnePreviewAdvanced;
    private CameraPreview mCameraOnePreview;
    private View mCameraTwoPreviewAdvanced;
    private CameraPreview mCameraTwoPreview;
    private DrawingView mDrawingView;
    
    // Listeners
    private OnClickListener mSwitchFlashListener;
    private OnClickListener mSwitchCameraListener;
    private OnClickListener mGalleryListener;
    private OnClickListener mSettingsListener;
    private OnClickListener mShutterListener;
    private OnClickListener mAcceptListener;
    private OnClickListener mCancelListener;
    private OnTouchListener mShutterHoldListener;
    protected AtomicBoolean mPreviewIsRunning;
    
    // Runnables
    private Runnable mAlertCameraThreadError;
    private Runnable mCameraSetupComplete;
    private Runnable mNewPictureAddedToGallery;
    private Runnable mSetTouchFocusView;
    
    private OrientationEventListener mOrientationEventListener;
    
    public static CameraFragmentUI newInstance() {
        CameraFragmentUI cameraFragment = new CameraFragmentUI();
        return cameraFragment;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeInstanceVariables();
        
        initHandler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        initializeViews(inflater, container);
        removeUnsupportedViews();
        setViewListeners();
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mCameraThread = new CameraThread(this, mContext);
        mCameraThread.start();

        configureUIElements();
        postCameraInitializations();
        
    }

    @Override
    public void onPause() {
        super.onPause();
        mPreviewIsRunning.set(false);
        mPictureTaken = false; // Delete pending picture
        mCameraThread.quitThread();
        waitForCameraThreadToFinish();
        removeCameraPreview(true);
        removeAllPostedRunnables();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
    
    private void initializeInstanceVariables() {
        mContext = this.getActivity();
        
        mCameraFragmentListener = (CameraFragmentListener) mContext;
        mFlashEnabled = false;
        mPreviewIsRunning = new AtomicBoolean(false);
        
        // Setup the shake detection
        SensorManager sensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        ShakeDetector sd = new ShakeDetector(this);
        sd.start(sensorManager);
        
    }
    
    private void removeUnsupportedViews() {
        if (!DeviceInfo.supportsFrontCamera(mContext)) {
            mSwitchCameraButton.setVisibility(View.GONE);
        }
    }
    
    private void initHandler() {
        synchronized(this) {
            mHandler = new Handler(Looper.getMainLooper()); // This handler binds automatically to the Looper of the UI Thread
            this.notifyAll();
        }
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
    
    private void initializeViews(LayoutInflater inflater, ViewGroup container) {
        if (DeviceInfo.hasSoftButtons(mContext)) {
            mRootView = inflater.inflate(R.layout.camera_layout_with_navigationbar, container, false);
        } else {
            mRootView = inflater.inflate(R.layout.camera_layout_without_navigationbar, container, false);
        }
        
        mControlLayout = (FrameLayout) mRootView.findViewById(R.id.control_mask);
        mPreviewLayout = (FrameLayout) mRootView.findViewById(R.id.preview_mask);
        
        mShutterButton = (ImageButton) mRootView.findViewById(R.id.shutter_button);
        mSwitchCameraButton = (OrientationImageButton) mRootView.findViewById(R.id.switch_camera);
        mSwitchFlashButton = (OrientationImageButton) mRootView.findViewById(R.id.switch_flash);
        mAcceptButton = (OrientationImageButton) mRootView.findViewById(R.id.accept_image);
        mCancelButton = (OrientationImageButton) mRootView.findViewById(R.id.discard_image);
        mGalleryButton = (OrientationImageButton) mRootView.findViewById(R.id.goto_gallery);
        mSettingsButton = (OrientationImageButton) mRootView.findViewById(R.id.settings_button);
        mViewPager = (ViewPager) getActivity().findViewById(R.id.viewpager);
        mCameraViewFlipper = (ViewFlipper) mRootView.findViewById(R.id.camera_view_flipper);
        
        mDrawingView = new DrawingView(mContext);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mPreviewLayout.addView(mDrawingView, params);
    }
    
    private void setViewListeners() {
        mShutterButton.setOnTouchListener(getShutterHoldListener());
        mSwitchCameraButton.setOnClickListener(getSwitchCameraListener());
        mSwitchFlashButton.setOnClickListener(getSwitchFlashListener());
        mGalleryButton.setOnClickListener(getSwitchToGalleryListener());
        mAcceptButton.setOnClickListener(getAcceptListener());
        mCancelButton.setOnClickListener(getCancelListener());
        mSettingsButton.setOnClickListener(getSettingsListener());
    }
    
    private void postCameraInitializations() {
        mCameraThread.initializeCamera(mCurrentPreviewID); // Default id setup
        mCameraThread.initializeCameraProperties();
        mCameraThread.setCameraParameters(mFlashEnabled, null); 
        mCameraThread.setCameraDisplayOrientation();
    }
    
    // Methods that make changes to the UI
    /*@SuppressLint("NewApi")
    private void setupCameraPreviews(int cameraID) {
        // If device supports API 14 then add a TextureView (better performance) to the RL, else add a SurfaceView (no other choice)
        if (cameraID == CameraThread.CAMERA_ID_BACK) {
            if (DeviceInfo.supportsSDK(14)) {
                mCameraOnePreviewAdvanced = new CameraPreviewAdvanced(mContext, mCameraThread);
                mCameraViewFlipper.addView(mCameraOnePreviewAdvanced);
            } else {
                mCameraOnePreview = new CameraPreview(mContext, mCameraThread);
                mCameraViewFlipper.addView(mCameraOnePreview);
            }
            setCurrentPreviewID(CameraThread.CAMERA_ID_BACK); // Keep track of the current camera/ preview that is shown
        }
        if (cameraID == CameraThread.CAMERA_ID_FRONT) {
            if (DeviceInfo.supportsSDK(14)) {
                mCameraTwoPreviewAdvanced = new CameraPreviewAdvanced(mContext, mCameraThread);
                mCameraViewFlipper.addView(mCameraTwoPreviewAdvanced);
            } else {
                mCameraTwoPreview = new CameraPreview(mContext, mCameraThread);
                mCameraViewFlipper.addView(mCameraTwoPreview);
            }
            setCurrentPreviewID(CameraThread.CAMERA_ID_FRONT); // Keep track of the current camera/ preview that is shown
        }
        Log.d(TAG, "Instantiated camera preview");
    }*/
    
    @SuppressLint("NewApi")
    private void setupCameraPreviews(int cameraID) {
        // If device supports API 14 then add a TextureView (better performance) to the RL, else add a SurfaceView (no other choice)
        if (DeviceInfo.supportsSDK(14)) {
            setupTexturePreview(cameraID);
        } else {
            setupSurfacePreview(cameraID);
        }
        Log.d(TAG, "Instantiated camera preview");
    }
    
    private void setupTexturePreview(int cameraID) {
        if (cameraID == CameraThread.CAMERA_ID_BACK) {
            mCameraOnePreviewAdvanced = new CameraPreviewAdvanced(mContext, mCameraThread);
            mCameraViewFlipper.addView(mCameraOnePreviewAdvanced);
            setCurrentPreviewID(CameraThread.CAMERA_ID_BACK); // Keep track of the current camera/ preview that is shown
        } 
        if (cameraID == CameraThread.CAMERA_ID_FRONT) {
            mCameraTwoPreviewAdvanced = new CameraPreviewAdvanced(mContext, mCameraThread);
            mCameraViewFlipper.addView(mCameraTwoPreviewAdvanced);
            setCurrentPreviewID(CameraThread.CAMERA_ID_FRONT); // Keep track of the current camera/ preview that is shown
        }
    }
    
    private void setupSurfacePreview(int cameraID) {
        if (cameraID == CameraThread.CAMERA_ID_BACK) {
            mCameraOnePreview = new CameraPreview(mContext, mCameraThread);
            mCameraViewFlipper.addView(mCameraOnePreview);
            setCurrentPreviewID(CameraThread.CAMERA_ID_BACK); // Keep track of the current camera/ preview that is shown
        } 
        if (cameraID == CameraThread.CAMERA_ID_FRONT) {
            mCameraTwoPreview = new CameraPreview(mContext, mCameraThread);
            mCameraViewFlipper.addView(mCameraTwoPreview);
            setCurrentPreviewID(CameraThread.CAMERA_ID_FRONT); // Keep track of the current camera/ preview that is shown
        }
    }
    
    private void waitForCameraThreadToFinish() {
        // Wait for the camera thread to finish deinitialization
        try {
            mCameraThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } 
    }
    
    private void setFlashIcon() {
        if (mFlashEnabled) {
            mSwitchFlashButton.setImageResource(R.drawable.ic_action_flash_on);
        } else {
            mSwitchFlashButton.setImageResource(R.drawable.ic_action_flash_off);
        }
    }
    
    private void setModeIcon() {
        if (mVideoEnabled) {
            mSettingsButton.setImageResource(R.drawable.ic_action_camera);
        } else {
            mSettingsButton.setImageResource(R.drawable.ic_action_video);
        }
    }
    
    private void reorganizeUI() {
        mControlLayout.bringToFront();
        mControlLayout.invalidate();
    }
    
    private void setShutterRetake() {
        mPictureTaken = true;
        configureUIElements();
    }
    
    private void makeUIMessage(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }
    
    private void configureUIElements() {
        if (mPictureTaken) {
            mSettingsButton.setVisibility(View.GONE);
            mGalleryButton.setVisibility(View.GONE);
            mAcceptButton.setVisibility(View.VISIBLE);
            mCancelButton.setVisibility(View.VISIBLE);
            mAcceptButton.bringToFront();
            mCancelButton.bringToFront();
            
        } else {
            mAcceptButton.setVisibility(View.GONE);
            mCancelButton.setVisibility(View.GONE);
            mSettingsButton.setVisibility(View.VISIBLE);
            mGalleryButton.setVisibility(View.VISIBLE);
        }
    }
    
    private void retakePicture() {
        resetShutter();
    }
    
    private void resetShutter() {
        mPictureTaken = false;
        configureUIElements();
        
        mCameraThread.stopCamera(); // Deinitialize camera
        postCameraInitializations(); // Reinitialize camera
        removeCameraPreview(true);
        
    }
    /*
    private void removeAllCameraPreviewViews() {
        if(DeviceInfo.supportsSDK(14)) {
            mCameraViewFlipper.removeView(mCameraOnePreviewAdvanced);
            mCameraViewFlipper.removeView(mCameraTwoPreviewAdvanced);
        } else {
            mCameraViewFlipper.removeView(mCameraOnePreview);
            mCameraViewFlipper.removeView(mCameraTwoPreview);
        }
        Log.d(TAG, "Removed all camera preview views");
    }
    
    private void removeHiddenCameraPreviewView() {
        if(DeviceInfo.supportsSDK(14)) {
            if (mCurrentPreviewID == CameraThread.CAMERA_ID_BACK) {
                mCameraViewFlipper.removeView(mCameraTwoPreviewAdvanced);
            } else if (mCurrentPreviewID == CameraThread.CAMERA_ID_FRONT) {
                mCameraViewFlipper.removeView(mCameraOnePreviewAdvanced);
            }
        } else {
            if (mCurrentPreviewID == CameraThread.CAMERA_ID_BACK) {
                mCameraViewFlipper.removeView(mCameraTwoPreview);
            } else if (mCurrentPreviewID == CameraThread.CAMERA_ID_FRONT) {
                mCameraViewFlipper.removeView(mCameraOnePreview);
            }
        }
    }*/
    
    private void removeCameraPreview(boolean removeAll) {
        if (DeviceInfo.supportsSDK(14)) {
            removeTextureView(removeAll);
        } else {
            removeSurfaceView(removeAll);
        }
    }
    
    private void removeTextureView(boolean removeAll) {
        // If requested then remove all preview views
        if (removeAll) {
            mCameraViewFlipper.removeView(mCameraOnePreviewAdvanced);
            mCameraViewFlipper.removeView(mCameraTwoPreviewAdvanced);
        // otherwise we check the current camera id and remove the preview currently not used
        } else {
            if (mCurrentPreviewID == CameraThread.CAMERA_ID_BACK) {
                mCameraViewFlipper.removeView(mCameraTwoPreviewAdvanced);
            } else if (mCurrentPreviewID == CameraThread.CAMERA_ID_FRONT) {
                mCameraViewFlipper.removeView(mCameraOnePreviewAdvanced);
            }
        }
    }
    
    private void removeSurfaceView(boolean removeAll) {
        // If requested then remove all preview views
        if (removeAll) {
            mCameraViewFlipper.removeView(mCameraOnePreview);
            mCameraViewFlipper.removeView(mCameraTwoPreview);
        // otherwise we check the current camera id and remove the not used preview
        } else {
            if (mCurrentPreviewID == CameraThread.CAMERA_ID_BACK) {
                mCameraViewFlipper.removeView(mCameraTwoPreview);
            } else if (mCurrentPreviewID == CameraThread.CAMERA_ID_FRONT) {
                mCameraViewFlipper.removeView(mCameraOnePreview);
            }
        }
    }
    
    private void removeAllPostedRunnables() {
        mHandler.removeCallbacks(mAlertCameraThreadError);
        mHandler.removeCallbacks(mCameraSetupComplete);
        mHandler.removeCallbacks(mNewPictureAddedToGallery);
        mHandler.removeCallbacks(mSetTouchFocusView);
    }
    
    private void startAnimation() {
        AnimationFactory.flipTransition(mCameraViewFlipper, FlipDirection.LEFT_RIGHT, FLIP_PREVIEW_ANIMATION_DURATION);
    }
    
     // Listeners
    private OnClickListener getSwitchFlashListener() {
        mSwitchFlashListener = new View.OnClickListener() {
           
            @Override
            public void onClick(View v) {
                mFlashEnabled = !mFlashEnabled;
                mCameraThread.setCameraParameters(mFlashEnabled, null);
                setFlashIcon();
            }
        };
        return mSwitchFlashListener;
    }
   
    private OnClickListener getSwitchCameraListener() {
        mSwitchCameraListener = new View.OnClickListener() {
           
            @Override
            public void onClick(View v) {
                if (mPreviewIsRunning.get()) {
                    Log.d(TAG, "Camera switched");
                    mCameraThread.switchCamera();
                    mCameraWasSwapped = true;
                    mPreviewIsRunning.set(false);
                }
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
                mVideoEnabled = !mVideoEnabled;
                if (mVideoEnabled) {
                    makeUIMessage(VIDEO_MODE);
                    mCameraThread.setupVideo();
                } else {
                    makeUIMessage(CAMERA_MODE);
                    mCameraThread.cancelVideoSetup();
                }
                
                setModeIcon();
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
                    if (mVideoEnabled) {  // Video mode
                        if (mIsRecording) {
                            mIsRecording = false;
                            mCameraThread.stopVideoRecording();
                        } else {
                            mIsRecording = true;
                            mCameraThread.startVideoRecording();
                        }
                    } else { // Camera mode
                        if (mPictureTaken) {
                            retakePicture();
                        } else if (mPreviewIsRunning.get()) {
                            mCameraThread.takePicture();
                            setShutterRetake();
                            mPreviewIsRunning.set(false);
                        }
                    }
                    
                }
            };
        }
        return mShutterListener;
    }
    
    private OnTouchListener getShutterHoldListener() {
        if (mShutterHoldListener == null) {
            mShutterHoldListener = new OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mCameraThread.startCapturing();
                        return true;
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        mCameraThread.stopCapturing();
                        return true;
                    }
                    return false;
                }
            };
            
        }
        return mShutterHoldListener;
    }
    
    private OnClickListener getAcceptListener() {
        if (mAcceptListener == null) {
            mAcceptListener = new OnClickListener() {

                @Override
                public void onClick(View view) {
                    mCameraThread.writePictureData();
                    resetShutter();
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

    // Overridden methods from CameraThreadListener
    @Override
    public synchronized void alertCameraThread(final String message) {
        mAlertCameraThreadError = new AlertCameraThreadError(message);
        getHandler().post(mAlertCameraThreadError);
    }

    @Override
    public synchronized void cameraSetupComplete(final int cameraID) {
        System.out.println("Entered cameraSetupComplete. Handler is " + getHandler());
        mCameraSetupComplete = new CameraSetupComplete(cameraID);
        getHandler().post(mCameraSetupComplete);
    }
    
    @Override
    public synchronized void newPictureAddedToGallery() {
        mNewPictureAddedToGallery = new NewPictureAddedToGallery();
        getHandler().post(mNewPictureAddedToGallery);
    }
    
    @Override
    public synchronized void setTouchFocusView(final Rect tFocusRect) {
        mSetTouchFocusView = new SetTouchFocusView(tFocusRect);
        getHandler().post(mSetTouchFocusView);
    }
    
    // Private Runnable classes
    private class AlertCameraThreadError implements Runnable {

        private String message;
        public AlertCameraThreadError(String message) {
            this.message = message;
        }
        
        @Override
        public void run() {
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        }
    }
    
    private class CameraSetupComplete implements Runnable {

        private int cameraID;
        public CameraSetupComplete(int cameraID) {
            this.cameraID = cameraID;
        }
        
        @Override
        public void run() {
            Log.d(TAG, "Camera setup complete. Now setting up camera previews");
            setupCameraPreviews(cameraID);
            reorganizeUI();
            if (mCameraWasSwapped) {
                startAnimation();
                removeCameraPreview(false); // We remove the old preview so that the previews don't accumulate
                mCameraWasSwapped = false;
            }
            mPreviewIsRunning.set(true);
            
        }
    }
    
    private class NewPictureAddedToGallery implements Runnable {
        
        @Override
        public void run() {
            mCameraFragmentListener.refreshAdapter();
        }
    }
    
    private class SetTouchFocusView implements Runnable {

        private Rect focusRect;
        public SetTouchFocusView(Rect focusRect) {
            this.focusRect = focusRect;
        }
        
        @Override
        public void run() {
            mDrawingView.setHaveTouch(true, focusRect);
            mDrawingView.invalidate();
            mDrawingView.bringToFront();
        }
    }
    
    // Setters and getters
    private void setCurrentPreviewID(int previewID) {
        mCurrentPreviewID = previewID;
    }
    
    private int getCurrentCameraID() {
        return mCurrentPreviewID;
    }
    
    private int getCurrentDeviceRotaion() {
        return DeviceInfo.getDeviceRotation(mContext);
    }

    // When device is shaken the preview is discarded
    @Override
    public void hearShake() {
        if (mPictureTaken) {
            resetShutter();
        }
    }

    @Override
    public void makeFlashAnimation() {
        getHandler().postAtFrontOfQueue(new Runnable() {

            @Override
            public void run() {
                Flasher flasher = new Flasher(mContext, mPreviewLayout);
                flasher.flash(1);
            }
        });
    }
}
