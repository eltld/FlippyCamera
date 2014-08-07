package org.sebbas.android.views;

import org.sebbas.android.flippycamera.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class CascadeLayout extends ViewGroup {

    private int mHorizontalSpacing;
    private int mVerticalSpacing;

    public CascadeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        //this.setBackgroundResource(R.drawable.square_image_selector);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CascadeLayout);

        try {
            mHorizontalSpacing = a.getDimensionPixelSize(
                    R.styleable.CascadeLayout_horizontal_spacing, getResources().getDimensionPixelSize(
                            R.dimen.cascade_horizontal_spacing));

            mVerticalSpacing = a.getDimensionPixelSize(
                    R.styleable.CascadeLayout_vertical_spacing, getResources().getDimensionPixelSize(
                            R.dimen.cascade_vertical_spacing));
        } finally {
            a.recycle();
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getPaddingLeft();
        int height = getPaddingTop();

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {

            View child = getChildAt(i);
            measureChild(child, widthMeasureSpec, heightMeasureSpec);

            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            width = getPaddingLeft() + mHorizontalSpacing * (count -1 -i) ;
            height = getPaddingTop() + mVerticalSpacing * (count -1 -i);

            lp.x = width;
            lp.y = height;
        }

        width = getChildAt(0).getLayoutParams().width + getChildAt(0).getMeasuredWidth() + getPaddingRight();
        height = getChildAt(getChildCount() - 1).getMeasuredHeight() + getPaddingBottom();

        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();

            child.layout(lp.x, lp.y, lp.x + child.getMeasuredWidth(), lp.y + child.getMeasuredHeight());
        }
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p.width, p.height);
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {
        int x;
        int y;
        public int verticalSpacing;

        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);

            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CascadeLayout_LayoutParams);
            
            try {
                verticalSpacing = a.getDimensionPixelSize(R.styleable.CascadeLayout_LayoutParams_layout_vertical_spacing, -1);
            } finally {
                a.recycle();
            }
        }

        public LayoutParams(int w, int h) {
            super(w, h);
        }
    }
}