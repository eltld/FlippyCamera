package org.sebbas.android.flippycamera;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.support.v4.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment {

    // Static factory method that returns a new fragment instance to the client
    public static SettingsFragment newInstance() {
        SettingsFragment sf = new SettingsFragment();
        return sf;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_layout);
    }
}
