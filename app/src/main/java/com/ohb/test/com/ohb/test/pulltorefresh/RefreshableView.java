package com.ohb.test.com.ohb.test.pulltorefresh;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ohb.test.R;

/**
 * Created by ouhaibo on 2015/10/6.
 */
public class RefreshableView extends LinearLayout implements View.OnTouchListener {
    public static final int STATUS_PULL_TO_REFRESH = 0;
    public static final int STATUS_RELEASE_TO_REFRESH = 1;
    public static final int STATUS_REFRESHING = 2;
    public static final int STATUS_REFRESH_FINISHED = 3;
    public static final int SCROLL_BACK_SPEED = 20;
    private View mHeader;
    private ListView mListView;
    private ProgressBar mProgressBar;
    private ImageView mImgArrow;
    private TextView mTxtDesc;
    private MarginLayoutParams headerLayoutParams;
    private int id = -1;
    private int hideHeaderHeight;
    private int currentStatus = STATUS_REFRESH_FINISHED;
    private int lastStatus = currentStatus;
    private int touchSlop;
    private float yDown;
    private boolean loadOnce;
    private boolean ableToPull;
    private PullToRefreshListenr mListener;

    public RefreshableView(Context context) {
        super(context);
        init(context);
    }

    public RefreshableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RefreshableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mHeader = LayoutInflater.from(context).inflate(R.layout.pull_to_refresh_layout, null, true);
        mProgressBar = (ProgressBar) mHeader.findViewById(R.id.progress_bar);
        mImgArrow = (ImageView) mHeader.findViewById(R.id.arrow);
        mTxtDesc = (TextView) mHeader.findViewById(R.id.description);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setOrientation(VERTICAL);
        addView(mHeader, 0);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed && !loadOnce) {
            hideHeaderHeight = -mHeader.getHeight();
            headerLayoutParams = (MarginLayoutParams) mHeader.getLayoutParams();
            headerLayoutParams.topMargin = hideHeaderHeight;
            mListView = (ListView) getChildAt(1);
            mListView.setOnTouchListener(this);
            loadOnce = true;

        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        setIsAbleToPull(event);
        if (ableToPull) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    yDown = event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float yMove = event.getRawY();
                    int d = (int) (yMove - yDown);
                    if (d <= 0 && headerLayoutParams.topMargin <= hideHeaderHeight) {
                        return false;
                    }
                    if (d <= touchSlop) {
                        return false;
                    }
                    if (currentStatus != STATUS_REFRESHING) {
                        if (headerLayoutParams.topMargin <= 0) {
                            currentStatus = STATUS_PULL_TO_REFRESH;
                        } else {
                            currentStatus = STATUS_RELEASE_TO_REFRESH;
                        }
                        headerLayoutParams.topMargin = (d / 2) + hideHeaderHeight;
                        mHeader.setLayoutParams(headerLayoutParams);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                default:
                    if (currentStatus == STATUS_PULL_TO_REFRESH) {
                        new HideHeaderTask().execute();
                    } else if (currentStatus == STATUS_RELEASE_TO_REFRESH) {
                        new RefreshingTask().execute();
                    }
                    break;
            }
            if (currentStatus == STATUS_PULL_TO_REFRESH || currentStatus == STATUS_RELEASE_TO_REFRESH) {
                updateHeaderView();
                mListView.setPressed(false);
                mListView.setFocusable(false);
                mListView.setFocusableInTouchMode(false);
                lastStatus = currentStatus;
                return true;
            }

        }
        return false;
    }

    private void updateHeaderView() {
        if (lastStatus != currentStatus) {
            if (currentStatus == STATUS_PULL_TO_REFRESH) {
                mTxtDesc.setText(getResources().getString(R.string.pull_to_refresh));
                mImgArrow.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
                rotateArrow();
            } else if (currentStatus == STATUS_RELEASE_TO_REFRESH) {
                mTxtDesc.setText(getResources().getString(R.string.release_to_refresh));
                mImgArrow.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
                rotateArrow();
            } else if (currentStatus == STATUS_REFRESHING) {
                mTxtDesc.setText(getResources().getString(R.string.refreshing));
                mProgressBar.setVisibility(View.VISIBLE);
                mImgArrow.clearAnimation();
                mImgArrow.setVisibility(View.GONE);
            }
        }
    }

    public void setOnFinishListener(int id, PullToRefreshListenr listenr) {
        mListener = listenr;
        this.id = id;
    }


    public void finishRefreshing() {
        currentStatus = STATUS_REFRESH_FINISHED;
        new HideHeaderTask().execute();
    }


    private void rotateArrow() {
        float pivotX = mImgArrow.getWidth() / 2f;
        float pivotY = mImgArrow.getHeight() / 2f;
        float fromDegress = 0f;
        float toDegress = 0f;
        if (currentStatus == STATUS_PULL_TO_REFRESH) {
            fromDegress = 180f;
            toDegress = 360f;
        } else if (currentStatus == STATUS_RELEASE_TO_REFRESH) {
            fromDegress = 0f;
            toDegress = 180f;
        }
        RotateAnimation anim = new RotateAnimation(fromDegress, toDegress, pivotX, pivotY);
        anim.setDuration(500);
        anim.setFillAfter(true);
        mImgArrow.startAnimation(anim);
    }

    private void setIsAbleToPull(MotionEvent event) {
        View firstChild = mListView.getChildAt(0);
        if (firstChild != null) {
            int firstVisiblePos = mListView.getFirstVisiblePosition();
            if (firstVisiblePos == 0 && firstChild.getTop() == 0) {
                if (!ableToPull) {
                    yDown = event.getRawY();
                }
                ableToPull = true;
            } else {
                if (headerLayoutParams.topMargin != hideHeaderHeight) {
                    headerLayoutParams.topMargin = hideHeaderHeight;
                    mHeader.setLayoutParams(headerLayoutParams);
                }
                ableToPull = false;
            }
        } else {
            ableToPull = true;
        }
    }

    public interface PullToRefreshListenr {
        void onRefresh();
    }

    class RefreshingTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            int topMargin = headerLayoutParams.topMargin;
            while (topMargin > 0) {
                topMargin -= SCROLL_BACK_SPEED;
                if (topMargin < 0) {
                    topMargin = 0;
                }
                publishProgress(topMargin);
            }
            currentStatus = STATUS_REFRESHING;
            if (mListener != null) {
                mListener.onRefresh();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            headerLayoutParams.topMargin = values[0];
            mHeader.setLayoutParams(headerLayoutParams);
        }
    }

    class HideHeaderTask extends AsyncTask<Void, Integer, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {
            int topMargin = headerLayoutParams.topMargin;
            while (true) {
                topMargin -= SCROLL_BACK_SPEED;
                if (topMargin <= hideHeaderHeight) {
                    topMargin = hideHeaderHeight;
                    break;
                }
                publishProgress(topMargin);
            }
            return topMargin;
        }

        @Override
        protected void onProgressUpdate(Integer... topMargin) {
            headerLayoutParams.topMargin = topMargin[0];
            mHeader.setLayoutParams(headerLayoutParams);
        }

        @Override
        protected void onPostExecute(Integer topMargin) {
            headerLayoutParams.topMargin = topMargin;
            mHeader.setLayoutParams(headerLayoutParams);
            currentStatus = STATUS_REFRESH_FINISHED;
        }
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
