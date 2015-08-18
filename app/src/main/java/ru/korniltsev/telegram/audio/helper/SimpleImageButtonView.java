package ru.korniltsev.telegram.audio.helper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

public class SimpleImageButtonView extends View {
    private Drawable[]ds;
    @Nullable private Drawable current;
    public SimpleImageButtonView(Context context) {
        super(context);
    }

    public void setDs(Drawable[] ds) {
        this.ds = ds;

    }

    public void setCurrent(int i) {
        this.current = ds[i];
        invalidate();
    }



    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

        final int w = getWidth();
        final int h = getHeight();
        for (Drawable d : ds) {
            int iconW = d.getIntrinsicWidth();
            int iconH = d.getIntrinsicHeight();
            int ty = (h-iconH)/2;
            int tx = (w - iconW) / 2;
            int iconLeft = tx;
            int iconRight = iconLeft + iconW;
            int iconTop =  ty;
            int iconBottom = iconTop + iconH;
            d.setBounds(iconLeft, iconTop, iconRight, iconBottom);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (current != null){
            current.draw(canvas);
        }
    }
}
