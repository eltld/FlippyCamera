package org.sebbas.android.flickcam;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class GalleryRootFragment extends Fragment {

    private static final String TAG = "root_fragment";
    private boolean mHiddenFolders;
    
    public static GalleryRootFragment newInstance(boolean hiddenFolders) {
        GalleryRootFragment galleryRootFragment = new GalleryRootFragment();
        
        Bundle args = new Bundle();
        args.putBoolean("hiddenFolders", hiddenFolders);
        galleryRootFragment.setArguments(args);
        
        return galleryRootFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        View view = inflater.inflate(R.layout.gallery_root, container, false);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        
        transaction.replace(R.id.root_frame, FolderFragment.newInstance(mHiddenFolders));
        transaction.commit();
        
        return view;
    }
    
}
