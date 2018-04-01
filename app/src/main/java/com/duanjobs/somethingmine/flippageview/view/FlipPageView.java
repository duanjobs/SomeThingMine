package com.duanjobs.somethingmine.flippageview.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.Scroller;
import com.duanjobs.somethingmine.R;
import java.lang.reflect.Field;
import java.security.InvalidParameterException;

/**
 * Created by duanjobs on 18/2/1.
 */

public class FlipPageView extends FrameLayout implements Runnable{
    private static String TAG = "FlipPageView";

    public enum Gesture {
        NONE, VERTICAL, HORIZONTAL;
    }
    public enum PageFlipTo{
        NONE, PREVIOUS, NEXT;
    }

    private int mPageWidth;
    private int mPageIndex,mPageCount = Integer.MAX_VALUE;
    private float mScrollStartX;
    private float mCurPointX,mLastPointX,mInitPointX;
    private PageContainerView mCurPage,mTogglePage,mMovePage;
    private PageFlipTo mFlipTo;
    private Gesture mGesture;
    private boolean mPageDragged;
    private boolean mPageChange;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private ViewConfiguration mConfiguration;
    private VelocityTracker mVelocityTracker;
    private FlipPageListener mFlipPageListener;
    private Scroller mScroller;
    private View mRecyclePageView;
    private Boolean allowGesture;

    public FlipPageView(Context context) {
        this(context,null);
    }

    public FlipPageView(Context context, AttributeSet attrs) {
        this(context,attrs,0);
    }

    public FlipPageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.FlipPageView);
        allowGesture = ta.getBoolean(R.styleable.FlipPageView_fpv_allow_gesture,true);
        ta.recycle();
        mConfiguration = ViewConfiguration.get(getContext());
        mTouchSlop = mConfiguration.getScaledTouchSlop() * 2;
        mMinimumVelocity = mConfiguration.getScaledMinimumFlingVelocity();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mPageWidth = getMeasuredWidth();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mCurPage = new PageContainerView(getContext());
        addView(mCurPage,params);

        mTogglePage = new PageContainerView(getContext());
        addView(mTogglePage,params);

        mCurPage.bringToFront();

        mScroller = new Scroller(getContext());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if(mScroller != null) {
            mScroller.abortAnimation();
            mScroller = null;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if(!allowGesture)
            return super.dispatchTouchEvent(event);
        boolean handled = false;
        int action = event.getAction();

        checkPageIndexValid();
        if(mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        switch(action) {
            case MotionEvent.ACTION_DOWN: {

                stopScrollPage();

                mScrollStartX = 0;
                mPageDragged = false;
                mFlipTo = PageFlipTo.NONE;
                mGesture = Gesture.NONE;
                mLastPointX = mCurPointX = mInitPointX = event.getX();
            }
            break;

            case MotionEvent.ACTION_MOVE: {

                mCurPointX = event.getX();

                if(mGesture == Gesture.NONE) {
                    if(mCurPage.checkScrollViewDraged()) {
                        mGesture = Gesture.VERTICAL;
                    }else if(checkPageDraged(event)) {
                        mGesture = Gesture.HORIZONTAL;
                    }
                }

                if(mGesture == Gesture.HORIZONTAL) {
                    checkPageDraged(event);
                    if(checkPageFlipTo(event)) {
                        dragPage();
                    }
                    handled = true;
                }

                mLastPointX = mCurPointX;
            }
            break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {

                mVelocityTracker.computeCurrentVelocity(1000);
                float velocity = mVelocityTracker.getXVelocity();
                if(mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }

                if(mPageDragged) {
                    if(!checkFlingPage(velocity)) {
                        checkScrollPage();
                    }
                }
                handled = mGesture == Gesture.HORIZONTAL ? true : handled;
            }
        }

        if(!handled) {
            handled = super.dispatchTouchEvent(event);
        }
        return handled;
    }

    @Override
    public void run() {
        scrollPage();
    }

    /**
     * 检查页面是否可以滑动
     * @param event
     * @return
     */
    private boolean checkPageDraged(MotionEvent event) {
        float deltaX = Math.abs(event.getX() - mInitPointX);

        if(!mPageDragged && deltaX > mTouchSlop) {
            mPageDragged = true;
        }

        return mPageDragged;
    }

    /**
     * 检查页面切换，上一页 or 下一页
     * @param event
     * @return true: 能滑动; false: 不能滑动
     */
    private boolean checkPageFlipTo(MotionEvent event) {
        boolean canflip = false;
        PageFlipTo original = mFlipTo;

        if(mCurPointX >= mInitPointX) {
            mFlipTo = PageFlipTo.PREVIOUS;
        }else if(mCurPointX < mInitPointX) {
            mFlipTo = PageFlipTo.NEXT;
        }
        if(mFlipTo == PageFlipTo.PREVIOUS) {
            canflip = mPageIndex - 1 < 0 ? false : true;
        }else if(mFlipTo == PageFlipTo.NEXT) {
            canflip = mPageIndex + 1 >= mPageCount ? false : true;
        }

        if(canflip && original != mFlipTo) {
            flipPageInit();
        }

        if(!canflip) {
            mPageDragged = false;
            mInitPointX = event.getX();
        }

        return canflip;
    }

    /**
     * 翻页初始化
     */
    private void flipPageInit() {
        View newPage;
        int pageindex;

        if(mFlipPageListener == null) {
            return;
        }

        if(mFlipTo == PageFlipTo.PREVIOUS) {
            pageindex = mPageIndex - 1 < 0 ? 0 : mPageIndex - 1;

            newPage = mFlipPageListener.onCreatePage(mRecyclePageView, pageindex);
            setPageView(mTogglePage,newPage);
            mTogglePage.setX(-mPageWidth);
            mCurPage.setX(0);

            mMovePage = mTogglePage;
        }else if(mFlipTo == PageFlipTo.NEXT) {
            pageindex = mPageIndex + 1 >= mPageCount ? mPageCount - 1 : mPageIndex + 1;

            newPage = mFlipPageListener.onCreatePage(mRecyclePageView, pageindex);
            setPageView(mTogglePage,newPage);
            mTogglePage.setX(0);
            mCurPage.setX(0);

            mMovePage = mCurPage;
        }
        mMovePage.bringToFront();
    }

    /**
     * 移动页面
     */
    private void dragPage() {
        if(mPageDragged) {
            float deltaX = mCurPointX - mLastPointX;
            float translationX = mMovePage.getTranslationX();
            mMovePage.setTranslationX(calculateX(translationX + deltaX));
        }
    }

    /**
     * 判断页面是否滑动
     */
    private void checkScrollPage() {
        float transAbsX = Math.abs(mMovePage.getTranslationX());

        if(transAbsX != mPageWidth && !isPageScrolling()) {
            startScrollPage(transAbsX,false);
        }else {
            mRecyclePageView = mTogglePage.getPageView();
            mTogglePage.removePageView();
        }
    }

    /**
     * 开始活动页面
     * @param transAbsX 页面滑动的绝对值
     * @param force 强制滑动
     */
    private void startScrollPage(float transAbsX,boolean force) {
        float moveX,autoScrollX;
        float changePageDelta = mPageWidth / 3.0f;

        if(mScroller == null) {
            return;
        }

        mScrollStartX = mMovePage.getX();

        if(mFlipTo == PageFlipTo.PREVIOUS) {
            moveX = mPageWidth - transAbsX;
            if(moveX > changePageDelta || force) {
                autoScrollX = transAbsX;
            }else {
                autoScrollX = -moveX;
            }
        }else {
            moveX = Math.abs(transAbsX);
            if(moveX > changePageDelta || force) {
                autoScrollX = -(mPageWidth - transAbsX);
            }else {
                autoScrollX = moveX;
            }
        }

        mPageChange = force ? true : moveX > changePageDelta ? true : false;

        mScroller.startScroll(0,0,(int)autoScrollX,0,300);

        post(this);
    }

    /**
     * 停止页面滑动
     */
    private void stopScrollPage() {
        if(isPageScrolling()) {
            mScroller.abortAnimation();
            scrollStop();
        }
    }

    /**
     * scroll停止
     */
    private synchronized void scrollStop() {
        View page = getPageView(mTogglePage);
        mTogglePage.removePageView();

        if (mPageChange) {
            if (mFlipTo == PageFlipTo.PREVIOUS) {
                setPageIndex(mPageIndex - 1);
            } else {
                setPageIndex(mPageIndex + 1);
            }

            setPageView(mCurPage, page);

            if (mFlipPageListener != null) {
                mFlipPageListener.onPageChanged(mCurPage.getPageView(), mPageIndex);
            }
            mPageChange = false;
        }
        mCurPage.bringToFront();
        mCurPage.setX(0);
        mTogglePage.setX(0);
    }

    /**
     * 滑动页面
     */
    private void scrollPage() {

        if(mScroller == null) {
            return;
        }

        if(mScroller.computeScrollOffset()) {
            float scrollX = mScroller.getCurrX();
            mMovePage.setTranslationX(calculateX(mScrollStartX + scrollX));

            post(this);
        }

        if(!isPageScrolling()) {
            scrollStop();
        }

        invalidate();
    }

    /**
     * 判断页面是否正在自动滑动
     * @return
     */
    private boolean isPageScrolling() {
        return mScroller != null && !mScroller.isFinished() ? true : false;
    }

    /**
     * 切换页面
     * @param to > 0: 下一页; < 0: 上一页
     */
    public void flipPage(int to) {
        mFlipTo = to > 0 ? PageFlipTo.NEXT : to < 0 ? PageFlipTo.PREVIOUS : PageFlipTo.NONE;

        if(mFlipTo != PageFlipTo.NONE && !isPageScrolling()) {
            flipPageInit();
            startScrollPage(Math.abs(mMovePage.getTranslationX()),true);
        }
    }

    /**
     * 惯性滑动page
     * @param velocity
     * @return
     */
    private boolean checkFlingPage(float velocity) {
        boolean flingpage = false;

        if(Math.abs(velocity) > mMinimumVelocity) {
            startScrollPage(Math.abs(mMovePage.getTranslationX()),true);
            flingpage = true;
        }

        return flingpage;
    }

    /**
     * 计算 X
     * @param x
     * @return
     */
    private float calculateX(float x) {
        float cx = Math.abs(x) < mPageWidth ? x : -mPageWidth;
        return cx;
    }

    /**
     * 设置当前显示页
     * @param index
     */
    public void setCurPage(int index) {
        View newPage;

        if(mFlipPageListener != null) {
            setPageIndex(index);
            newPage = mFlipPageListener.onCreatePage(mRecyclePageView, index);
            setPageView(mCurPage, newPage);
            mCurPage.bringToFront();
        }
    }

    /**
     * 设置当前页码
     * @param index
     */
    private void setPageIndex(int index) {
        mPageIndex = index;
    }

    /**
     * 获取Page的子View
     * @param container
     * @return
     */
    private View getPageView(PageContainerView container) {
        View view = container.getPageView();
        return view;
    }

    /**
     * 设置Page的子View
     * @param container
     * @param view
     */
    private void setPageView(PageContainerView container, View view) {
        if(view != null) {
            if (container.getPageView() != null) {
                mRecyclePageView = container.getPageView();
                container.removePageView();
            }
            container.setPageView(view);
        }
    }

    /**
     * 设置页面数
     * @param count
     */
    public void setPageCount(int count) {
        mPageCount = count;
    }

    /**
     * 检查page index
     */
    private void checkPageIndexValid() {
        if(mPageIndex >= mPageCount) {
            throw new InvalidParameterException("Page Index Must Smaller Than Page Count");
        }else if(mPageIndex < 0){
            throw new InvalidParameterException("Page Index Must Not Negative Value");
        }
    }

    /**
     * 获取当前用户见的page view
     * @return
     */
    public View getCurPageView() {
        return mCurPage.getPageView();
    }

    /**
     * 设置页面创建监听
     * @param listener
     */
    public void setFlipPageListener(FlipPageListener listener) {
        mFlipPageListener = listener;
    }

    public interface FlipPageListener {
        public View onCreatePage(View oldpage, int index);
        public void onPageChanged(View newpage, int index);
    }

    /**
     * Page的container view，支持Scroll
     */
    public static class PageContainerView extends ScrollView {
        private FrameLayout mContainer;

        public PageContainerView(Context context) {
            super(context);
            setFillViewport(true);

            mContainer = new FrameLayout(getContext());
            mContainer.setClickable(true);
            addView(mContainer);

            setVerticalScrollBarEnabled(false);
        }

        public View getPageView() {
            return mContainer.getChildAt(0);
        }

        public void setPageView(View page) {
            scrollTo(0,0);
            mContainer.addView(page);
        }

        public void removePageView() {
            mContainer.removeAllViews();
        }

        public boolean checkScrollViewDraged() {
            boolean draged = false;
            try {
                Class scrollClass = ScrollView.class;
                Field draggingField = scrollClass.getDeclaredField("mIsBeingDragged");
                draggingField.setAccessible(true);
                draged = draggingField.getBoolean(this);
            }catch (Exception e) {
                e.printStackTrace();
            }
            return draged;
        }
    }
}
