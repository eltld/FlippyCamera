package org.sebbas.android.flickcam;

import java.util.concurrent.atomic.AtomicBoolean;

import org.sebbas.android.interfaces.CameraUICommunicator;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class PictureTakerThread extends Thread {

    private static final String TAG = "picture_taker_thread";
    
    private Camera mCamera;
    private int mFrameByteSize;
    private Handler mHandler;
    
    // Callbacks
    private ShutterCallback mShutterCallback;
    private PictureCallback mRawCallback;
    private PictureCallback mPostViewCallback;
    private PictureCallback mJpegCallback;
    private AutoFocusCallback mAutoFocusCallback;
    
    private PictureWriterThread mPictureWriterThread;
    private CameraThread mCameraThread;
    private CameraUICommunicator mCameraThreadListener;
    private static AtomicBoolean mCameraIsBusy;
    
    public PictureTakerThread(Camera camera, PictureWriterThread pictureWriterThread, CameraUICommunicator cameraThreadListener, CameraThread cameraThread) {
        mCamera = camera;
        mFrameByteSize = getFrameByteSize();
        mPictureWriterThread = pictureWriterThread;
        mCameraThread = cameraThread;
        mCameraThreadListener = cameraThreadListener;
    }
    
    @Override
    public void run() {
        Looper.prepare();
        synchronized(this) {
            super.run();
            try {
                
                mHandler = new Handler();
                this.notifyAll();

                mCameraIsBusy = new AtomicBoolean(false);

            } catch (Throwable t) {
                Log.e(TAG, "Picture taker thread halted due to an error", t);
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
    
    public void removeAllCallbacks() {
        mHandler.removeCallbacks(null);
    }
    
    public void allocateBufferForCamera() {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                if (!Thread.interrupted()) {
                    Log.d(TAG, "Allocating buffer");
                    mCamera.addCallbackBuffer(new byte[mFrameByteSize]);
                }
                
            }
            
        });
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
    
    public void takePicture() {
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                mCameraIsBusy.set(true);
                mCamera.takePicture(getShutterCallback(), getRawCallback(), getJpegCallback());
            }
            
        });
        
    }
    
    public void takeFocusedPicture() {
    	getHandler().post(new Runnable() {

			@Override
			public void run() {
				mCameraIsBusy.set(true);
				mCamera.autoFocus(getAutoFocusCallBack());
			}
    		
    	});
    }
    
    private ShutterCallback getShutterCallback() {
        if (mShutterCallback == null) {
            mShutterCallback = new ShutterCallback() {

                @Override
                public void onShutter() {
                    mCameraThreadListener.makeFlashAnimation();
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
                    mCameraThread.startCameraPreview();
                    mCameraIsBusy.set(false);
                }
            };
        }
        return mJpegCallback;
    }
    
    private AutoFocusCallback getAutoFocusCallBack() {
        if (mAutoFocusCallback == null) {
            mAutoFocusCallback = new AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    camera.takePicture(getShutterCallback(), getRawCallback(), getJpegCallback());
                }
            };
        }
        return mAutoFocusCallback;
    }
    
    public static boolean cameraIsBusy() {
        return mCameraIsBusy.get();
    }
}
