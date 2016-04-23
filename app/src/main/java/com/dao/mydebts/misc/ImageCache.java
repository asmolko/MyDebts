package com.dao.mydebts.misc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.util.LruCache;

import com.dao.mydebts.BuildConfig;
import com.dao.mydebts.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import okhttp3.internal.DiskLruCache;
import okhttp3.internal.io.FileSystem;
import okio.Buffer;
import okio.Sink;
import okio.Source;

import static java.lang.Math.min;

/**
 * Cache for loaded avatars, profile images etc.
 *
 * @author Oleg Chernovskiy, Alexander Smolko
 */
public class ImageCache {
    public static final String IC_TAG = "ImageCache";

    private static ImageCache ourInstance;

    private LruCache<String, Drawable> mCache;
    private DiskLruCache mDiskCache;

    public static ImageCache getInstance(Context context) {
        if (ourInstance == null) {
            ourInstance = new ImageCache(context);
        }
        return ourInstance;
    }

    private ImageCache(Context context) {
        mDiskCache = DiskLruCache.create(FileSystem.SYSTEM, new File(context.getCacheDir(), context.getString(R.string.image_cache_name)),
                BuildConfig.VERSION_CODE, 1, R.integer.google_plus_contact_image_cache_max_size);
        mCache = new LruCache<>(R.integer.google_plus_contacts_cache_size);
    }

    public Drawable get(String url) {
        if (url == null)
            return null;

        Drawable drawable = mCache.get(url);

        if (drawable == null) {
            try {
                String diskKey = getDiskKey(url);
                DiskLruCache.Snapshot snapshot = mDiskCache.get(diskKey);
                if (snapshot == null) {
                    Log.d(IC_TAG, "Disk Cache for " + diskKey + " key doesn't exists");
                    return null;
                }
                Source source = snapshot.getSource(0);
                Buffer buffer = new Buffer();
                while (source.read(buffer, 1000) != -1) {
                }
                Bitmap bitmap = BitmapFactory.decodeStream(buffer.inputStream());
                if (bitmap == null) {
                    return null;
                }

                RoundedBitmapDrawable rbd = RoundedBitmapDrawableFactory.create(null, bitmap);
                rbd.setCornerRadius(Math.max(bitmap.getWidth(), bitmap.getHeight()) / 2.0f);

                mCache.put(diskKey, rbd);
            } catch (IOException e) {
                Log.e(IC_TAG, "Disk Cache is corrupted", e);
                try {
                    mDiskCache.delete();
                } catch (IOException e1) {
                    Log.e(IC_TAG, "Disk Cache is corrupted, delete failed",e1);
                }
            }
        }

        return drawable;
    }

    private String getDiskKey(String url) {
        //keys must match regex [a-z0-9_-]{1,120}
        String key = url.replace('/', '_').replace(':', '_').replace('.', '_');
        return key.substring(0, min(key.length(), 120)).toLowerCase();
    }

    public void put(String url, RoundedBitmapDrawable value) {
        mCache.put(url, value);
        new DiskSaverTask().execute(new CacheEntry(url, value));
    }

    class CacheEntry {
        String key;
        RoundedBitmapDrawable value;

        public CacheEntry(String key, RoundedBitmapDrawable value) {
            this.key = key;
            this.value = value;
        }
    }

    class DiskSaverTask extends AsyncTask<CacheEntry, Void, Void> {

        @Override
        protected Void doInBackground(CacheEntry... params) {
            for (CacheEntry cacheEntry : params) {
                try {
                    DiskLruCache.Editor editor = mDiskCache.edit(getDiskKey(cacheEntry.key));
                    if (editor == null) {
                        return null;
                    }
                    Sink sink = editor.newSink(0);

                    Bitmap bitmap = cacheEntry.value.getBitmap();
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);

                    Buffer buffer = new Buffer();
                    buffer.write(stream.toByteArray());

                    sink.write(buffer, buffer.size());

                    editor.commit();
                } catch (IOException e) {
                    Log.e(IC_TAG, "Disk Cache is corrupted");
                    try {
                        mDiskCache.delete();
                    } catch (IOException e1) {
                        Log.e(IC_TAG, "Disk Cache is corrupted, delete failed");
                    }
                }
            }
            return null;
        }
    }
}
