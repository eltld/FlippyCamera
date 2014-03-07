package org.sebbas.android.views;

import org.sebbas.android.interfaces.CameraPreviewListener;
import org.sebbas.android.listener.ScaleListener;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    
    private static final String TAG = "camera_preview";
    private static final int INVALID_POINTER_ID = -1;

    private SurfaceHolder mHolder;
    private CameraPreviewListener mListener;
    
    private float mPosX;
    private float mPosY;

    private float mLastTouchX;
    private float mLastTouchY;
    private int mActivePointerId = INVALID_POINTER_ID;
    
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.f;
    
    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @SuppressWarnings("deprecation")
    public CameraPreview(Context context, CameraPreviewListener listener) {
        super(context);
        mListener = listener;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener(listener, this));
        
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, "Surface was Changed");
        mListener.startRecorder();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "Surface was Created");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Surface was Destroyed");
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(event);

        // THIS CODE IS NOT NEEDED -> REMOVED SOON
        /*final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN: {
            final float x = event.getX();
            final float y = event.getY();

            mLastTouchX = x;
            mLastTouchY = y;
            mActivePointerId = event.getPointerId(0);
            break;
        }

        case MotionEvent.ACTION_MOVE: {
            final int pointerIndex = event.findPointerIndex(mActivePointerId);
            final float x = event.getX(pointerIndex);
            final float y = event.getY(pointerIndex);

            // Only move if the ScaleGestureDetector isn't processing a gesture.
            if (!mScaleDetector.isInProgress()) {
                final float dx = x - mLastTouchX;
                final float dy = y - mLastTouchY;

                mPosX += dx;
                mPosY += dy;

                invalidate();
            }

            mLastTouchX = x;
            mLastTouchY = y;
            
            break;
        }

        case MotionEvent.ACTION_UP: {
            mActivePointerId = INVALID_POINTER_ID;
            break;
        }

        case MotionEvent.ACTION_CANCEL: {
            mActivePointerId = INVALID_POINTER_ID;
            break;
        }

        case MotionEvent.ACTION_POINTER_UP: {
            final int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) 
                    >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
            final int pointerId = event.getPointerId(pointerIndex);
            if (pointerId == mActivePointerId) {
                // This was our active pointer going up. Choose a new
                // active pointer and adjust accordingly.
                final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                mLastTouchX = event.getX(newPointerIndex);
                mLastTouchY = event.getY(newPointerIndex);
                mActivePointerId = event.getPointerId(newPointerIndex);
            }
            break;
        }
        }*/
        return true;
    }
    
    /*private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            
            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(1.0f, Math.min(mScaleFactor, 2.0f));
            
            mListener.performZoom(mScaleFactor);
            invalidate();
            return true;
        }
    }
    
    
    // THIS CODE WILL MOST PROBABLY BE PHASED OUT SOON !!
    /*private class MediaRecorderStarter extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            checkSurfaceExists();
            stopPreview();
            startPreview();
            return null;
        }
        
    }
    
    public void checkSurfaceExists() {
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            Log.d(TAG, "Preview Surface does not exist");
            return;
        }
    }
    
    private void stopPreview() {
        // stop preview before making changes
        try {
            mCamera.stopPreview();
            
            mListener.releaseMediaRecorder();
            
            Log.d(TAG, "Camera Stopped Successfully");
        } catch (Exception e) {
            Log.d(TAG, "Error Stopping Camera, it most likely is a non-existent preview");
        }
    }
    
    private void startPreview() {
        // start preview with new settings
        try {
            //mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            
            mListener.prepareMediaRecorder();
            mListener.startMediaRecorder();
            
            
            Log.d(TAG, "Preview Started Successfully");
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }*/
}