package ru.korniltsev.telegram.chat_list.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class ChatListDividerItemDecorator extends RecyclerView.ItemDecoration {
    private final int from;
    private final Paint p;

    public ChatListDividerItemDecorator(int from ) {
        this.from = from;
//        from = calc.dp(72);
//        setWillNotDraw(false);
        p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xffd4d4d4);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.set(0, 0, 0, 1);
    }

    final Rect rect = new Rect();
    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final int childCount = parent.getChildCount();
        for (int i =0; i < childCount ; ++i) {
            final View childAt = parent.getChildAt(i);
            final int b = childAt.getBottom();
            final int r = childAt.getRight();
            this.rect.set(from, b - 1, r, b);
            c.drawRect(rect, p);
        }
    }
}
