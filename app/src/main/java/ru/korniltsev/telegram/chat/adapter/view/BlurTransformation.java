package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.graphics.Bitmap;
import com.crashlytics.android.core.CrashlyticsCore;
import com.squareup.picasso.Transformation;
import ru.korniltsev.blur.rs.Blur;

public class BlurTransformation implements Transformation {
    final Context appCtx;

    public BlurTransformation(Context appCtx) {
        this.appCtx = appCtx;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        final Bitmap result;
        try {
            result = Blur.blur(appCtx, source, 25f);
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
