package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import com.google.common.primitives.Chars;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.emoji.images.Emoji;
import rx.Subscription;

public class TextMessageView extends View {
    private final int width;
    private final Emoji emoji;
    private StaticLayout staticLayout;
    private TextPaint paint;
    private Subscription subscription;
    private CharSequence currentText;

    public TextMessageView(Context context) {
        super(context);
        final MyApp from = MyApp.from(context);
        final int displayWidth = from.displayWidth;
        final DpCalculator calc = from.dpCalculator;

        width = displayWidth - calc.dp(41 + 9 + 11 + 8);
        paint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.LINEAR_TEXT_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTextSize(calc.dp(14f));
        emoji = MyApp.from(context).emoji;
        AppUtils.rtlPerformanceFix(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        subscription = emoji.pageLoaded().subscribe(new ObserverAdapter<Bitmap>() {
            @Override
            public void onNext(Bitmap response) {
                invalidate();
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        subscription.unsubscribe();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int height = staticLayout.getHeight();
        setMeasuredDimension(width, height);
    }

    public void setText(@NonNull CharSequence text) {
        if (currentText != null && currentText.equals(text)) {
            return;
        }
        this.currentText = text;
        staticLayout = new StaticLayout(text, paint, width, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
        requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        staticLayout.draw(canvas);
    }
}
