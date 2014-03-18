package org.sebbas.android.listener;

import android.graphics.Rect;

public interface CameraThreadListener {

	public void alertCameraThreadError(String message);
	public void cameraSetupComplete(int cameraID);
	public void newPictureAddedToGallery();
	public void setTouchFocusView(Rect tFocusRect);
}
