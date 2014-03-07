package org.sebbas.android.interfaces;

import android.hardware.Camera;

public interface CameraPreviewListener {
    public void performZoom(Camera camera, float scaleFactor);
    public void startRecorder();
}
