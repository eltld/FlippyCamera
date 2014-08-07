package org.sebbas.android.flippycamera;

import org.sebbas.android.helper.AppConstant;
import org.sebbas.android.views.DrawInsetsFrameLayout;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.preference.PreferenceFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class PreferencesFragmentUI extends PreferenceFragment {

	public static final String TAG = "settings_fragment";
    public static final String SHARED_PREFS_NAME = "settings";
    
    private RelativeLayout mRelativeLayout;
    
    private Context mContext;
    private MainFragmentActivity mMainFragment;
    private FrameLayout mFrameLayout;
    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    
    // Static factory method that returns a new fragment instance to the client
    public static PreferencesFragmentUI newInstance() {
        PreferencesFragmentUI sf = new PreferencesFragmentUI();
        return sf;
    }
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getActivity();
        //mMainFragment = (MainFragmentActivity) this.getActivity();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    
    	Resources r = getResources();
    	final float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, AppConstant.GRID_PADDING, r.getDisplayMetrics());
    	
        mFrameLayout = (FrameLayout) inflater.inflate(R.layout.settings_layout, container, false);
        mRelativeLayout = (RelativeLayout) mFrameLayout.findViewById(R.id.settings_container);
        mDrawInsetsFrameLayout = (DrawInsetsFrameLayout) mFrameLayout.findViewById(R.id.draw_insets_framelayout);
        
        //mMainFragment.getSupportActionBar().setHomeButtonEnabled(false);
        //mMainFragment.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        
        mDrawInsetsFrameLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {
            @Override
            public void onInsetsChanged(Rect insets) {
                // Update the padding
            	mFrameLayout.setPadding(insets.left, 146, insets.right, 96);
            }
        });
        
        this.getFragmentManager().beginTransaction().add(mRelativeLayout.getId(), SettingsFragment.newInstance()).commit();
        ((GalleryActivity) this.getActivity()).getDrawInsetsFrameLayout().bringToFront();
        
        return mFrameLayout;
    }
    
}
