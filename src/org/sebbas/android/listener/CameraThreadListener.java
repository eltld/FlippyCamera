package org.sebbas.android.listener;

import android.hardware.Camera;

public interface CameraThreadListener {

	public void alertCameraThreadError(String message);
	public void cameraSetupComplete(Camera camera, int cameraID);
	public void newPictureAddedToGallery();
}
