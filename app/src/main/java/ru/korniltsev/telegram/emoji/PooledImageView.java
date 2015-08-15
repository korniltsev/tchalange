package ru.korniltsev.telegram.emoji;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.utils.bitmap.BitmapPool;

import javax.inject.Inject;

public class PooledImageView extends View {
    private final Paint paint;
    private final BitmapPool bitmapPool;
    int size = -1;
    @Inject RxGlide glide;
    private final MyTarget target;

    public PooledImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        bitmapPool = MyApp.from(context).bitmapPool;
        target = new MyTarget();
        setWillNotDraw(false);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }



    public void setSize(int size) {
        this.size = size;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(size, size);
    }

    public void bind(TdApi.File photo, boolean b) {
        setBitmap(null);

        glide.loadPhoto(photo, true, BitmapPool.STICKER_THUMB)
                .priority(Picasso.Priority.HIGH)
                .into(target);
    }

    @Nullable private Bitmap bitmap;


    private class MyTarget implements Target {

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            setBitmap(bitmap);
            //todo fade
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            //do nothing
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            //do nothing
        }
    }

    final Rect targetRect = new Rect();
    private void setBitmap(Bitmap newBitmap) {
        if (this.bitmap != null){
//            bitmapPool.release(this.bitmap);
        }
        this.bitmap = newBitmap;
        if (newBitmap != null){
//            bitmapPool.assertNotReleased(newBitmap);
            final int w = newBitmap.getWidth();
            final int h = newBitmap.getHeight();
            float ratio = (float)w/h;
            if (w < h){
                final int targetHeight = size;
                final int targetWidth = (int) (targetHeight * ratio);

                final int top = 0;
                final int bottom = size;
                final int left = (size - targetWidth)/2;
                final int right = left + targetWidth;
                targetRect.set(left, top, right, bottom);
            } else {
                final int targetWidth = size;
                final int targetHeight = (int) (targetWidth / ratio);

                final int left = 0;
                final int right = size;
                final int top = (size - targetHeight)/2;
                final int bottom = top + targetHeight;
                targetRect.set(left, top, right, bottom);
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, null, targetRect, paint);
        }
    }
}
