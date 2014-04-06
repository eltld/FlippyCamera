package org.sebbas.android.views;

import org.sebbas.android.flickcam.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public final class SquaredImageView extends ImageView {

    public SquaredImageView(Context context) {
        super(context);
        this.setBackgroundResource(R.drawable.square_image_selector);
    }

    public SquaredImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public SquaredImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override 
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
    }
}