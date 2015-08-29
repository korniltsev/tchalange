package ru.korniltsev.telegram.chat;

import android.content.Context;
import android.os.SystemClock;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import ru.korniltsev.telegram.core.Utils;

public class DebugRecyclerView extends RecyclerView {
    public DebugRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        long start = System.nanoTime();
        super.onMeasure(widthSpec, heightSpec);
        long end = System.nanoTime();
        Utils.logDuration(start, end, "measure Chat RecyclerView");
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        long start = System.nanoTime();
        super.onLayout(changed, l, t, r, b);
        long end = System.nanoTime();
        Utils.logDuration(start, end, "onLayout Chat RecyclerView");
    }
}
