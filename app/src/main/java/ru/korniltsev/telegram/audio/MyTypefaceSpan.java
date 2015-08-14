package ru.korniltsev.telegram.audio;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import android.text.style.TypefaceSpan;

public class MyTypefaceSpan extends MetricAffectingSpan {
    final Typeface font;
    public MyTypefaceSpan(Typeface font) {
//        super("");
        this.font = font;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        apply(ds);
    }

    @Override
    public void updateMeasureState(TextPaint paint) {
        apply(paint);
    }

    private void apply(TextPaint paint) {
        paint.setTypeface(font);
        int flags = paint.getFlags();
        flags |= Paint.SUBPIXEL_TEXT_FLAG;
        paint.setFlags(flags);
    }
}
