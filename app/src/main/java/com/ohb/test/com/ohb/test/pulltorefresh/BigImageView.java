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
            BitmapFactory.decodeStream(in, null, options);
            mPictureWidth = options.outWidth;
            mPictureHeight = options.outHeight;
            mOptions = new BitmapFactory.Options();
            mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
            mOptions.inMutable = true;
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
        mOptions.inBitmap = bm;
        canvas.drawBitmap(bm, 0, 0, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                updatePoint(event, curPoint);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                updatePoint(curPoint, prePoint);
                updatePoint(event, curPoint);
                mRect.offset((int) (prePoint.x - curPoint.x), (int) (prePoint.y - curPoint.y));
                if (mRect.left < 0) {
                    mRect.left = 0;
                    mRect.right = getWidth();
                }
                if (mRect.right > mPictureWidth) {
                    mRect.left = mPictureWidth - getWidth();
                    mRect.right = mPictureWidth;
                }
                if (mRect.top < 0) {
                    mRect.top = 0;
                    mRect.bottom = getHeight();
                }
                if (mRect.bottom > mPictureHeight) {
                    mRect.top = mPictureHeight - getHeight();
                    mRect.bottom = mPictureHeight;
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
            default:
                break;
        }
        super.onTouchEvent(event);
        return true;
    }

    private void updatePoint(MotionEvent event, PointF pointF) {
        pointF.x = event.getX();
        pointF.y = event.getY();
    }

    private void updatePoint(PointF src, PointF dst) {
        dst.x = src.x;
        dst.y = src.y;
    }
}
