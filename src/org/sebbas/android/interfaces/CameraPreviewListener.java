package org.sebbas.android.interfaces;

public interface CameraPreviewListener {
    public void performZoom(float scaleFactor);
    public void prepareMediaRecorder();
    public void releaseMediaRecorder();
    public void startMediaRecorder();
}
