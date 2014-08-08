package org.sebbas.android.flippycamera;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class PreferencesFragmentUI extends PreferenceFragment {

    public static final String TAG = "settings_fragment";
    public static final String SHARED_PREFS_NAME = "settings";
    
    private OnCompleteListener mViewCompleteListener;
    private View mView;
    private ListView mListView;
    
    // Static factory method that returns a new fragment instance to the client
    public static PreferencesFragmentUI newInstance() {
        PreferencesFragmentUI sf = new PreferencesFragmentUI();
        return sf;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_layout);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = super.onCreateView(inflater, container, savedInstanceState);
        return mView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "View created");
        
        if (mView != null) {
            mListView= (ListView) view.findViewById(android.R.id.list);
            mListView.setScrollBarStyle(View.GONE);
        }
        
        mViewCompleteListener.onComplete(mListView);
    }

    @Override
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
        try {
            mViewCompleteListener = (OnCompleteListener) activity;
            System.out.println("Called");
        }
        catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnCompleteListener");
        }
    }
    
    public static interface OnCompleteListener {
        public abstract void onComplete(ListView listView);
    }
    
}
