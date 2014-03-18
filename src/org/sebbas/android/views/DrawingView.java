package org.sebbas.android.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;

public class DrawingView extends View {

	private static final String TAG = "drawing_view";
    private static final float STROKE_WIDTH = 4;
    
    private Paint mDrawingPaint;
    private boolean mHaveTouch;
    private Rect mTouchArea;
    
    public DrawingView(Context context) {
        super(context);
            mDrawingPaint = new Paint();
            mDrawingPaint.setColor(Color.WHITE);
            mDrawingPaint.setStyle(Paint.Style.STROKE);
            mDrawingPaint.setStrokeWidth(STROKE_WIDTH);
            
            mHaveTouch = false;
    }
    
    public void setHaveTouch(boolean touch, Rect touchArea) {
        mHaveTouch = touch;
        mTouchArea = touchArea;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if(mHaveTouch) {
        	Log.d(TAG, "Showing touch rect");
            canvas.drawRect(mTouchArea.left, mTouchArea.top, mTouchArea.right, mTouchArea.bottom, mDrawingPaint);
        }
    }
}
