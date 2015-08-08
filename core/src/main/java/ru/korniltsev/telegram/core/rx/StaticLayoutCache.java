package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import android.text.StaticLayout;
import android.util.LruCache;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StaticLayoutCache {
    final Context ctx;
    private final LruCache<Key, StaticLayout> cache;

    @Inject
    public StaticLayoutCache(Context ctx) {
        this.ctx = ctx;
        cache = new LruCache<>(100);
    }

    public void put(Key key, StaticLayout staticLayout) {
        cache.put(key, staticLayout);
    }

    public StaticLayout check(Key key) {
        return cache.get(key);
    }

    public static class Key {
        final String str;
        final int width;

        public Key(String str, int width) {
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


}
