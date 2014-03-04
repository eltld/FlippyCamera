package org.sebbas.android.flickcam;

import org.sebbas.android.adapter.FullScreenImageAdapter;
import org.sebbas.android.helper.Utils;
import org.sebbas.android.viewpager.ZoomOutPageTransformer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

public class FullScreenViewActivity extends Activity{

    private Utils mUtils;
    private FullScreenImageAdapter mAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_fullscreen_view);

        mViewPager = (ViewPager) findViewById(R.id.pager);

        mUtils = new Utils(getApplicationContext());

        Intent i = getIntent();
        int position = i.getIntExtra("position", 0);

        mAdapter = new FullScreenImageAdapter(FullScreenViewActivity.this,
                mUtils.getFilePaths());

        mViewPager.setAdapter(mAdapter);
        mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());
        // displaying selected image first
        mViewPager.setCurrentItem(position);
    }
}