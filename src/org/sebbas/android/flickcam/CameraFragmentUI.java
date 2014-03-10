package org.sebbas.android.flickcam;

import org.sebbas.android.helper.DeviceInfo;
import org.sebbas.android.interfaces.CameraFragmentListener;
import org.sebbas.android.listener.CameraThreadListener;
import org.sebbas.android.listener.DeviceOrientationListener;
import org.sebbas.android.views.CameraPreviewAdvancedNew;
import org.sebbas.android.views.CameraPreviewNew;
import org.sebbas.android.views.OrientationImageButton;

import com.tekle.oss.android.animation.AnimationFactory;
import com.tekle.oss.android.animation.AnimationFactory.FlipDirection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class CameraFragmentUI extends Fragment implements CameraThreadListener {

    // Private constants
    protected static final int GALLERY_FRAGMENT_NUMBER = 2;
    private static final int FLIP_PREVIEW_ANIMATION_DURATION = 300;
    protected static final String TAG = "camera_fragment";
    
    // Instance variables for the fragment
    private Context mContext;
    private Handler mHandler;
    private int mCurrentPreviewID;
    private CameraFragmentListener mCameraFragmentListener;
    private DeviceOrientationListener mOrientationEventListener;
    private CameraThread mCameraThread;
    private boolean mCameraWasSwapped;

    // Instance variables for the camera
    private boolean mFlashEnabled;
    private int mDeviceRotation;
    protected boolean mPictureTaken;
    
    
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
    private CameraPreviewAdvancedNew mCameraOnePreviewAdvanced;
    private CameraPreviewNew mCameraOnePreview;
    private CameraPreviewAdvancedNew mCameraTwoPreviewAdvanced;
    private CameraPreviewNew mCameraTwoPreview;
    
    // Listeners
    private OnClickListener mSwitchFlashListener;
    private OnClickListener mSwitchCameraListener;
    private OnClickListener mGalleryListener;
    private OnClickListener mSettingsListener;
    private OnClickListener mShutterListener;
    private OnClickListener mAcceptListener;
    private OnClickListener mCancelListener;
    
    
    public static CameraFragmentUI newInstance() {
        CameraFragmentUI cameraFragment = new CameraFragmentUI();
        return cameraFragment;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeInstanceVariables();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        initializeViews(inflater, container);
        setViewListeners();
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mCameraThread = new CameraThread(this, mContext);
        mCameraThread.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        postCameraInitializations();
    }

    @Override
    public void onPause() {
        super.onPause();
        mCameraThread.stopCamera();
     // Wait for the camera thread to finish deinitialization
        try {
            mCameraThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } 
        removeAllCameraPreviewViews();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void initializeInstanceVariables() {
        mContext = this.getActivity();
        mHandler = new Handler(); // This handler binds automatically to the Looper of the UI Thread
        mCameraFragmentListener = (CameraFragmentListener) mContext;
        mFlashEnabled = false;
        mDeviceRotation = DeviceInfo.getDeviceRotation(mContext);
        mOrientationEventListener = new DeviceOrientationListener(mContext);
        mOrientationEventListener.enable();
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
    }
    
    private void setViewListeners() {
        mShutterButton.setOnClickListener(getShutterListener());
        mSwitchCameraButton.setOnClickListener(getSwitchCameraListener());
        mSwitchFlashButton.setOnClickListener(getSwitchFlashListener());
        mGalleryButton.setOnClickListener(getSwitchToGalleryListener());
        mAcceptButton.setOnClickListener(getAcceptListener());
        mCancelButton.setOnClickListener(getCancelListener());
    }
    
    private void postCameraInitializations() {
        mCameraThread.initializeCamera(mCurrentPreviewID); // Default id setup
        mCameraThread.initializeCameraProperties();
        mCameraThread.setCameraParameters(mFlashEnabled, mDeviceRotation); 
        mCameraThread.setCameraDisplayOrientation();
    }
    
    // Methods that make changes to the UI
    @SuppressLint("NewApi")
    private void startPreview(Camera camera, int cameraID) {
        // If device supports API 14 then add a TextureView (better performance) to the RL, else add a SurfaceView (no other choice)
        if (cameraID == CameraThread.CAMERA_ID_BACK) {
            if (DeviceInfo.supportsSDK(14)) {
                mCameraOnePreviewAdvanced = new CameraPreviewAdvancedNew(mContext, mCameraThread, camera);
                mPreviewLayout.addView(mCameraOnePreviewAdvanced);
            } else {
                mCameraOnePreview = new CameraPreviewNew(mContext, mCameraThread);
                mPreviewLayout.addView(mCameraOnePreview);
            }
            setCurrentPreviewID(CameraThread.CAMERA_ID_BACK); // Keep track of the current camera/ preview that is shown
        }
        if (cameraID == CameraThread.CAMERA_ID_FRONT) {
            if (DeviceInfo.supportsSDK(14)) {
                mCameraTwoPreviewAdvanced = new CameraPreviewAdvancedNew(mContext, mCameraThread, camera);
                mPreviewLayout.addView(mCameraTwoPreviewAdvanced);
            } else {
                mCameraTwoPreview = new CameraPreviewNew(mContext, mCameraThread);
                mPreviewLayout.addView(mCameraTwoPreview);
            }
            setCurrentPreviewID(CameraThread.CAMERA_ID_FRONT); // Keep track of the current camera/ preview that is shown
        }
    }
    
    private void setFlashIcon() {
        if (mFlashEnabled) {
            mSwitchFlashButton.setImageResource(R.drawable.ic_action_flash_on);
        } else {
            mSwitchFlashButton.setImageResource(R.drawable.ic_action_flash_off);
        }
    }
    
    private void reorganizeUI() {
        mControlLayout.bringToFront();
        mControlLayout.invalidate();
    }
    
    private void setShutterRetake() {
        mPictureTaken = true;
        swapUIElements();
    }
    
    private void swapUIElements() {
        if (mPictureTaken) {
            mAcceptButton.setVisibility(View.VISIBLE);
            mCancelButton.setVisibility(View.VISIBLE);
            mAcceptButton.bringToFront();
            mCancelButton.bringToFront();
            mSettingsButton.setVisibility(View.GONE);
            mGalleryButton.setVisibility(View.GONE);
        } else {
            mAcceptButton.setVisibility(View.GONE);
            mCancelButton.setVisibility(View.GONE);
            mSettingsButton.setVisibility(View.VISIBLE);
            mGalleryButton.setVisibility(View.VISIBLE);
        }
    }
    
    private void removeAllCameraPreviewViews() {
        if(DeviceInfo.supportsSDK(14)) {
            mCameraViewFlipper.removeView(mCameraOnePreviewAdvanced);
            mCameraViewFlipper.removeView(mCameraTwoPreviewAdvanced);
        } else {
            mCameraViewFlipper.removeView(mCameraOnePreview);
            mCameraViewFlipper.removeView(mCameraTwoPreview);
        }
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
    }
    
    
    private void startAnimation() {
        AnimationFactory.flipTransition(mCameraViewFlipper, FlipDirection.LEFT_RIGHT, FLIP_PREVIEW_ANIMATION_DURATION);
    }
    
    private void retakePicture() {
        resetShutter();
    }
    
    private void resetShutter() {
        swapUIElements();
        
        mCameraThread.stopCamera();
        postCameraInitializations();
        removeAllCameraPreviewViews();
    }
    
     // Listeners
     private OnClickListener getSwitchFlashListener() {
         mSwitchFlashListener = new View.OnClickListener() {
           
             @Override
             public void onClick(View v) {
                 mFlashEnabled = !mFlashEnabled;
                 mCameraThread.setCameraParameters(mFlashEnabled, mDeviceRotation);
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
                mCameraThread.switchCamera();
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
                // TODO
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
                        mCameraThread.takePicture();
                        setShutterRetake();
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
    public void alertCameraThreadError(final String message) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
            }
            
        });
        
    }

    @Override
    public void cameraSetupComplete(final Camera camera, final int cameraID) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                startPreview(camera, cameraID);
                reorganizeUI();
                if (mCameraWasSwapped) {
                    startAnimation();
                    removeHiddenCameraPreviewView(); // We remove the old preview so that the previews don't accumulate
                    mCameraWasSwapped = false;
                }
                
            }
            
        });
       
    }
    
    // Setters and getters
    private void setCurrentPreviewID(int previewID) {
        mCurrentPreviewID = previewID;
    }
    
    private int getCurrentCameraID() {
        return mCurrentPreviewID;
    }

    @Override
    public void newPictureAddedToGallery() {
        mCameraFragmentListener.refreshAdapter();
    }
}
