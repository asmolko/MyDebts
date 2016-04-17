package com.dao.mydebts.misc;

import android.graphics.drawable.Drawable;
import android.util.LruCache;

/**
 * Cache for loaded avatars, profile images etc.
 *
 * @author Oleg Chernovskiy on 17.04.16.
 */
public class ImageCache {

    private static ImageCache ourInstance = new ImageCache();

    private LruCache<String, Drawable> mCache = new LruCache<>(100);

    public static ImageCache getInstance() {
        return ourInstance;
    }

    private ImageCache() {
    }

    public Drawable get(String url) {
        if(url == null)
            return null;

        return mCache.get(url);
    }

    public Drawable put(String url, Drawable value) {
        return mCache.put(url, value);
    }
}
