package com.ohb.test.com.ohb.test.pulltorefresh;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import java.util.logging.Handler;

/**
 * Created by ouhaibo on 2015/10/6.
 */
public class RefreshableView extends LinearLayout implements View.OnTouchListener{
    public RefreshableView(Context context) {
        super(context);
        init();
    }

    public RefreshableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RefreshableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }
}
