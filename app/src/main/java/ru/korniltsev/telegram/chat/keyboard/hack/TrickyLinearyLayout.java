package ru.korniltsev.telegram.chat.keyboard.hack;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

public class TrickyLinearyLayout extends LinearLayout {
    private int trickyMargin;
    private int fixedHeight = -1;
    private int fixedMargin;

    public TrickyLinearyLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (fixedHeight != -1) {
            log("fixed: " +fixedHeight);
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(fixedHeight, MeasureSpec.EXACTLY));
        } else {
            log("minus margin : " +trickyMargin);
            final int mode = MeasureSpec.getMode(heightMeasureSpec);
            final int size = MeasureSpec.getSize(heightMeasureSpec);
            if (mode != MeasureSpec.EXACTLY) {
                throw new IllegalStateException("the view works only with MeasureSpec.EXACTLY");
            }
            final int newHeight = Math.max(1, size - trickyMargin);//:O
            int newHeightSpec = MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY);
            super.onMeasure(widthMeasureSpec, newHeightSpec);
        }
        log("measuredHeight: " + getMeasuredHeight());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        log("h: " + getHeight());
    }

    private int log(String msg) {
        return Log.d("TrickyLinearyLayout", msg);
    }

    public void setTrickyMargin(int trickyMargin) {
        this.trickyMargin = trickyMargin;
        requestLayout();
        log("setTrickyMargin " + trickyMargin)
        ;
    }

    public void fixHeight() {
        this.fixedHeight = getHeight();
        this.fixedMargin = trickyMargin;
        requestLayout();
        log("fix height ");
    }

    public void resetFixedheight() {
        log("reset fixed height ");
        if (fixedHeight != -1){
            log("reset fixed height impl");
            fixedHeight = -1;
            requestLayout();
        }

    }

    public void updateFixedHeight(int keyboardHeight) {
        if (fixedHeight != -1
                && fixedMargin != 0
                && fixedMargin != keyboardHeight ){
            int diff = fixedMargin - keyboardHeight;
            fixedHeight += diff;
            fixedMargin = keyboardHeight;
            log("increase fixed height by " + diff );
            requestLayout();
        } else {
            requestLayout();
        }
    }
}
