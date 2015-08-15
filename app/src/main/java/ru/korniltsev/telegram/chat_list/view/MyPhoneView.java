package ru.korniltsev.telegram.chat_list.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;

public class MyPhoneView extends View {

    private final int width;
    private TextPaint textPaint;
    private StaticLayout staticLayout;

    public MyPhoneView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final MyApp from = MyApp.from(context);
        width = from.displayWidth;
        final DpCalculator calc = from.dpCalculator;
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(calc.dp(14f));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final float lineWidth = staticLayout.getLineWidth(0);
        final int height = staticLayout.getHeight();
        setMeasuredDimension((int) (lineWidth + 1), height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        staticLayout.draw(canvas);
    }

    public void setText(String text) {
        staticLayout = createLayout(text);
        invalidate();
    }

    @NonNull
    private StaticLayout createLayout(String text) {
        return new StaticLayout(text, textPaint, width, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
    }
}
