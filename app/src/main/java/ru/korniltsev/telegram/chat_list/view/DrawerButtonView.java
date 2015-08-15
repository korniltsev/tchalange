package ru.korniltsev.telegram.chat_list.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.View;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;

public class DrawerButtonView extends View {

    private final int height;
    private final int displayWidth;
    private final DpCalculator calc;
    private Drawable icon;
    private TextPaint textPaint;
    private StaticLayout text;
    private int ty;
    private int tx;

    public DrawerButtonView(Context context, int height, int width, DpCalculator calc, String text, Drawable icon) {
        super(context);
        final int dip16 = calc.dp(16f);

        this.height = height;
        displayWidth = width;
        this.calc = calc;
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF222222);

        textPaint.setTextSize(dip16);

        this.icon = icon;
        this.text = new StaticLayout(text, textPaint, displayWidth, Layout.Alignment.ALIGN_NORMAL, 1f, 0, false);
        ty = (height - this.text.getHeight())/2;
        tx = calc.dp(71);
        int iconTy = (height - icon.getIntrinsicHeight())/2;
        int left = dip16;
        int right = left + icon.getIntrinsicWidth();
        int top = iconTy;
        int bottom = top + icon.getIntrinsicHeight();
        icon.setBounds(left, top, right, bottom);

    }



    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(tx, ty);
        text.draw(canvas);
        canvas.restore();
        icon.draw(canvas);
    }
}
