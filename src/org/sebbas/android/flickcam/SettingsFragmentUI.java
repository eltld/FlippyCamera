package org.sebbas.android.flickcam;

import org.sebbas.android.views.DrawInsetsFrameLayout;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class SettingsFragmentUI extends Fragment {

	public static final String TAG = "settings_fragment";
    public static final String SHARED_PREFS_NAME = "settings";
    
    private LinearLayout mLinearLayout;
    
    private Context mContext;
    private MainFragmentActivity mMainFragment;
    private FrameLayout mFrameLayout;
    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    
    // Static factory method that returns a new fragment instance to the client
    public static SettingsFragmentUI newInstance() {
        SettingsFragmentUI sf = new SettingsFragmentUI();
        return sf;
    }
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getActivity();
        mMainFragment = (MainFragmentActivity) this.getActivity();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    
        mFrameLayout = (FrameLayout) inflater.inflate(R.layout.settings_layout, container, false);
        mLinearLayout = (LinearLayout) mFrameLayout.findViewById(R.id.settings_container);
        mDrawInsetsFrameLayout = (DrawInsetsFrameLayout) mFrameLayout.findViewById(R.id.draw_insets_framelayout);
        
        mMainFragment.getSupportActionBar().setHomeButtonEnabled(false);
        mMainFragment.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        
        mDrawInsetsFrameLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {
            @Override
            public void onInsetsChanged(Rect insets) {
                // Update the padding
                mLinearLayout.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            }
        });
        
        this.getFragmentManager().beginTransaction().add(mLinearLayout.getId(), SettingsFragment.newInstance()).commit();
        
        return mFrameLayout;
    }
    
}
