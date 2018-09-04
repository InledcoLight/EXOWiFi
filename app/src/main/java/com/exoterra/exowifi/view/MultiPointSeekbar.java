package com.exoterra.exowifi.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.util.Pools;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.exoterra.comman.SizeUtil;
import com.exoterra.exowifi.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liruya on 2018/4/27.
 */

public class MultiPointSeekbar extends View
{
    private final int POINT_COUNT_DEFAULT = 1;
    private final int POINT_COUNT_MIN = 1;
    private final int POINT_COUNT_MAX = 10;
    private final int HINT_MODE_HIDE = 0;
    private final int HINT_MODE_MOVING = 1;
    private final int HINT_MODE_SELECTED = 2;

    private final int SELECTED_POINT_NONE = -1;

    private float mDpUnitSize;
    private float mSpUnitSize;

    private boolean mAttached;

    private int mMaxWidth;
    private int mMinWidth;
    private int mMaxHeight;
    private int mMinHeight;

    private int mLineWidth;

    private int mPointCount;
    private int mLineColor;
    private int mThumbColor;
    private int mSelectedThumbColor;
    private Drawable mThumb;
    private Drawable mSelectedThumb;
    private Drawable[] mThumbDrawables;
    private int[] mProgress;
    private String mMaxLengthHint;
    private int mHintBackgroundColor;
    private int mHintPadding;
    private int mHintColor;
    private int mHintSize;
    private int mHintMode;
//    private ColorStateList mThumbTintList;
//    private PorterDuff.Mode mThumbMode;
//    private boolean mHasThumbTint;
//    private boolean mHasThumbTintMode;
    private int mMax;
    private int mMin;

    private float mTouchDownX;

    private int mSelectedPoint = SELECTED_POINT_NONE;
    private boolean mDragEnabled;
    private boolean mDragging;

    boolean mIsUserSeekable = true;

    private final int NO_ALPHA = 0xFF;
    private float mDisabledAlpha;

    private int mScaledTouchSlop;

    private int mHintWidth;
    private int mHintHeight;
    private int mTextHeight;

    private Paint mLinePaint;
    private Paint mThumbPaint;
    private Paint mHintPaint;

    private List<Integer> mTouchPoints;

    private List<RefreshData> mRefreshDataList = new ArrayList<>();
    private boolean mRefreshIsPosted;
    private RefreshDataRunnable mRefreshDataRunnable;
    private long mUiThreadId;

    private GetTextImpl mGetTextImpl;
    private Listener mListener;

    public MultiPointSeekbar( Context context )
    {
        this( context, null );
    }

    public MultiPointSeekbar( Context context, AttributeSet attrs )
    {
        this( context, attrs, 0 );
    }

    public MultiPointSeekbar( Context context, AttributeSet attrs, int defStyleAttr )
    {
        super( context, attrs, defStyleAttr );
        mDpUnitSize = SizeUtil.dp2px( context, 1 );
        mSpUnitSize = SizeUtil.sp2px( context, 1 );
        mUiThreadId = Thread.currentThread().getId();
        final TypedArray a = context.obtainStyledAttributes( attrs,
                                                             R.styleable.MultiPointSeekbar );
        mPointCount = a.getInt( R.styleable.MultiPointSeekbar_pointCount,
                                POINT_COUNT_DEFAULT );
        mThumbDrawables = new Drawable[POINT_COUNT_MAX];
        mProgress = new int[POINT_COUNT_MAX];
        mThumbColor = a.getColor( R.styleable.MultiPointSeekbar_thumbColor, 0xFFFFFFFF );
        mSelectedThumbColor = a.getColor( R.styleable.MultiPointSeekbar_selectedThumbColor, getResources().getColor( R.color.colorAccent ) );
        mMinWidth = (int) a.getDimension( R.styleable.MultiPointSeekbar_minWidth, mDpUnitSize * 64 );
        mMinHeight = (int) a.getDimension( R.styleable.MultiPointSeekbar_minHeight, mDpUnitSize * 4 );
        mMaxWidth = (int) a.getDimension( R.styleable.MultiPointSeekbar_maxWidth, mDpUnitSize * 1024 );
        mMaxHeight = (int) a.getDimension( R.styleable.MultiPointSeekbar_maxHeight, mDpUnitSize * 32 );
        mLineColor = a.getColor( R.styleable.MultiPointSeekbar_lineColor, 0xFFFFFFFF );
        mMax = a.getInt( R.styleable.MultiPointSeekbar_maximum, 100 );
        mMin = a.getInt( R.styleable.MultiPointSeekbar_minimum, 0 );
        mThumb = a.getDrawable( R.styleable.MultiPointSeekbar_thumbDrawable );
        mSelectedThumb = a.getDrawable( R.styleable.MultiPointSeekbar_selectedThumDrawable );
        mHintBackgroundColor = a.getColor( R.styleable.MultiPointSeekbar_hintBackgroundColor, getResources().getColor( R.color.colorAccent ) );
        mMaxLengthHint = a.getString( R.styleable.MultiPointSeekbar_maxLengthHint );
        mHintColor = a.getColor( R.styleable.MultiPointSeekbar_hintColor, 0xFFFFFFFF );
        mHintSize = (int) a.getDimension( R.styleable.MultiPointSeekbar_hintSize, 12 * mSpUnitSize );
        mHintPadding = (int) a.getDimension( R.styleable.MultiPointSeekbar_hintPadding, 4 * mDpUnitSize );
        mHintMode = a.getInt( R.styleable.MultiPointSeekbar_hintMode, HINT_MODE_SELECTED );
//        if ( a.hasValue( R.styleable.MultiPointSeekbar_thumbTint ) )
//        {
//            ColorStateList tint = a.getColorStateList( R.styleable.MultiPointSeekbar_thumbTint );
//            for ( int i = 0; i < mPointCount; i++ )
//            {
//                mThumbTintList[i] = tint;
//                mHasThumbTint[i] = true;
//            }
//        }
//        if ( a.hasValue( R.styleable.MultiPointSeekbar_thumbTintMode ) )
//        {
//            int m = a.getInt( R.styleable.MultiPointSeekbar_thumbTintMode, -1 );
//            for ( int i = 0; i < mPointCount; i++ )
//            {
//                PorterDuff.Mode mode = DrawableUtils.parseTintMode( m, mThumbMode[i] );
//            }
//        }
        for ( int i = 0; i < mPointCount; i++ )
        {
            mThumbDrawables[i] = mThumb;
            mProgress[i] = 0;
        }
        mTouchPoints = new ArrayList<>();
        a.recycle();
        mScaledTouchSlop = ViewConfiguration.get( context ).getScaledTouchSlop();
        init();
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        if (mRefreshDataList != null) {
            synchronized (this) {
                final int count = mRefreshDataList.size();
                for (int i = 0; i < count; i++) {
                    final RefreshData rd = mRefreshDataList.get(i);
                    doRefreshProgress(rd.index, rd.progress, rd.fromUser );
                    rd.recycle();
                }
                mRefreshDataList.clear();
            }
        }
        mAttached = true;
    }

    @Override
    protected void onDetachedFromWindow()
    {
        if (mRefreshDataRunnable != null) {
            removeCallbacks(mRefreshDataRunnable);
            mRefreshIsPosted = false;
        }
        super.onDetachedFromWindow();
        mAttached = false;
    }

    @Override
    protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec )
    {
        super.onMeasure( widthMeasureSpec, heightMeasureSpec );
        int widthMode = MeasureSpec.getMode( widthMeasureSpec );
        int widthSize = MeasureSpec.getSize( widthMeasureSpec );
        int heightMode = MeasureSpec.getMode( heightMeasureSpec );
        int heightSize = MeasureSpec.getSize( heightMeasureSpec );
        int width = Math.max( mMinWidth, Math.min( mMaxWidth, widthSize ) );
        int height = Math.max( mMinHeight, Math.min( mMaxHeight, heightSize ) );
        height = Math.min( height, mHintWidth + mHintPadding + getThumbHeight() );
        width += getPaddingLeft() + getPaddingRight();
        height += getPaddingTop() + getPaddingBottom();
        if ( widthMode == MeasureSpec.EXACTLY )
        {
            width = widthSize;
        }
        else if ( widthMode == MeasureSpec.AT_MOST )
        {

        }
        if ( heightMode == MeasureSpec.EXACTLY )
        {
            height = heightSize;
        }
        else if ( heightMode == MeasureSpec.AT_MOST )
        {
            height = mHintHeight + mHintPadding + getThumbHeight() + getPaddingTop() + getPaddingBottom();
        }
        setMeasuredDimension( width, height );
    }

    @Override
    protected void onDraw( Canvas canvas )
    {
        super.onDraw( canvas );

        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        drawLine( canvas, w, h );
        for ( int i = 0; i < mPointCount; i++ )
        {
            if ( mSelectedPoint == i )
            {
                continue;
            }
            drawThumb( canvas, w, h, i );
        }
        drawThumb( canvas, w, h, mSelectedPoint );
        drawHint( canvas, w, h, mSelectedPoint );
    }

    @Override
    public boolean onTouchEvent( MotionEvent event )
    {
        float x = event.getX();
        float y = event.getY();
        switch ( event.getAction() )
        {
            case MotionEvent.ACTION_DOWN:
                if ( mSelectedPoint == SELECTED_POINT_NONE )
                {
                    mDragEnabled = false;
                    mDragging = false;
                    mTouchPoints.clear();
                    for ( int i = 0; i < mPointCount; i++ )
                    {
                        if ( isPointTouched( i, x, y ) )
                        {
                            mTouchPoints.add( i );
                        }
                    }
                    if ( mTouchPoints.size() == 1 )
                    {
                        mSelectedPoint = mTouchPoints.get( 0 );
                        mTouchDownX = x;
                        if ( mListener != null )
                        {
                            mListener.onPointSelected( mSelectedPoint );
                        }
                        invalidate();
                        return true;
                    }
                    else if ( mTouchPoints.size() > 1 && mTouchPoints.size() < mPointCount )
                    {
                        if ( mListener != null )
                        {
                            mListener.onMultiPointTouched( mTouchPoints );
                        }
                    }
                }
                else if ( mSelectedPoint >= 0 && mSelectedPoint < mPointCount )
                {
                    if ( isPointTouched( mSelectedPoint, x, y ) )
                    {
                        mTouchDownX = x;
                        mDragEnabled = true;
                        mDragging = false;
                        return true;
                    }
                    else
                    {
                        mDragEnabled = false;
                        mDragging = false;
                    }
                }
                else
                {
                    mDragEnabled = false;
                    mDragging = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if ( mSelectedPoint >= 0 && mSelectedPoint < mPointCount && mDragEnabled )
                {
                    if ( mDragging )
                    {
                        pointTouchEvent( event, mSelectedPoint );
                    }
                    else
                    {
                        if ( Math.abs( mTouchDownX - event.getX() ) >= mScaledTouchSlop )
                        {
                            mDragging = true;
                            if ( mListener != null )
                            {
                                mListener.onStartPointTouch( mSelectedPoint );
                            }
                        }
                    }
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if ( mDragEnabled && mDragging )
                {
                    if ( mListener != null )
                    {
                        mListener.onStopPointTouch( mSelectedPoint );
                    }
                }
                mDragEnabled = false;
                mDragging = false;
                break;
            case MotionEvent.ACTION_UP:
                if ( mDragEnabled )
                {
                    if ( mDragging )
                    {
                        pointTouchEvent( event, mSelectedPoint );
                        if ( mListener != null && mSelectedPoint >= 0 && mSelectedPoint < mPointCount )
                        {
                            mListener.onStopPointTouch( mSelectedPoint );
                        }
                    }
                    else
                    {
                        if ( mListener != null && mSelectedPoint >= 0 && mSelectedPoint < mPointCount )
                        {
                            mListener.onStartPointTouch( mSelectedPoint );
                            pointTouchEvent( event, mSelectedPoint );
                            mListener.onStopPointTouch( mSelectedPoint );
                        }
                    }
                }
                mDragEnabled = false;
                mDragging = false;
                break;
        }
        return super.onTouchEvent( event );
    }

    private void pointTouchEvent( MotionEvent event, int idx )
    {
        if ( idx < 0 && idx >= mPointCount )
        {
            return;
        }
        final int x = Math.round( event.getX() );
        final int y = Math.round( event.getY() );
        final int thumbWidth = getThumbWidth();
        final int extra = thumbWidth < mHintWidth ? mHintWidth : thumbWidth;
        final int lineWidth = getWidth() - getPaddingLeft() - getPaddingRight() - extra;
        final int left = getPaddingLeft() + extra/2;
        final int right = getWidth() - getPaddingRight() - extra/2;
        float scale;
        if ( x < left )
        {
            scale = 0.0f;
        }
        else if ( x > right )
        {
            scale = 1.0f;
        }
        else
        {
            scale = ( x - left )/(float) lineWidth;
        }
        final int progress = Math.round( mMin + scale * ( mMax - mMin ) );
        if ( progress == mProgress[idx] )
        {
            return;
        }
        mProgress[idx] = progress;
        if ( mUiThreadId == Thread.currentThread().getId() )
        {
            doRefreshProgress( idx, progress, true );
        }
        else
        {
            if ( mRefreshDataRunnable == null )
            {
                mRefreshDataRunnable = new RefreshDataRunnable();
            }
            final RefreshData rd = RefreshData.obtain( idx, progress, true );
            mRefreshDataList.add( rd );
            if ( mAttached && !mRefreshIsPosted )
            {
                post( mRefreshDataRunnable );
                mRefreshIsPosted = true;
            }
        }
    }

    private void doRefreshProgress( int idx, int progress, boolean fromUser )
    {
        if ( mListener != null )
        {
            mListener.onPointProgressChanged( idx, progress, fromUser );
        }
        invalidate();
    }

    private static class RefreshData
    {
        private static int POOL_MAX = 8;
        private static final Pools.SynchronizedPool<RefreshData> sPool = new Pools.SynchronizedPool<>( POOL_MAX );

        private int index;
        private int progress;
        private boolean fromUser;

        public static RefreshData obtain( int index, int progress, boolean fromUser )
        {
            RefreshData rd = sPool.acquire();
            if ( rd == null )
            {
                rd = new RefreshData();
            }

            rd.index = index;
            rd.progress = progress;
            rd.fromUser = fromUser;
            return rd;
        }

        public void recycle()
        {
            sPool.release( this );
        }
    }

    private class RefreshDataRunnable implements Runnable
    {
        @Override
        public void run()
        {
            synchronized ( MultiPointSeekbar.this )
            {
                final int count = mRefreshDataList.size();
                for ( int i = 0; i < count; i++ )
                {
                    final RefreshData rd = mRefreshDataList.get( i );
                    doRefreshProgress( rd.index, rd.progress, rd.fromUser );
                    rd.recycle();
                }
                mRefreshDataList.clear();
                mRefreshIsPosted = false;
            }
        }
    }

    private boolean isPointTouched( int idx, float x, float y )
    {
        if ( idx < 0 || idx >= mPointCount )
        {
            return false;
        }
        int width = getThumbWidth();
        int height = getThumbHeight();
        int extra = width < mHintWidth ? mHintWidth : width;
        int lineWidth = getWidth() - getPaddingLeft() - getPaddingRight() - extra;
        if ( width < 100 )
        {
            width = 100;
        }
        if ( height < 100 )
        {
            height = 100;
        }
        int left = (int) ( getPaddingLeft() + ( extra - width )/ 2 + lineWidth * (mProgress[idx] - mMin) / (float) ( mMax - mMin) );
        int right = left + width;
        int top = getPaddingTop() + mHintHeight + mHintPadding;
        int bottom = top + height;
        if ( x >= left && x <= right && y >= top && y <= bottom )
        {
            return true;
        }
        return false;
    }

    public void setGetTextImpl( GetTextImpl impl )
    {
        mGetTextImpl = impl;
    }

    public void setListener( Listener listener )
    {
        mListener = listener;
    }

    private void init()
    {
        mLinePaint = new Paint();
        mThumbPaint = new Paint();
        mHintPaint = new Paint();
        mLinePaint.setColor( mLineColor );
        mLinePaint.setStyle( Paint.Style.FILL );
        mHintPaint.setTextAlign( Paint.Align.CENTER );
        mHintPaint.setStyle( Paint.Style.FILL );

        refreshHintSize();
    }

    private void refreshHintSize()
    {
        mHintPaint.setTextSize( mHintSize );
        if ( TextUtils.isEmpty( mMaxLengthHint ) )
        {
            mMaxLengthHint = "100%";
        }
        Rect rect = new Rect();
        mHintPaint.getTextBounds( mMaxLengthHint, 0, mMaxLengthHint.length(), rect );
        mHintWidth = (int) ( mDpUnitSize * 4 + rect.width());
        mHintHeight = (int) ( mHintWidth * 1.2f );
        mTextHeight = rect.height();
    }

    private int getThumbHeight()
    {
        if ( mThumb != null )
        {
            return mThumb.getIntrinsicHeight();
        }
        return (int) ( mDpUnitSize * 16 );
    }

    private int getThumbWidth()
    {
        if ( mThumb != null )
        {
            return mThumb.getIntrinsicWidth();
        }
        return (int) ( mDpUnitSize * 16 );
    }

    private void drawTimer( int cx, int cy, int r, Canvas canvas )
    {
        mThumbPaint.setColor( 0xFFFFFFFF );
        canvas.drawCircle( cx, cy, r, mThumbPaint );
        Path path = new Path();
        path.moveTo( cx, cy - r/2 );
        path.lineTo( cx, cy );
        path.lineTo( cx + r/2, cy );
        mThumbPaint.setColor( 0xFF000000 );
        canvas.drawPath( path, mThumbPaint );
    }

    private void drawLine( Canvas canvas, int w, int h )
    {
        int thumbWidth = getThumbWidth();
        int extra = thumbWidth < mHintWidth ? mHintWidth : thumbWidth;
        int lineHeight = (int) ( mDpUnitSize * 4);
        int left = getPaddingLeft() + extra/2;
        int top = getPaddingTop() + mHintHeight + mHintPadding + ( h - getPaddingTop() - getPaddingBottom() - mHintHeight - mHintPadding - lineHeight ) / 2;
        int right = w - getPaddingRight() - extra/2;
        int bottom = top + lineHeight;
        canvas.drawRoundRect( left, top, right, bottom, lineHeight/2, lineHeight/2, mLinePaint );
    }

    private void drawThumb( Canvas canvas, int w, int h, int idx )
    {
        if ( idx < 0 || idx >= mPointCount )
        {
            return;
        }
        if ( mProgress[idx] < mMin )
        {
            mProgress[idx] = mMin;
        }
        if ( mProgress[idx] > mMax )
        {
            mProgress[idx] = mMax;
        }
        Drawable thumb = mSelectedPoint == idx ? mSelectedThumb : mThumb;
        int thumbHeight = getThumbHeight();
        int thumbWidth = getThumbWidth();
        int extra = thumbWidth < mHintWidth ? mHintWidth : thumbWidth;
        int lineWidth = w - getPaddingLeft() - getPaddingRight() - extra;
        int left = (int) ( getPaddingLeft() + lineWidth * mProgress[idx] / (float)( mMax - mMin) + ( extra - thumbWidth) / 2);
        int top = getPaddingTop() + mHintHeight + mHintPadding + ( h - getPaddingTop() - getPaddingBottom() - mHintHeight - mHintPadding - thumbHeight )/2;
        int right = left + thumbWidth;
        int bottom = top + thumbHeight;
        if ( thumb != null )
        {
            mThumbDrawables[idx].setBounds( left, top, right, bottom );
//            mThumbDrawables[idx].draw( canvas );
            final int saveCount = canvas.save();
//            canvas.translate( getPaddingLeft() + lineWidth*mProgress[idx]/(float)(mMax-mMin)  + (extra - thumbWidth)/2, top );
            mThumbDrawables[idx].draw( canvas );
            canvas.restoreToCount( saveCount );
        }
        else
        {
            float cx = getPaddingLeft() + extra/2 + lineWidth * mProgress[idx] / (float) ( mMax - mMin );
            float cy = getPaddingTop() + mHintHeight + mHintPadding + ( h - getPaddingTop() - getPaddingBottom() - mHintHeight - mHintPadding )/2;
            mThumbPaint.setColor( mSelectedPoint == idx ? mSelectedThumbColor : mThumbColor );
            mThumbPaint.setStyle( Paint.Style.FILL );
            canvas.drawCircle( cx, cy, mDpUnitSize * 8, mThumbPaint );
            mThumbPaint.setColor( 0xFF000000 );
            mThumbPaint.setStrokeWidth( 4 );
            canvas.drawLine( cx-2, cy, cx + mDpUnitSize*6, cy, mThumbPaint );
            canvas.drawLine( cx, cy+2, cx, cy - mDpUnitSize*6, mThumbPaint );
        }
    }

    private String getHintText( int progress )
    {
        if ( mGetTextImpl != null )
        {
            return mGetTextImpl.getText( progress );
        }
        return "" + (progress - mMin)*100/(mMax-mMin) + "%";
    }

    private void drawHint( Canvas canvas, int w, int h, int idx )
    {
        if ( idx < 0 || idx >= mPointCount || mSelectedPoint != idx )
        {
            return;
        }
        if ( mHintMode == HINT_MODE_HIDE )
        {
            return;
        }
        if ( mHintMode == HINT_MODE_SELECTED || mDragging )
        {
            if ( mProgress[idx] < mMin )
            {
                mProgress[idx] = mMin;
            }
            if ( mProgress[idx] > mMax )
            {
                mProgress[idx] = mMax;
            }
            int thumbWidth = getThumbWidth();
            int extra = thumbWidth < mHintWidth ? mHintWidth : thumbWidth;
            int lineWidth = w - getPaddingLeft() - getPaddingRight() - extra;
            int left = (int) ( getPaddingLeft() + extra / 2 + lineWidth * mProgress[idx] / (float) ( mMax - mMin ) - mHintWidth / 2 );
            int top = getPaddingTop();
            int right = left + mHintWidth;
            int bottom = top + mHintHeight;
            Path path = new Path();
            RectF rectF = new RectF( left, top, right, top + mHintWidth );
            path.arcTo( rectF, 150, 240 );
            path.lineTo( ( left + right ) / 2, bottom );
            path.close();
            mHintPaint.setColor( mHintBackgroundColor );
            canvas.drawPath( path, mHintPaint );

            String text = getHintText( mProgress[idx] );
            if ( text.length() > mMaxLengthHint.length() )
            {
                text = text.substring( 0, mMaxLengthHint.length() );
            }
            mHintPaint.setColor( mHintColor );
            canvas.drawText( text, ( left + right ) / 2, ( top + bottom ) / 2 + mTextHeight / 4, mHintPaint );
        }
    }

    public void setThumb( Drawable thumb )
    {
        final boolean needUpdate;
        if ( mThumb != null && thumb != mThumb )
        {
            mThumb.setCallback( null );
            needUpdate = true;
        }
        else
        {
            needUpdate = false;
        }

        if ( thumb != null )
        {
            thumb.setCallback( this );

            if ( needUpdate && thumb.getIntrinsicWidth() != mThumb.getIntrinsicWidth()
                 && thumb.getIntrinsicHeight() != mThumb.getIntrinsicHeight() )
            {
                requestLayout();
            }
        }

        mThumb = thumb;
        invalidate();

        if ( needUpdate )
        {

            if ( thumb != null && thumb.isStateful() )
            {
                int[] state = getDrawableState();
                thumb.setState( state );
            }
        }
    }

    public Drawable getThumb()
    {
        return mThumb;
    }

    public synchronized int getPointCount()
    {
        return mPointCount;
    }

    public synchronized void setPointCount( int pointCount )
    {
        if ( pointCount <= 0 || pointCount > POINT_COUNT_MAX || pointCount == mPointCount )
        {
            return;
        }
        for ( int i = mPointCount; i < pointCount; i++ )
        {
            mProgress[i] = mProgress[mPointCount-1];
        }
        mPointCount = pointCount;
        invalidate();
    }

    public synchronized int getLineColor()
    {
        return mLineColor;
    }

    public synchronized void setLineColor( int lineColor )
    {
        mLineColor = lineColor;
        invalidate();
    }

    public synchronized int getThumbColor()
    {
        return mThumbColor;
    }

    public synchronized void setThumbColor( int thumbColor )
    {
        mThumbColor = thumbColor;
        invalidate();
    }

    public synchronized int getSelectedThumbColor()
    {
        return mSelectedThumbColor;
    }

    public synchronized void setSelectedThumbColor( int selectedThumbColor )
    {
        mSelectedThumbColor = selectedThumbColor;
        invalidate();
    }

    public int[] getProgress()
    {
        return mProgress;
    }

    public int getProgressByIndex( int idx )
    {
        if ( idx < 0 || idx >= mPointCount )
        {
            return mMin;
        }
        return mProgress[idx];
    }

    public synchronized void setProgress( int[] progress )
    {
        if ( progress == null || progress.length == 0 || progress.length > POINT_COUNT_MAX )
        {
            return;
        }
        if ( mPointCount != progress.length )
        {
            mPointCount = progress.length;
            if ( mListener != null )
            {
                mListener.onPointCountChanged( mPointCount );
            }
        }
        for ( int i = 0; i < mPointCount; i++ )
        {
            if ( progress[i] < mMin )
            {
                progress[i] = mMin;
            }
            if ( progress[i] > mMax )
            {
                progress[i] = mMax;
            }
            if ( mProgress[i] != progress[i] )
            {
                mProgress[i] = progress[i];
                if ( mListener != null )
                {
                    mListener.onPointProgressChanged( i, mProgress[i], false );
                }
            }
        }
        invalidate();
    }

    public synchronized void setProgress( int idx, int progress )
    {
        if ( idx < 0 || idx >= mPointCount )
        {
            return;
        }
        if ( progress < mMin )
        {
            progress = mMin;
        }
        if ( progress > mMax )
        {
            progress = mMax;
        }
        if ( progress == mProgress[idx] )
        {
            return;
        }
        mProgress[idx] = progress;
        if ( mListener != null )
        {
            mListener.onPointProgressChanged( idx, progress, false );
        }
        invalidate();
    }

    public String getMaxLengthHint()
    {
        return mMaxLengthHint;
    }

    public void setMaxLengthHint( String maxLengthHint )
    {
        mMaxLengthHint = maxLengthHint;
        refreshHintSize();
        invalidate();
    }

    public int getHintBackgroundColor()
    {
        return mHintBackgroundColor;
    }

    public void setHintBackgroundColor( int hintBackgroundColor )
    {
        mHintBackgroundColor = hintBackgroundColor;
        invalidate();
    }

    public int getHintPadding()
    {
        return mHintPadding;
    }

    public void setHintPadding( int hintPadding )
    {
        mHintPadding = hintPadding;
        invalidate();
    }

    public int getHintColor()
    {
        return mHintColor;
    }

    public void setHintColor( int hintColor )
    {
        mHintColor = hintColor;
        invalidate();
    }

    public int getHintSize()
    {
        return mHintSize;
    }

    public void setHintSize( int hintSize )
    {
        mHintSize = hintSize;
        invalidate();
    }

    public int getHintMode()
    {
        return mHintMode;
    }

    public void setHintMode( int hintMode )
    {
        mHintMode = hintMode;
        invalidate();
    }

    public int getMax()
    {
        return mMax;
    }

    public void setMax( int max )
    {
        if ( max <= mMin )
        {
            return;
        }
        mMax = max;
        invalidate();
    }

    public int getMin()
    {
        return mMin;
    }

    public void setMin( int min )
    {
        if ( min >= mMax )
        {
            return;
        }
        mMin = min;
        invalidate();
    }

    public int getSelectedPoint()
    {
        return mSelectedPoint;
    }

    public void setSelectedPoint( int selectedPoint )
    {
        if ( selectedPoint < 0 || selectedPoint >= mPointCount || selectedPoint == mSelectedPoint )
        {
            return;
        }
        mSelectedPoint = selectedPoint;
        if ( mListener != null )
        {
            mListener.onPointSelected( selectedPoint );
        }
        invalidate();
    }

    public void clearSelectedPoint()
    {
        if ( mSelectedPoint >= 0 && mSelectedPoint < mPointCount )
        {
            mSelectedPoint = SELECTED_POINT_NONE;
            invalidate();
        }
    }

    private void setThumbPos( int w, Drawable thumb, float scale, int offset )
    {
        int available = w - getPaddingLeft() - getPaddingRight();
        final int thumbWidth = thumb.getIntrinsicWidth();
        final int thumbHeight = thumb.getIntrinsicHeight();
        available -= thumbWidth;
        final int thumbPos = (int) ( scale * available + 0.5f);

        final int top;
        final int bottom;
        if ( offset == Integer.MIN_VALUE )
        {
            final Rect oldBounds = thumb.getBounds();
            top = oldBounds.top;
            bottom = oldBounds.bottom;
        }
        else
        {
            top = offset;
            bottom = offset + thumbHeight;
        }

        final int left = thumbPos;
        final int right = left + thumbWidth;
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
        {
            final Drawable background = getBackground();
            if ( background != null )
            {
                final int offsetY = getPaddingTop();
            }
        }
        thumb.setBounds( left, top, right, bottom );
    }

    public interface GetTextImpl
    {
        String getText( int progress );
    }

    public interface Listener
    {
        void onPointCountChanged( int pointCount );

        void onPointSelected( int index );

        void onMultiPointTouched( List<Integer> points );

        void onStartPointTouch( int index );

        void onStopPointTouch( int index );

        void onPointProgressChanged( int index, int progress, boolean fromUser );
    }

}
