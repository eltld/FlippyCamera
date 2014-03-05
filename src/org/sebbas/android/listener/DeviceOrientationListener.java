package org.sebbas.android.listener;

import android.content.Context;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

public class DeviceOrientationListener extends OrientationEventListener {

    private Context mContext;
    private int mDeviceRotation;
    
    public DeviceOrientationListener(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (orientation < 45 && orientation >= 315) {
            mDeviceRotation = 90;
        } else if (orientation >= 45 && orientation < 135) {
            mDeviceRotation = 180;
        } else if (orientation >= 135 && orientation < 225) {
            
        } else {
            
        }
    }
    
    public void enableOrientationListener() {
        if (this.canDetectOrientation()) {
            this.enable();
        }
    }
    
    public void disableOrientationListener() {
       this.disable();
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

}
