package org.sebbas.android.helper;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Environment;
import android.view.Display;
import android.view.Surface;
import android.view.ViewConfiguration;
import android.view.WindowManager;

public class DeviceInfo {

    private Context mContext;
    private Point mSize;
    private Display mDisplay;

    public DeviceInfo(Context context) {
        mContext = context;
        mSize = new Point();
        mDisplay = ((Activity) context).getWindowManager().getDefaultDisplay();
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
    
    // Helper method for printing out camera values. Mainly used for debugging
    public static String listToString(List<int[]> sizes) {
        String result = "Range is [";
        for (int[] entry : sizes) {
            int low = entry[0];
            int high = entry[1];
            result += "(" + low + "/" + high + ")";
        }
        return result += "]";
    }
    
    public static boolean supportsAutoFocus(Parameters parameters) {
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes == null) return false;
        if (focusModes.contains((String)Camera.Parameters.FOCUS_MODE_AUTO)) {
            return true;
        }
        return false;
    }
    
    public static boolean supportsFlash(Parameters parameters) {
        List<String> flashModes = parameters.getSupportedFlashModes();
        if (flashModes == null) return false;
        if (flashModes.contains((String)Camera.Parameters.FLASH_MODE_ON) 
                && flashModes.contains((String)Camera.Parameters.FLASH_MODE_OFF)) {
            return true;
        }
        return false;
    }
    
    public static boolean supportsWhiteBalance(Parameters parameters) {
        List<String> whiteBalanceModes = parameters.getSupportedWhiteBalance();
        if (whiteBalanceModes == null) return false;
        if (whiteBalanceModes.contains((String)Camera.Parameters.WHITE_BALANCE_AUTO)) { 
            return true;
        }
        return false;
    }
    
    @SuppressLint("NewApi")
    public static int getRealScreenWidth(Context context) {
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        int width;
        try { 
            display.getRealSize(size);
            width = size.x; 
        } catch (NoSuchMethodError e) {
            width = display.getWidth();
        }
        return width;
    }
    
    @SuppressLint("NewApi")
    public static int getRealScreenHeight(Context context) {
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        int height;
        try { 
            display.getRealSize(size);
            height = size.y; 
        } catch (NoSuchMethodError e) {
            height = display.getHeight();
        }
        return height;
    }
    
    @SuppressLint("NewApi")
	public static int getScreenWidth(Context context) {
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        int width;
        try { 
            display.getSize(size);
            width = size.x; 
        } catch (NoSuchMethodError e) {
            width = display.getWidth();
        }
        return width;
    }
    
    @SuppressLint("NewApi")
	public static int getScreenHeight(Context context) {
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        int height;
        try { 
            display.getSize(size);
            height = size.y; 
        } catch (NoSuchMethodError e) {
            height = display.getHeight();
        }
        return height;
    }
}
