package org.sebbas.android.flickcam;

import java.util.ArrayList;

import org.sebbas.android.adapter.GridViewImageAdapter;
import org.sebbas.android.helper.AppConstant;
import org.sebbas.android.helper.Utils;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class GalleryFragment extends Fragment {

    public static final String TAG = "gallery_fragment";
    
    private Utils mUtils;
    private ArrayList<String> mImagePaths = new ArrayList<String>();
    private GridViewImageAdapter mAdapter;
    private GridView mGridView;
    private int mColumnWidth;
    
    // Static factory method that returns a new fragment instance to the client
    public static GalleryFragment newInstance() {
        GalleryFragment gf = new GalleryFragment();
        return gf;
    }
    
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    
        GridView gridView = (GridView)inflater.inflate(R.layout.gallery_grid_view, container, false);
        mGridView = (GridView)gridView.findViewById(R.id.grid_view);
        mUtils = new Utils(this.getActivity());
        
        setupGridView();
        
        return gridView;
    }

    public void setupGridView() {
        initilizeGridLayout();
        setGridViewAdapter();
        setGridViewClickListener();
    }

    private void setGridViewClickListener() {
        mGridView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position,
                    long id) {
                Intent i = new Intent(GalleryFragment.this.getActivity(), FullScreenViewActivity.class);
                i.putExtra("position", position);
                System.out.println("Clicked on position " + position);
                GalleryFragment.this.getActivity().startActivity(i);
                
            }
        });
    }

    private void setGridViewAdapter() {
        // loading all image paths from SD card
        mImagePaths = mUtils.getFilePaths();
 
        // Gridview adapter
        mAdapter = new GridViewImageAdapter(this.getActivity(), mImagePaths, mColumnWidth);
 
        // setting grid view adapter
        mGridView.setAdapter(mAdapter);
    }
    
    private void initilizeGridLayout() {
        Resources r = getResources();
        float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, AppConstant.GRID_PADDING, r.getDisplayMetrics());
 
        mColumnWidth = (int) ((mUtils.getScreenWidth() - ((AppConstant.NUM_OF_COLUMNS + 1) * padding)) / AppConstant.NUM_OF_COLUMNS);
 
        mGridView.setNumColumns(AppConstant.NUM_OF_COLUMNS);
        mGridView.setColumnWidth(mColumnWidth);
        mGridView.setStretchMode(GridView.NO_STRETCH);
        mGridView.setPadding((int) padding, (int) padding, (int) padding, (int) padding);
        mGridView.setHorizontalSpacing((int) padding);
        mGridView.setVerticalSpacing((int) padding);
    }
}
