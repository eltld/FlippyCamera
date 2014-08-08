package org.sebbas.android.flippycamera;

import org.sebbas.android.views.DrawInsetsFrameLayout;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

public class SettingsActivity extends ActionBarActivity implements PreferencesFragmentUI.OnCompleteListener {

    private static final String TAG = "settings_activity";
    
    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        
        // Variables for the UI
        mDrawInsetsFrameLayout = (DrawInsetsFrameLayout) findViewById(R.id.draw_insets_framelayout);
        
        // Add the preference fragment to the layout container
        addPreferenceFragment();
        
        this.getSupportActionBar().setHomeButtonEnabled(true);
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gallery, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                return true;
            default: 
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handleNavigationBack();
    }


    @Override
    public boolean onSupportNavigateUp() {
        super.onSupportNavigateUp();
        handleNavigationBack();
        return true;
    }
    
    private void handleNavigationBack() {
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();        
    }

    @Override
    public void onComplete(final ListView listView) {
        mDrawInsetsFrameLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {
            @Override
            public void onInsetsChanged(Rect insets) {
                // Update the padding
                listView.setPadding(listView.getPaddingLeft(), insets.top, listView.getPaddingRight(), insets.bottom);
            }
        });
    }
    
    private void addPreferenceFragment() {
    	PreferencesFragmentUI preferencesFragmentUI = PreferencesFragmentUI.newInstance();
        FragmentManager fm  = getSupportFragmentManager();
        fm.beginTransaction()
            .add(R.id.settings_container, preferencesFragmentUI)
            .commit();
    }
}
