package org.sebbas.android.listener;

import android.content.Context;
import android.view.OrientationEventListener;

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

}
