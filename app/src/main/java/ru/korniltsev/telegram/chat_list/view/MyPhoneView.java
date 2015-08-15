package ru.korniltsev.telegram.chat_list.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;

public class MyPhoneView extends View {

    private final int width;
    private TextPaint textPaint;
    private StaticLayout staticLayout;

    public MyPhoneView(Context context, int textSize, boolean bold) {
        super(context);
        final MyApp from = MyApp.from(context);
        final DpCalculator calc = from.dpCalculator;
        width = calc.dp(304) - calc.dp(18) * 2;
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(textSize);
        if (bold) {
            textPaint.setTypeface(Typeface.create((String)null, Typeface.BOLD));
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int height = staticLayout.getHeight();
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        staticLayout.draw(canvas);
    }

    public void setText(String text) {
        final CharSequence ellipsized = TextUtils.ellipsize(text, textPaint, width, TextUtils.TruncateAt.END);
        staticLayout = createLayout(ellipsized);
        invalidate();
    }

    @NonNull
    private StaticLayout createLayout(CharSequence text) {
        return new StaticLayout(text, textPaint, width, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
    }
}
