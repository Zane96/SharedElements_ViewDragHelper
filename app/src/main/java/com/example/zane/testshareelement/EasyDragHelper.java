package com.example.zane.testshareelement;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

/**
 * Created by Zane on 16/6/4.
 * Email: zanebot96@gmail.com
 */

public class EasyDragHelper extends ViewGroup{
    /**
     * 被拖拉控件当前拖动的方向
     */
    public static final int NONE = 1 << 0;
    public static final int HORIZONTAL = 1 << 1;
    public static final int VERTICAL = 1 << 2;

    /**
     * 最终被拖拉控件滑向的方向
     */
    public static final int SLIDE_RESTORE_ORIGINAL = 1 << 0;
    public static final int SLIDE_TO_LEFT = 1 << 1;
    public static final int SLIDE_TO_RIGHT = 1 << 2;

    /**
     * 被拖拉控件最低透明度
     */
    private static final float MIN_ALPHA = 0.1f;

    /**
     * 被拖拉控件最终缩小的比例
     */
    private static final float PLAYER_RATIO = 0.5f;

    /**
     * 被拖拉控件的长宽比
     */
    private static final float VIDEO_RATIO = 16f / 9f;

    /**
     * 当被拖拉控件最小化后，其在水平方向的偏移量常量
     */
    private static final float ORIGINAL_MIN_OFFSET = 1f / (1f + PLAYER_RATIO);
    private static final float LEFT_DRAG_DISAPPEAR_OFFSET = (4f - PLAYER_RATIO) / (4f + 4f * PLAYER_RATIO);
    private static final float RIGHT_DRAG_DISAPPEAR_OFFSET = (4f + PLAYER_RATIO) / (4f + 4f * PLAYER_RATIO);

    /**
     * 核心类
     */
    private ViewDragHelper mDragHelper;

    /**
     * 本ViewGroup只能包含2个直接子组件
     */
    //被拖拉的控件
    private View mPlayer;
    //被拖拉控件下面的控件
    private View mDesc;

    /**
     * 第一次调用onMeasure时调用
     */
    private boolean mIsFinishInit = false;

    /**
     * 是否最小化
     */
    private boolean mIsMinimum = true;

    /**
     * 垂直方向的拖动范围
     */
    private int mVerticalRange;

    /**
     * 水平方向的拖动范围
     */
    private int mHorizontalRange;

    /**
     * 即this.getPaddingTop()
     */
    private int mMinTop;

    /**
     * 被拖拉控件的top
     */
    private int mTop;

    /**
     * 被拖拉控件的left
     */
    private int mLeft;

    /**
     * 被拖拉控件的最大宽度，避免重复计算
     */
    private int mPlayerMaxWidth;

    /**
     * 被拖拉控件最小宽度，避免重复计算
     */
    private int mPlayerMinWidth;

    /**
     * 当前拖动的方向
     */
    private int mDragDirect = NONE;

    /**
     * 垂直拖动时的偏移量
     * (mTop - mMinTop) / mVerticalRange
     */
    private float mVerticalOffset = 1f;

    /**
     * 水平拖动的偏移量
     * (mLeft + mPlayerMinWidth) / mHorizontalRange)
     */
    private float mHorizontalOffset = ORIGINAL_MIN_OFFSET;

    /**
     * 弱引用，绑定回调
     */
    private WeakReference<Callback> mCallback;

    /**
     * 触发ACTION_DOWN时的坐标
     */
    private int mDownX;
    private int mDownY;

    /**
     * 最终被拖拉控件消失的方向
     */
    private int mDisappearDirect = SLIDE_RESTORE_ORIGINAL;

    //------------------------------------初始化操作-------------------------------

    public EasyDragHelper(Context context) {
        this(context, null);
    }

    public EasyDragHelper(Context context, AttributeSet attrs){
        this(context, attrs, 0);
    }

    public EasyDragHelper(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        init();
    }

    private void init(){
        mDragHelper = ViewDragHelper.create(this, 1f, new MyHelperCallback());
        setBackgroundColor(Color.TRANSPARENT);
    }

    public void disappearPosition(){
        this.setAlpha(0f);
        mLeft = mHorizontalRange - mPlayerMinWidth;
        mTop = mVerticalRange;
        mIsMinimum = true;
        mVerticalOffset = 1f;
    }

    public void restorePosition(){
        this.setAlpha(1f);
        mLeft = mPlayerMinWidth;
        mTop = mMinTop;
        mIsMinimum = false;
        mVerticalOffset = 0f;
    }

    //-------------------------------------事件监听------------------------------

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        //事件拦截交给viewdraghelper去处理
        return mDragHelper.shouldInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //判断被拖拉的控件现在是不是在event发生的范围内
        boolean isHit = mDragHelper.isViewUnder(mPlayer,(int)event.getX(),(int)event.getY());

        if(isHit) {
            switch (MotionEventCompat.getActionMasked(event)){
                case MotionEvent.ACTION_DOWN: {
                    mDownX = (int) event.getX();
                    mDownY = (int) event.getY();
                }
                break;

                case MotionEvent.ACTION_MOVE:
                    if(mDragDirect == NONE){
                        int dx = Math.abs(mDownX - (int)event.getX());
                        int dy = Math.abs(mDownY - (int)event.getY());
                        //能够产生滑动效果的最小临界值
                        int slop = mDragHelper.getTouchSlop();

                        if(Math.sqrt(dx * dx + dy * dy) >= slop) {
                            if (dy >= dx)
                                mDragDirect = VERTICAL;
                            else
                                mDragDirect = HORIZONTAL;
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:{
                    if(mDragDirect == NONE) {
                        int dx = Math.abs(mDownX - (int) event.getX());
                        int dy = Math.abs(mDownY - (int) event.getY());
                        int slop = mDragHelper.getTouchSlop();
                        //手指离开的时候,根据手指的位置来改变mIsMinimum,然后判断是需要动画变换到最大化还是最小化
                        if (Math.sqrt(dx * dx + dy * dy) < slop){
                            mDragDirect = VERTICAL;
                            if(mIsMinimum)
                                maximize();
                            else
                                minimize();
                        }
                    }
                }
                break;

                default:
                    break;
            }
        }

        mDragHelper.processTouchEvent(event);
        return isHit;
    }

    //-------------------------------------用户手指离开之后的动画效果------------------------------

    private void maximize() {
        mIsMinimum = false;
        slideVerticalTo(0f);
    }

    private void minimize() {
        mIsMinimum = true;
        slideVerticalTo(1f);
    }

    //根据现在是要自动移到最上方还是最下方,进行动画操作(惯性操作)
    private boolean  slideVerticalTo(float slideOffset) {
        int topBound = mMinTop;
        int y = (int) (topBound + slideOffset * mVerticalRange);

        if (mDragHelper.smoothSlideViewTo(mPlayer, mIsMinimum ?
                                                           (int)(mPlayerMaxWidth * (1 - PLAYER_RATIO)) : getPaddingLeft(), y)) {
            ViewCompat.postInvalidateOnAnimation(this);
            return true;
        }
        return false;
    }

    private void slideToLeft(){
        slideHorizontalTo(0f);
        mDisappearDirect = SLIDE_TO_LEFT;
    }

    private void slideToRight(){
        slideHorizontalTo(1f);
        mDisappearDirect = SLIDE_TO_RIGHT;
    }

    private void slideToOriginalPosition(){
        slideHorizontalTo(ORIGINAL_MIN_OFFSET);
        mDisappearDirect = SLIDE_RESTORE_ORIGINAL;
    }

    //根据用户现在所需要的控件消失的方向来分别操作从左消失或者从右消失的动画操作(惯性操作)
    private boolean slideHorizontalTo(float slideOffset){
        int leftBound = -mPlayer.getWidth();
        int x = (int)(leftBound + slideOffset * mHorizontalRange);

        if(mDragHelper.smoothSlideViewTo(mPlayer, x, mTop)){
            ViewCompat.postInvalidateOnAnimation(this);
            return true;
        }
        return false;
    }

    //-------------------------------------viewdraghelper的回调监听------------------------------

    private class MyHelperCallback extends ViewDragHelper.Callback{
        //如果view是不是我们希望被拖拉的控件
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == mPlayer;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if(state == ViewDragHelper.STATE_IDLE){
                //如果现在是最小化,并且是横向移动,并且是达到了要消失的要求,那么调用回调函数并且初始化控件
                if(mIsMinimum && mDragDirect == HORIZONTAL && mDisappearDirect != SLIDE_RESTORE_ORIGINAL){
                    if(mCallback != null && mCallback.get() != null)
                        mCallback.get().onDisappear(mDisappearDirect);

                    mDisappearDirect = SLIDE_RESTORE_ORIGINAL;
                    //初始化为消失的position
                    disappearPosition();
                    //初始化size和layout
                    requestLayoutLightly();
                }
                //方向初始化
                mDragDirect = NONE;
            }
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            int range = 0;

            if(child == mPlayer && mDragDirect == VERTICAL){
                range = mVerticalRange;
            }
            return range;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            int range = 0;

            if(child == mPlayer && mIsMinimum && mDragDirect == HORIZONTAL){
                range = mHorizontalRange;
            }
            return range;
        }

        //返回纵向移动坐标,如果到了边界就不应该移动
        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            int newTop = mTop;
            if(child == mPlayer && mDragDirect == VERTICAL) {
                int topBound = mMinTop;
                int bottomBound = topBound + mVerticalRange;
                //边界控制
                newTop = Math.min(Math.max(top, topBound), bottomBound);
            }
            return newTop;
        }

        //返回横向移动坐标
        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            int newLeft = mLeft;
            if(child == mPlayer && mIsMinimum && mDragDirect == HORIZONTAL){
                int leftBound = -mPlayer.getWidth();
                int rightBound = leftBound + mHorizontalRange;
                newLeft = Math.min(Math.max(left,leftBound),rightBound);
            }
            return newLeft;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            //根据坐标来计算mVerticalOffset,再通过mVerticalOffset去重新测量被拖拉控件的size和layout
            //并且改变透明度
            if(mDragDirect == VERTICAL) {
                mTop = top;
                mVerticalOffset = (float) (mTop - mMinTop) / mVerticalRange;
                mDesc.setAlpha(1 - mVerticalOffset);
            }else if(mIsMinimum && mDragDirect == HORIZONTAL){
                mLeft = left;
                mHorizontalOffset = Math.abs((float)(mLeft + mPlayerMinWidth) / mHorizontalRange);
            }
            //重新测量
            requestLayoutLightly();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            //根据偏向是否达到了range的0.5而进行不同惯性操作
            if(mDragDirect == VERTICAL)
            {
                if(yvel > 0 || (yvel == 0 && mVerticalOffset >= 0.5f))
                    minimize();
                else if(yvel < 0 || (yvel == 0 && mVerticalOffset < 0.5f))
                    maximize();
            }else if (mIsMinimum && mDragDirect == HORIZONTAL){
                if((mHorizontalOffset < LEFT_DRAG_DISAPPEAR_OFFSET && xvel < 0))
                    slideToLeft();
                else if((mHorizontalOffset > RIGHT_DRAG_DISAPPEAR_OFFSET && xvel > 0))
                    slideToRight();
                else
                    slideToOriginalPosition();
            }
        }
    }

    //viewdraghelper是利用了Scroller的,这个是必须写的函数
    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    //不懂去看注释
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if(getChildCount() != 2)
            throw new RuntimeException("this ViewGroup must only contains 2 views");
        //拿到我的两个子view
        mPlayer = getChildAt(0);
        mDesc = getChildAt(1);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //每次都主动开始测量子view
        customMeasure(widthMeasureSpec, heightMeasureSpec);

        int maxWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = View.MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
                resolveSizeAndState(maxHeight, heightMeasureSpec, 0));

        //测量中的初始化操作
        if(!mIsFinishInit){
            mMinTop = getPaddingTop();
            mPlayerMinWidth = mPlayer.getMeasuredWidth();
//            mPlayerMaxWidth = (int)(mPlayerMinWidth / PLAYER_RATIO);
            mHorizontalRange = mPlayerMaxWidth + mPlayerMinWidth;
            mVerticalRange = getMeasuredHeight() - getPaddingTop() - getPaddingBottom()
                                     - mPlayer.getMeasuredHeight();

            restorePosition();
            mIsFinishInit = true;
        }
    }

    private void customMeasure(int widthMeasureSpec, int heightMeasureSpec){
        measurePlayer(widthMeasureSpec, heightMeasureSpec);
        measureDesc(widthMeasureSpec, heightMeasureSpec);
    }

    private void measurePlayer(int widthMeasureSpec, int heightMeasureSpec){
        final ViewGroup.LayoutParams lp = mPlayer.getLayoutParams();

        if(!mIsFinishInit) {
            int measureWidth = getChildMeasureSpec(widthMeasureSpec,
                    getPaddingLeft() + getPaddingRight(), lp.width);

            mPlayerMaxWidth = View.MeasureSpec.getSize(measureWidth);
        }

        justMeasurePlayer();
    }

    private void measureDesc(int widthMeasureSpec, int heightMeasureSpec){
        measureChild(mDesc, widthMeasureSpec, heightMeasureSpec);
    }

    //测量被拖拉控件,调用它的onMeasure()
    private void justMeasurePlayer(){
        int widthCurSize =(int)(mPlayerMaxWidth * (1f - mVerticalOffset * (1f - PLAYER_RATIO)));
        int childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(widthCurSize, View.MeasureSpec.EXACTLY);

        int heightSize =(int)(View.MeasureSpec.getSize(childWidthMeasureSpec) / VIDEO_RATIO);
        int childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(heightSize, View.MeasureSpec.EXACTLY);

        mPlayer.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        onLayoutLightly();
    }

    private void onLayoutLightly(){
        if(mDragDirect != HORIZONTAL) {
            mLeft = this.getWidth() - this.getPaddingRight() - this.getPaddingLeft()
                            - mPlayer.getMeasuredWidth();

            mDesc.layout(mLeft, mTop + mPlayer.getMeasuredHeight(),
                    mLeft + mDesc.getMeasuredWidth(), mTop + mDesc.getMeasuredHeight());
        }

        mPlayer.layout(mLeft, mTop, mLeft + mPlayer.getMeasuredWidth(), mTop + mPlayer.getMeasuredHeight());
    }

    private void requestLayoutLightly(){
        justMeasurePlayer();
        onLayoutLightly();
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public void setCallback(Callback callback){
        mCallback = new WeakReference<>(callback);
    }

    public interface Callback{
        void onDisappear(int direct);
    }
}
