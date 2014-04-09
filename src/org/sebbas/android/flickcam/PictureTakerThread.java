package org.sebbas.android.flickcam;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class PictureTakerThread extends Thread {

	private static final String TAG = "picture_taker_thread";
	
	private Camera mCamera;
	private int mFrameByteSize;
	private Handler mHandler;
	
	public PictureTakerThread(Camera camera) {
	    mCamera = camera;
	    mFrameByteSize = getFrameByteSize();
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
}
