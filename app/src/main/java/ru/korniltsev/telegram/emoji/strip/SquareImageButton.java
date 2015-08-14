package ru.korniltsev.telegram.emoji.strip;

import android.content.Context;
import android.widget.ImageButton;

class SquareImageButton extends ImageButton {

    public SquareImageButton(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, heightMeasureSpec);
    }
}
