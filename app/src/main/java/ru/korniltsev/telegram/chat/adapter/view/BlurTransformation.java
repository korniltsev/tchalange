package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.graphics.Bitmap;
import com.crashlytics.android.core.CrashlyticsCore;
import com.squareup.picasso.Transformation;
import ru.korniltsev.telegram.common.FastBlur;

public class BlurTransformation implements Transformation {

    final int radius;

    public BlurTransformation(int radius) {
        this.radius = radius;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        try {
            Bitmap result = FastBlur.doBlur( source, radius, false);
            source.recycle();
            return result;
        } catch (Throwable th) {
            CrashlyticsCore.getInstance().logException(th);
            return source;
        }
    }

    @Override
    public String key() {
        return "renderscript-blur";
    }
}
