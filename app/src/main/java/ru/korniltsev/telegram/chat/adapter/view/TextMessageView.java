package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.crashlytics.android.core.CrashlyticsCore;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.emoji.images.Emoji;
import ru.korniltsev.telegram.core.rx.EmojiParser;
import ru.korniltsev.telegram.core.rx.StaticLayoutCache;
import rx.Subscription;

import java.util.Arrays;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_UP;

public class TextMessageView extends View implements Emoji.Listener {
    private final int width;
    private final Emoji emoji;
    private final EmojiParser emojiParser;
    private StaticLayout staticLayout;
    private static TextPaint paint;
    //    private Subscription subscription;
    private Spannable currentText;
    @Nullable private EmojiParser.ReferenceSpan currentTouchSpan;
    private StaticLayoutCache layoutCache;

    public TextMessageView(Context context) {
        this(context, null);
    }

    public TextMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final MyApp app = MyApp.from(context);
        layoutCache = app.staticLayoutCache;
        final int displayWidth = app.displayWidth;
        final DpCalculator calc = app.calc;

        width = getTextWidth(displayWidth, calc);
        initPaints(calc);
        emoji = app.emoji;
        emojiParser = app.emojiParser;
    }

    public static synchronized void initPaints(DpCalculator calc) {
        if (paint == null) {
            paint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.LINEAR_TEXT_FLAG);
            paint.setColor(Color.BLACK);
            paint.setTextSize(calc.dp(14f));
        }
    }

    public static int getTextWidth(int displayWidth, DpCalculator calc) {
        return displayWidth - calc.dp(41 + 9 + 11 + 8);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        emoji.addListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        emoji.removeListener(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            final int actionMasked = event.getActionMasked();
            if (actionMasked == ACTION_DOWN
                    || actionMasked == ACTION_UP) {
                int lineHeight = staticLayout.getHeight() / staticLayout.getLineCount();
                final int y = (int) event.getY();
                int lineNumber = y / lineHeight;
                final float x = event.getX();
                final float lineWidth = staticLayout.getLineWidth(lineNumber);
                if (x > lineWidth) {
                    return false;
                }
                final int offset = staticLayout.getOffsetForHorizontal(lineNumber, x);
                final EmojiParser.ReferenceSpan[] spans = currentText.getSpans(offset, offset, EmojiParser.ReferenceSpan.class);
                if (spans.length == 0) {
                    return false;
                }
                if (actionMasked == ACTION_DOWN) {
                    currentTouchSpan = spans[0];
                    return true;
                } else {
                    if (currentTouchSpan == spans[0]) {
                        emojiParser.getClickedSpans()
                                .onNext(spans[0]);
                        return true;
                    }
                }
                Log.d("TextMessageView", "touchodwn on line number " + lineNumber + " and offset " + offset + " spans: " + Arrays.toString(spans));
            }
        } catch (Exception e) {
            CrashlyticsCore.getInstance().logException(e);
        }
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int height = staticLayout.getHeight();
        setMeasuredDimension(width, height);
    }

    public void setText(@NonNull Spannable text) {
        this.currentText = text;
        staticLayout = getLayoutForText(layoutCache, width, text);
        requestLayout();
    }

    public static StaticLayout getLayoutForText(StaticLayoutCache cache, int width, @NonNull Spannable text) {
        return cache.getLayout(width, paint, text);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        staticLayout.draw(canvas);
    }

    @Override
    public void pageLoaded(int page) {
        if (currentText != null) {
            final Emoji.EmojiSpan[] spans = currentText.getSpans(0, currentText.length(), Emoji.EmojiSpan.class);
            if (spans.length != 0) {
                for (Emoji.EmojiSpan span : spans) {
                    if (span.d.info.page == page) {
                        invalidate();
                        return;
                    }
                }
            }
        } else {
            invalidate();
        }
    }
}
