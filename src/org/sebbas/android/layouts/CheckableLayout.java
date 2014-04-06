package org.sebbas.android.layouts;

import org.sebbas.android.flickcam.R;

import android.content.Context;
import android.widget.Checkable;
import android.widget.FrameLayout;

public class CheckableLayout extends FrameLayout implements Checkable {

	private boolean mChecked;
	
	public CheckableLayout(Context context) {
		super(context);
	}

	@Override
	public void setChecked(boolean checked) {
		mChecked = checked;
		this.setBackgroundResource(checked ? R.drawable.image_border : null);
	}

	@Override
	public boolean isChecked() {
		return mChecked;
	}

	@Override
	public void toggle() {
		setChecked(!mChecked);
	}

}
