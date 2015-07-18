package ru.korniltsev.telegram.profile.decorators;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import ru.korniltsev.telegram.common.AppUtils;

public class DividerItemDecorator extends RecyclerView.ItemDecoration{
    final int paddingLeft;
    final int color;
    final int itemPosition;
    final Paint paint ;
    private final Paint whitePaint;

    public DividerItemDecorator(int paddingLeft, int color, int itemPosition) {
        this.paddingLeft = paddingLeft;
        this.color = color;
        this.itemPosition = itemPosition;
        paint = new Paint();
        paint.setColor(color);
        whitePaint = new Paint();
        whitePaint.setColor(Color.WHITE);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {

        AppUtils.clear(outRect);
        if (parent.getChildViewHolder(view).getAdapterPosition() == itemPosition){
            outRect.bottom = 1;
        }
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final View targetView = AppUtils.getChildWithAdapterPosition(parent, itemPosition);
        if (targetView != null) {
            drawDivider(c, targetView);
        }
    }

    private void drawDivider(Canvas c, View child) {
        c.drawLine(child.getLeft(), child.getBottom(),
                child.getRight(), child.getBottom(), whitePaint);
        c.drawLine(child.getLeft() + paddingLeft, child.getBottom(),
                child.getRight(), child.getBottom(), paint);
    }
}
