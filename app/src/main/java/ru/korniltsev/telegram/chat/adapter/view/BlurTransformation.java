package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.graphics.Bitmap;
import com.crashlytics.android.core.CrashlyticsCore;
import com.squareup.picasso.Transformation;
import ru.korniltsev.blur.rs.Blur;

public class BlurTransformation implements Transformation {
    final Context appCtx;
    private float radius;

    public BlurTransformation(Context appCtx, float radius) {
        this.appCtx = appCtx;
        this.radius = radius;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        final Bitmap result;
        try {

            result = Blur.blur(appCtx, source, radius);
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
