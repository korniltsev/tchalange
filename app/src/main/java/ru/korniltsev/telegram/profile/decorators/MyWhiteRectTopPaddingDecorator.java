package ru.korniltsev.telegram.profile.decorators;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import ru.korniltsev.telegram.common.AppUtils;

public class MyWhiteRectTopPaddingDecorator extends RecyclerView.ItemDecoration {
    final int position;
    final int height;
    final Paint paint;

    public MyWhiteRectTopPaddingDecorator(int position, int height) {
        this.position = position;
        this.height = height;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
    }

    @Override
    public void getItemOffsets(Rect outRect, View child, RecyclerView parent, RecyclerView.State state) {
        AppUtils.clear(outRect);
        if (parent.getChildViewHolder(child).getAdapterPosition() == position){
            outRect.top = height;
        }
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final View targetView = AppUtils.getChildWithAdapterPosition(parent, position);
        if (targetView != null){
            c.drawRect(targetView.getLeft(), targetView.getTop() - height,
                    targetView.getRight(), targetView.getTop(),
                    paint);
        }
    }
}
