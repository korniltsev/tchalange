package ru.korniltsev.telegram.core.utils.bitmap;

import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.picasso.LruCache;
import junit.framework.Assert;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

public class BitmapPool {
    public static final Size STICKER_THUMB = new Size(128, 128);
    public static final boolean BITMAP_REUSE_SUPPORTED = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    private List<SoftReference<Bitmap>> stickerThumbs = new ArrayList<>();

    WeakHashMap<Bitmap, RefCountBitmap> refCounts = new WeakHashMap<>();

    public LruCache createPicassoCache(int maxSize) {
        return new LruCache(maxSize){

            @Override
            public void set(String key, Bitmap bitmap) {
                super.set(key, bitmap);
                if (BITMAP_REUSE_SUPPORTED) {
                    acquire(bitmap);
                }
            }

            @Override
            public void entryEvicted(Bitmap bitmap) {
                if (BITMAP_REUSE_SUPPORTED) {
                    release(bitmap);
                }
            }
        };
    }

    public synchronized void release(Bitmap bitmap) {
        final RefCountBitmap refCount = refCounts.get(bitmap);
        if (refCount == null) {
            return;
        }
        refCount.refCount--;
        if (refCount.refCount == 0) {
            saveSoftReference(bitmap);
        }
    }

    private synchronized void saveSoftReference(Bitmap bitmap) {
        this.stickerThumbs.add(new SoftReference<>(bitmap));
    }

    public synchronized void acquire(Bitmap bitmap) {
        final RefCountBitmap refCount = refCounts.get(bitmap);
        if (refCount == null) {
            return;
        }
        refCount.refCount++;
    }
    //todo transformations!!
    @NonNull public synchronized Bitmap get(){
        final Bitmap bitmapForReuse = getBitmapForReuse();
        if (bitmapForReuse != null){
            final RefCountBitmap refCount = refCounts.get(bitmapForReuse);
            Assert.assertTrue(refCount != null);
            Assert.assertTrue(refCount.refCount == 0);
            refCount.refCount = 1;
            return bitmapForReuse;
        }
        final Bitmap bitmap = Bitmap.createBitmap(STICKER_THUMB.w, STICKER_THUMB.h, Bitmap.Config.ARGB_8888);
        refCounts.put(bitmap, new RefCountBitmap(STICKER_THUMB));
        return bitmap;

    }

    @Nullable private synchronized Bitmap getBitmapForReuse(){
        final Iterator<SoftReference<Bitmap>> it = stickerThumbs.iterator();
        while (it.hasNext()){
            final SoftReference<Bitmap> next = it.next();
            final Bitmap bmp = next.get();
            it.remove();
            if (bmp != null) {
                return bmp;
            } 
        }
        return null;
    }

    public void assertNotReleased(Bitmap newBitmap) {
        final RefCountBitmap count = refCounts.get(newBitmap);
        Assert.assertNotNull(count);
        Assert.assertTrue(count.refCount != 0);

    }

    public class RefCountBitmap {
        final Size size;
        int refCount = 1;

        public RefCountBitmap(Size size) {
            this.size = size;
        }


    }

    public static final class Size {
        final int w;
        final int h;

        private Size(int w, int h) {
            this.w = w;
            this.h = h;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Size size = (Size) o;

            if (w != size.w) {
                return false;
            }
            return h == size.h;
        }

        @Override
        public int hashCode() {
            int result = w;
            result = 31 * result + h;
            return result;
        }
    }

//    class MyBitmap extends Bitmap {
//
//    }
}
