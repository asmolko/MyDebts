package com.dao.mydebts.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.DrawableUtils;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dao.mydebts.Constants;
import com.dao.mydebts.R;
import com.google.android.gms.plus.model.people.Person;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Adapter holding contact cards.
 * Supports async loading of account profile images.
 *
 * @author Alexander Smolko, Oleg Chernovskiy
 */
public class AccountsAdapter extends RecyclerView.Adapter<AccountsAdapter.AccViewHolder> {

    private static final String AA_TAG = AccountsAdapter.class.getSimpleName();

    private final List<Person> mContacts;
    private final Activity mActivity;

    private final ContactImageRetriever mImageLoader = new ContactImageRetriever();

    public AccountsAdapter(Activity context, List<Person> contacts) {
        this.mActivity = context;
        this.mContacts = contacts;
    }

    @Override
    public AccViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.contact_list_item, parent, false);
        return new AccViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(AccViewHolder holder, int position) {
        Person it = mContacts.get(position);
        holder.name.setText("");
        holder.badge.setImageDrawable(null);

        if(it.hasDisplayName()) {
            holder.name.setText(it.getDisplayName());
        }

        if(it.hasImage() && it.getImage().hasUrl() && !TextUtils.isEmpty(it.getImage().getUrl())) {
            // the fact that person has url does not mean it is non-null, tricked you! ;)
            mImageLoader.loadImage(it.getImage().getUrl(), new BadgeCallback(holder.badge));
        }
    }

    @Override
    public int getItemCount() {
        return mContacts.size();
    }

    class AccViewHolder extends RecyclerView.ViewHolder {
        public ImageView badge;
        public TextView name;

        public AccViewHolder(View view) {
            super(view);
            RelativeLayout rl = (RelativeLayout) view.findViewById(R.id.contact_item_root);
            name = (TextView) rl.findViewById(R.id.contact_name);
            badge = (ImageView) rl.findViewById(R.id.contact_img);
        }
    }

    /**
     * Async loader for person images.
     * Supports caching for better performance.
     */
    private class ContactImageRetriever {

        private final OkHttpClient client = new OkHttpClient();
        private final LruCache<String, Drawable> cache = new LruCache<>(30);

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
            Drawable cached = cache.get(url);
            if(cached != null) {
                callback.setRetrieved(cached);
                callback.run();
            }

            Request req = new Request.Builder().url(url).build();
            client.newCall(req).enqueue(callback);
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
            Log.w(AA_TAG, "Couldn't retrieve contact avatar", e);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            if(response.isSuccessful()) {
                Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
                RoundedBitmapDrawable rbd = RoundedBitmapDrawableFactory.create(mActivity.getResources(), bitmap);
                rbd.setCornerRadius(Math.max(bitmap.getWidth(), bitmap.getHeight()) / 2.0f);
                setRetrieved(rbd);

                mImageLoader.cache.put(call.request().url().toString(), rbd);
                mActivity.runOnUiThread(this);
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
