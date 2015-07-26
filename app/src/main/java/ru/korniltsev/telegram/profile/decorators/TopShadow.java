package ru.korniltsev.telegram.profile.decorators;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.core.emoji.DpCalculator;

public class TopShadow extends RecyclerView.ItemDecoration {
    final Context ctx;
    final DpCalculator calc;
    final int position;
    private final Drawable drawable;
    private final int dp4;

    public TopShadow(Context ctx, DpCalculator calc, int position) {
        this.ctx = ctx;
        this.calc = calc;
        this.position = position;
        drawable = ctx.getResources()
                .getDrawable(R.drawable.shadow_top
                );
        dp4 = calc.dp(4);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        AppUtils.clear(outRect);
        if (parent.getChildViewHolder(view).getAdapterPosition() == position){
            outRect.top = dp4;
        }

    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final View targetView = AppUtils.getChildWithAdapterPosition(parent, position);
        if (targetView != null) {
            drawable.setBounds(targetView.getLeft(),
                    targetView.getTop() - dp4,
                    targetView.getRight(),
                    targetView.getTop());
            drawable.draw(c);
        }
    }
}
