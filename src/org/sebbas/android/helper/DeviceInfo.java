package org.sebbas.android.helper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.view.Surface;
import android.view.ViewConfiguration;
import android.view.WindowManager;

public class DeviceInfo {

    private Context mContext;

    public DeviceInfo(Context context) {
        mContext = context;
    }
    
    // Check to see if the device supports the indicated SDK
    public static boolean supportsSDK(int sdk) {
        if (android.os.Build.VERSION.SDK_INT >= sdk) {
            return true;
        } 
        return false;
    }
    
     // Check if the device has soft buttons
    @SuppressLint("NewApi")
    public static boolean hasSoftButtons(Context context) {
        if (supportsSDK(14)) {
            return !ViewConfiguration.get(context).hasPermanentMenuKey();
        }
        return false;
    }
    
    public static int getDeviceRotation(Context context) {
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
    
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}
