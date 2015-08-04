package ru.korniltsev.telegram.chat.debug;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RelativeLayout;

public class DebugRelativeLayout extends RelativeLayout {
    public DebugRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        long start = System.nanoTime();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        long end = System.nanoTime();
//        log(start, end, "measure");
    }

    public static void log(long start, long end, String measure) {

        long duration = end - start;
        Log.d("DebugRelativeLayout", measure + " in " + duration + " nano Seconds");
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }
}
