package com.ycuwq.calendarview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * 一周的数据
 * Created by ycuwq on 2018/2/11.
 */
public class WeekView extends View {

    private final String TAG = getClass().getSimpleName();

    private static final int WEEK_SIZE = 7;

    private Paint mPaint;

    private List<Date> mDates;

    private CalendarViewDelegate mCalendarViewDelegate;

    private int mTextMaxWidth, mTextMaxHeight;
    private int mItemWidth;
    private int mFirstItemDrawX, mFirstItemDrawY;
    private Rect mDrawnRect;
    private int mTouchSlop;

    /**
     * 在月中的第几周
     */
    private int mWeekOrder;

    private float mTouchDownX, mTouchDownY;

    private int mSelectedItemPosition = -1;

    private OnDaySelectedListener mOnDaySelectedListener;

    public WeekView(Context context) {
        super(context);
    }

    public WeekView(Context context, @Nullable AttributeSet attrs) {
        super(context);
        List<Date> list = new ArrayList<>();
        for (int i = 1; i < 8; i++) {
            Date date = new Date(2018, 2, i);
            date.setHoliday("ABSDA");
            list.add(date);
        }
        mDates = list;
        mCalendarViewDelegate = new CalendarViewDelegate();
        mCalendarViewDelegate.setSelectedBg(context.getResources().getDrawable(R.drawable.com_ycuwq_calendarview_blue_circle));
        initPaint();
        computeTextSize();
        mDrawnRect = new Rect();
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public WeekView(Context context, @NonNull List<Date> dates, @NonNull CalendarViewDelegate calendarViewDelegate, int order) {
        super(context);
        mWeekOrder = order;
        mDates = dates;
        mCalendarViewDelegate = calendarViewDelegate;
        initPaint();
        computeTextSize();
        mDrawnRect = new Rect();
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setDateList(List<Date> dateList) {
        mDates = dateList;
    }

    private void initPaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.LINEAR_TEXT_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void computeTextSize() {
        mPaint.setTextSize(mCalendarViewDelegate.getTextSizeTop());
        mTextMaxWidth = (int) mPaint.measureText("00");
    }

    public void setOnDaySelectedListener(OnDaySelectedListener onDaySelectedListener) {
        mOnDaySelectedListener = onDaySelectedListener;
    }

    public void selectDate(Date date) {
        int position = mDates.indexOf(date);
        if (position >=0) {
            selectedDate(position);
        } else {
            cancelSelected();
        }
    }

    public void selectedDate(int position) {
        if (position >= 0 && position < mDates.size()) {
            mSelectedItemPosition = position;
            postInvalidate();
        }
    }

    public void cancelSelected() {
        if (mSelectedItemPosition > -1) {
            mSelectedItemPosition = -1;
            postInvalidate();
        }
    }

    /**
     *  计算实际的大小
     * @param specMode 测量模式
     * @param specSize 测量的大小
     * @param size     需要的大小
     * @return 返回的数值
     */
    private int measureSize(int specMode, int specSize, int size) {
        if (specMode == MeasureSpec.EXACTLY) {
            return specSize;
        } else {
            return Math.min(specSize, size);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int specWidthSize = MeasureSpec.getSize(widthMeasureSpec);
        int specWidthMode = MeasureSpec.getMode(widthMeasureSpec);
        int specHeightSize = MeasureSpec.getSize(heightMeasureSpec);
        int specHeightMode = MeasureSpec.getMode(heightMeasureSpec);

        int width = mTextMaxWidth * WEEK_SIZE;
        int height = specWidthSize / WEEK_SIZE;
        width += getPaddingLeft() + getPaddingRight();
        height += getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(measureSize(specWidthMode, specWidthSize, width),
                measureSize(specHeightMode, specHeightSize, height));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mDrawnRect.set(getPaddingLeft(), getPaddingTop(),
                getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        mItemWidth = mDrawnRect.width() / WEEK_SIZE;
        mFirstItemDrawX = mItemWidth / 2;
        mFirstItemDrawY = (int) ((mDrawnRect.height() - (mPaint.ascent() + mPaint.descent())) / 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mSelectedItemPosition >= 0 && mSelectedItemPosition < WEEK_SIZE) {
            Drawable clickBg = mCalendarViewDelegate.getSelectedBg();
            clickBg.setBounds(mSelectedItemPosition * mItemWidth, 0,
                    (mSelectedItemPosition + 1) * mItemWidth, mDrawnRect.bottom);
            clickBg.draw(canvas);
        }
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(mCalendarViewDelegate.getTextSizeTop());
        for (int i = 0; i < mDates.size(); i++) {
            Date date = mDates.get(i);
            if (i == mSelectedItemPosition) {
                //设置点击样式
                mPaint.setColor(mCalendarViewDelegate.getClickTextColor());
            } else if (date.getType() == Date.TYPE_THIS_MONTH){
                mPaint.setColor(mCalendarViewDelegate.getTextColorTop());
            } else {
                //将非当月的Item样式为Bottom的样式，与当月的区分
                mPaint.setColor(mCalendarViewDelegate.getTextColorBottom());
            }
            int itemDrawX = mFirstItemDrawX + i * mItemWidth;
            canvas.drawText(date.getDay() + "", itemDrawX, mFirstItemDrawY, mPaint);
        }
        if (mCalendarViewDelegate.isShowLunar() || mCalendarViewDelegate.isShowHoliday()) {
            mPaint.setColor(mCalendarViewDelegate.getTextColorBottom());
            mPaint.setTextSize(mCalendarViewDelegate.getTextSizeBottom());
            for (int i = 0; i < mDates.size(); i++) {
                if (i == mSelectedItemPosition) {
                    continue;
                }
                Date date = mDates.get(i);
                String text = null;
                if (mCalendarViewDelegate.isShowLunar()) {
                    text = date.getLunarDay();
                }
                if (mCalendarViewDelegate.isShowHoliday()) {
                    if (!TextUtils.isEmpty(date.getLunarHoliday())) {
                        text = date.getLunarHoliday();
                    }
                    if (!TextUtils.isEmpty(date.getHoliday())) {
                        text = date.getHoliday();
                    }
                }
                if (text != null) {
                    int itemDrawX = mFirstItemDrawX + i * mItemWidth;
                    canvas.drawText(text + "", itemDrawX, mFirstItemDrawY * 1.4f, mPaint);
                }
            }

        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownX = event.getX();
                mTouchDownY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
                float disX = mTouchDownX - event.getX();
                float disY = mTouchDownY - event.getY();
                if (Math.abs(disX) < mTouchSlop && Math.abs(disY) < mTouchSlop) {
                    if (mDrawnRect.contains((int) event.getX(), (int) event.getY())) {
                        int position = (int) (event.getX() / mItemWidth);
//                        postInvalidate();
                        if (mOnDaySelectedListener != null) {
                            mOnDaySelectedListener.onDaySelected(mDates.get(position), position, mWeekOrder);
                        }
                        return true;
                    }
                }
                break;
        }
        return super.onTouchEvent(event);
    }
    interface OnDaySelectedListener {
        void onDaySelected(Date date, int position, int weekOrder);
    }

}
