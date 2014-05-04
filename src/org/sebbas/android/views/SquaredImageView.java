package org.sebbas.android.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public final class SquaredImageView extends ImageView {

    public SquaredImageView(Context context) {
        super(context);
        this.setPadding(2, 2, 2, 2); // Padding to make the border show up
    }

    public SquaredImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setPadding(2, 2, 2, 2);
    }
    
    public SquaredImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setPadding(2, 2, 2, 2);
    }

    @Override 
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
    }
}