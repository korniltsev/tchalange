package ru.korniltsev.blur.rs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;



public class Blur {

    public static Bitmap blur(Context ctx, Bitmap src, float radius) {
        int width = src.getWidth();
        int height = src.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

        canvas.drawBitmap(src, 0, 0, paint);

        RenderScript rs = RenderScript.create(ctx);
        Allocation input = Allocation.createFromBitmap(rs, bitmap,
                Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        Allocation output = Allocation.createTyped(rs, input.getType());
        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

        blur.setInput(input);
        blur.setRadius(radius);
        blur.forEach(output);
        output.copyTo(bitmap);
        rs.destroy();

        return bitmap;
    }
}
