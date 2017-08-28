package com.ohb.test.com.ohb.test.pulltorefresh;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Administrator on 2017/8/28.
 */

public class BigImageView extends ImageView {
    private BitmapRegionDecoder mRegionDecoder;
    private BitmapFactory.Options mOptions;
    private int mPictureWidth, mPictureHeight;
    private Rect mRect;
    private Paint mPaint;
    private PointF prePoint, curPoint;

    public BigImageView(Context context) {
        super(context);
    }

    public BigImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BigImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void init(InputStream in) {
        try {
            mRegionDecoder = BitmapRegionDecoder.newInstance(in, true);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            Rect r = new Rect();
            BitmapFactory.decodeStream(in, r, options);
            mPictureWidth = r.width();
            mPictureHeight = r.height();
            mOptions = new BitmapFactory.Options();
            mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
            mPaint = new Paint();
            mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
            prePoint = new PointF();
            curPoint = new PointF();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BigImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mRect == null) {
            mRect = new Rect(0, 0, getMeasuredWidth(), getMeasuredHeight());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Bitmap bm = mRegionDecoder.decodeRegion(mRect, mOptions);
        canvas.drawBitmap(bm, 0, 0, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                updatePoint(event,curPoint);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
            default:
                break;
        }
        super.onTouchEvent(event);
        invalidate();
        return true;
    }

    private void updatePoint(MotionEvent event, PointF pointF) {
        pointF.x = event.getX();
        pointF.y = event.getY();
    }
}
