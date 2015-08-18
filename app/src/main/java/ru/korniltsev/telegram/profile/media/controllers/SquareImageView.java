package ru.korniltsev.telegram.profile.media.controllers;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class SquareImageView extends ImageView {
    public SquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private boolean horizontal;

    public void setHorizontal(boolean horizontal) {
        this.horizontal = horizontal;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (horizontal){
            super.onMeasure(heightMeasureSpec, heightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        }
    }
}
