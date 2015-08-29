package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.LruCache;

import javax.inject.Inject;
import javax.inject.Singleton;


public class StaticLayoutCache {
    private final LruCache<Key, StaticLayout> cache;


    public StaticLayoutCache() {
        cache = new LruCache<>(100);
    }

    public void put(Key key, StaticLayout staticLayout) {
        cache.put(key, staticLayout);
    }

    public StaticLayout check(Key key) {
        return cache.get(key);
    }

    public static class Key {
        final CharSequence str;
        final int width;

        public Key(CharSequence str, int width) {
            this.str = str;
            this.width = width;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Key key = (Key) o;

            if (width != key.width) {
                return false;
            }
            return str.equals(key.str);
        }

        @Override
        public int hashCode() {
            int result = str.hashCode();
            result = 31 * result + width;
            return result;
        }
    }
    public StaticLayout getLayout(int width, TextPaint paint, CharSequence text){
//        final int width = 700;
        final StaticLayoutCache.Key key = new StaticLayoutCache.Key(text, width);
        final StaticLayout check = check(key);
        if (check != null) {
            return check;
        }
        final StaticLayout res = new StaticLayout(text, paint, width, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
        put(key, res);
        return res;
    }
    public interface Factory {
        StaticLayout create(int width, TextPaint paint, CharSequence text);
    }


}
