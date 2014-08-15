package org.sebbas.android.adapter;

import java.util.ArrayList;

import org.sebbas.android.flippycamera.R;
import org.sebbas.android.views.NavigationDrawerItem;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
 
public class NavigationDrawerListAdapter extends BaseAdapter {
     
    private Context mContext;
    private ArrayList<NavigationDrawerItem> mNavDrawerItems;
     
    public NavigationDrawerListAdapter(Context context, ArrayList<NavigationDrawerItem> navDrawerItems){
        this.mContext = context;
        this.mNavDrawerItems = navDrawerItems;
    }
 
    @Override
    public int getCount() {
        return mNavDrawerItems.size();
    }
 
    @Override
    public Object getItem(int position) {
        return mNavDrawerItems.get(position);
    }
 
    @Override
    public long getItemId(int position) {
        return position;
    }
 
    @SuppressLint("NewApi") @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater)
                    mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.drawer_list_item, null);
        }
          
        ImageView imgIcon = (ImageView) convertView.findViewById(R.id.drawer_item_icon);
        TextView txtTitle = (TextView) convertView.findViewById(R.id.drawer_item_text);
        TextView txtCount = (TextView) convertView.findViewById(R.id.drawer_item_extra);
          
        imgIcon.setImageResource(mNavDrawerItems.get(position).getIcon());
        txtTitle.setText(mNavDrawerItems.get(position).getTitle());
        
         
        // displaying count
        // check whether it set visible or not
        if(mNavDrawerItems.get(position).getCounterVisibility()){
            txtCount.setText(mNavDrawerItems.get(position).getCount());
        }else{
            // hide the counter view
            txtCount.setVisibility(View.GONE);
        }
         
        return convertView;
    }
 
}