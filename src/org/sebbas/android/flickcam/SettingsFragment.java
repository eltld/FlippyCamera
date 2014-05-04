package org.sebbas.android.flickcam;

import org.sebbas.android.adapter.GridViewImageAdapter;
import org.sebbas.android.helper.Utils;
import org.sebbas.android.views.DrawInsetsFrameLayout;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridView;


public class SettingsFragment extends Fragment {

    public static final String TAG = "settings_fragment";
    
    private Utils mUtils;
    private GridViewImageAdapter mAdapter;
    private GridView mGridView;
    
    private Context mContext;
    private MainFragmentActivity mMainFragment;
    private FrameLayout mFrameLayout;
    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    
    // Static factory method that returns a new fragment instance to the client
    public static SettingsFragment newInstance() {
        SettingsFragment gf = new SettingsFragment();
        return gf;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getActivity();
        mMainFragment = (MainFragmentActivity) this.getActivity();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    
        mFrameLayout = (FrameLayout) inflater.inflate(R.layout.gallery_grid_view, container, false);
        mGridView = (GridView) mFrameLayout.findViewById(R.id.grid_view);
        mUtils = new Utils(this.getActivity());
        mDrawInsetsFrameLayout = (DrawInsetsFrameLayout) mFrameLayout.findViewById(R.id.draw_insets_framelayout);
        
        // Turn on the "up" back navigation option
        mMainFragment.getSupportActionBar().setHomeButtonEnabled(false);
        mMainFragment.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        
        mDrawInsetsFrameLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {
            @Override
            public void onInsetsChanged(Rect insets) {
                // Update the padding
                mGridView.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            }
        });
        
        return mFrameLayout;
    }
    
}
