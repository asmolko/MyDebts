package com.dao.mydebts.misc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.dao.mydebts.BuildConfig;
import com.dao.mydebts.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
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
    private final ContactImageRetriever mImageLoader = new ContactImageRetriever();

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

    /**
     * Simple retrieval, seeks only cached images
     * @param url url fo search for
     * @return drawable if it is found in cache
     */
    private Drawable getCached(String url) {
        if (TextUtils.isEmpty(url))
            return null;

        Drawable drawable = mCache.get(url);
        if (drawable != null) {
            return drawable;
        }

        try {
            String diskKey = getDiskKey(url);
            DiskLruCache.Snapshot snapshot = mDiskCache.get(diskKey);
            if (snapshot == null) {
                Log.d(IC_TAG, "Disk Cache for " + diskKey + " key doesn't exists");
                return null;
            }
            Source source = snapshot.getSource(0);
            Buffer buffer = new Buffer();
            source.read(buffer, snapshot.getLength(0));
            Bitmap bitmap = BitmapFactory.decodeStream(buffer.inputStream());
            if (bitmap == null) {
                return null;
            }

            RoundedBitmapDrawable rbd = RoundedBitmapDrawableFactory.create(null, bitmap);
            rbd.setCornerRadius(Math.max(bitmap.getWidth(), bitmap.getHeight()) / 2.0f);

            mCache.put(diskKey, rbd);
            drawable = rbd;
        } catch (IOException e) {
            Log.e(IC_TAG, "Disk Cache is corrupted", e);
            try {
                mDiskCache.delete();
            } catch (IOException e1) {
                Log.e(IC_TAG, "Disk Cache is corrupted, delete failed",e1);
            }
        }

        return drawable;
    }

    public void loadImage(String url, ImageView destination) {
        if(TextUtils.isEmpty(url))
            return;

        mImageLoader.loadImage(url, new BadgeCallback(destination));
    }

    private String getDiskKey(String url) {
        //keys must match regex [a-z0-9_-]{1,120}
        String key = url.toLowerCase().replaceAll("[^a-z0-9_-]", "_");
        return key.substring(0, min(key.length(), 120));
    }

    private void putCached(String url, RoundedBitmapDrawable value) {
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

    /**
     * Async loader for person images.
     * Supports caching for better performance.
     */
    private class ContactImageRetriever {

        private final OkHttpClient client = new OkHttpClient();

        public ContactImageRetriever() {
            client.dispatcher().setMaxRequests(5);
        }

        /**
         * This method is called from UI thread, however, its purpose is to offload
         * image retrieval to worker thread (if it's not available in cache).
         * @param url valid URL of image to load
         * @param callback callback to execute after retrieval (executes in worker thread!)
         */
        private void loadImage(String url, BadgeCallback callback) {
            Drawable cached = getCached(url);
            if(cached != null) {
                callback.setRetrieved(cached);
                callback.run();
            } else {
                Request req = new Request.Builder().url(url).build();
                client.newCall(req).enqueue(callback);
            }
        }

    }

    /**
     * Note: Callbacks are executed in thread pool too.
     *
     * @author Oleg Chernovskiy
     */
    private class BadgeCallback implements Callback, Runnable {

        private final ImageView mCaller;
        private Drawable retrieved;

        public BadgeCallback(ImageView mCaller) {
            this.mCaller = mCaller;
        }

        public void setRetrieved(Drawable retrieved) {
            this.retrieved = retrieved;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            Log.w(IC_TAG, "Couldn't retrieve contact avatar", e);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            if (!response.isSuccessful()) {
                return;
            }

            try(ResponseBody body = response.body()) {
                Bitmap loaded = BitmapFactory.decodeStream(body.byteStream());
                if (loaded == null) { // decode failed
                    return;
                }

                Bitmap scaled = Bitmap.createScaledBitmap(loaded, mCaller.getWidth(), mCaller.getHeight(), false);
                RoundedBitmapDrawable rbd = RoundedBitmapDrawableFactory.create(mCaller.getContext().getResources(), scaled);
                rbd.setCornerRadius(Math.max(scaled.getWidth(), scaled.getHeight()) / 2.0f);
                setRetrieved(rbd);

                ImageCache.getInstance(mCaller.getContext()).putCached(call.request().url().toString(), rbd);
                mCaller.post(this);
            }
        }

        /**
         * This is to be called on UI thread once finished
         * @see #onResponse(Call, Response)
         */
        @Override
        public void run() {
            mCaller.setImageDrawable(retrieved);
        }
    }
}
