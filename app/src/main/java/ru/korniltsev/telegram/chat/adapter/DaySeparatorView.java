package ru.korniltsev.telegram.chat.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.rx.StaticLayoutCache;

public class DaySeparatorView extends View {
    private static  TextPaint p;
    private final StaticLayoutCache staticLayoutCache;
    private final int displayWidth;
    private final DpCalculator calc;
    private StaticLayout layout;
    private int height ;
    private float tx;
    private int ty;

    public DaySeparatorView(Context context) {
        super(context);
        final MyApp from = MyApp.from(context);
        staticLayoutCache = from.staticLayoutCache;
        displayWidth = from.displayWidth;
        calc = from.calc;
        if (p == null) {
            p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            p.setTextSize(calc.dp(16));
            p.setColor(0xff252525);
            p.setTypeface(Typeface.create((String) null, Typeface.BOLD));
        }

        height = calc.dp(32);
        setWillNotDraw(false);
    }

    public void setText(String text) {
        layout = staticLayoutCache.getLayout(displayWidth, p, text);
        tx = (displayWidth - layout.getLineWidth(0))/2;
        ty = (height - layout.getHeight())/2;

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(displayWidth, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(tx, ty);
        layout.draw(canvas);
        canvas.restore();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
