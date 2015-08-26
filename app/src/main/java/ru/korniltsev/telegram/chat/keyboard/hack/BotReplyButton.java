package ru.korniltsev.telegram.chat.keyboard.hack;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;

public class BotReplyButton extends View {
    public static final int COLOR_UNPRESSED = 0xffE4E7E9;
    public static final int COLOR_PRESSED = 0xff75c1f7;
    private final DpCalculator calc;
    private final int padding;
    private final Paint pressedPaint;
    private final int radius;
    private CharSequence text;
    private TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private StaticLayout layout;
    private float tx;
    private int ty;

    public BotReplyButton(Context context) {
        super(context);
        calc = MyApp.from(context).calc;
        padding = calc.dp(16f);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(calc.dp(16f));

        pressedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pressedPaint.setColor(COLOR_UNPRESSED);
        radius = calc.dp(3f);
    }

    public void setText(CharSequence text) {
        this.text = text;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int actionMasked = event.getActionMasked();
        final float x = event.getX();
        final float y = event.getY();
        if (x < 0 || x > getWidth() || y < 0 || y > getHeight()) {
            pressedPaint.setColor(COLOR_UNPRESSED);
            invalidate();
        } else if (actionMasked == MotionEvent.ACTION_DOWN) {
            pressedPaint.setColor(COLOR_PRESSED);
            invalidate();
        } else if (actionMasked == MotionEvent.ACTION_UP
                || actionMasked == MotionEvent.ACTION_CANCEL) {
            pressedPaint.setColor(COLOR_UNPRESSED);
            invalidate();
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        final int width = getWidth();
        int textWi = width - padding * 2;
        this.layout = new StaticLayout(text, textPaint, textWi, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);

        float maxWidth = 0;
        float maxLineLeft = 0;
        for (int i = 0; i < layout.getLineCount(); ++i) {
            final float lineWidth = layout.getLineWidth(i);
            if (lineWidth > maxWidth) {
                maxWidth = lineWidth;
                maxLineLeft = layout.getLineLeft(i);
            }
//            maxWidth = (int) Math.max(maxWidth, lineWidth);
        }


        tx = padding + (width - padding * 2 - maxWidth) / 2 - maxLineLeft;
        ty = (getHeight() - layout.getHeight()) / 2;

        r.set(0, 0, getWidth(), getHeight());
    }

    final RectF r = new RectF();

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawRoundRect(r, radius, radius, pressedPaint);

        canvas.save();
        canvas.translate(tx, ty);
        layout.draw(canvas);
        canvas.restore();
    }
}
