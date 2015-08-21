package ru.korniltsev.telegram.chat.debug;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

public class SquareDumbResourceView {
    private final Drawable[] ds;
    private Drawable current;

    int tx;
    int ty;
    final View host;

    public SquareDumbResourceView(Drawable[] ds, View host) {
        this.ds = ds;
        this.host = host;
    }

    public void setSate(int state) {
        if (current != ds[state]) {
            current = ds[state];
            host.invalidate();
        }
    }

    public void draw(Canvas c) {
        if (current != null) {
            c.save();
            c.translate(tx, ty);
            current.draw(c);
            c.restore();
        }
    }

    public void layout(int iconRightTop, int iconRightLeft) {
        tx = iconRightLeft;
        ty = iconRightTop;
    }
}
