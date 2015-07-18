package ru.korniltsev.telegram.profile.decorators;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import ru.korniltsev.telegram.common.AppUtils;

public class InsetDecorator extends RecyclerView.ItemDecoration{
    final int position;
    final int topOffset;

    public InsetDecorator(int position, int topOffset) {
        this.position = position;
        this.topOffset = topOffset;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.set(0,0,0,0);
        if (parent.getChildViewHolder(view).getAdapterPosition() == position) {
            outRect.top = topOffset;
        }

    }
}
