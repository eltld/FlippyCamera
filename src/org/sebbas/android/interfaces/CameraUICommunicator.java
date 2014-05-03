package org.sebbas.android.interfaces;

import android.graphics.Rect;

public interface CameraUICommunicator {

	public void alertCameraThread(String message);
	public void cameraSetupComplete(int cameraID);
	public void newPictureAddedToGallery();
	public void setTouchFocusView(Rect tFocusRect);
	public void makeFlashAnimation();
}
