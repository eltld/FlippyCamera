package org.sebbas.android.views;

import java.util.ArrayList;
import java.util.List;

import org.sebbas.android.adapter.FolderViewImageAdapter;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

public class GalleryGridView extends GridView {

	private OnGalleryRefreshCallback mOnGalleryRefreshCallback;
	private ArrayList<List<String>> mImagePaths = new ArrayList<List<String>>();
	private FolderViewImageAdapter mAdapter;
	
	public GalleryGridView(Context context) {
		super(context);
	}
	
	public GalleryGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
 
    public GalleryGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
	
	public void setup() {
		
	}
	
	public void refresh() {
		
	}
	
	public void setRefreshListener(OnGalleryRefreshCallback onGalleryRefreshCallback) {
		mOnGalleryRefreshCallback = onGalleryRefreshCallback;
	}
	
	public void setSetupListener(OnGallerySetupCallback OnGallerySetupCallback) {
		
	}
	
	public interface OnGalleryRefreshCallback {
		public void onGalleryUpdated();
	}
	
	public interface OnGallerySetupCallback {
		public void onGallerySetup();
	}

}
