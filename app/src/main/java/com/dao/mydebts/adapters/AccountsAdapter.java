package com.dao.mydebts.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dao.mydebts.DebtsListActivity;
import com.dao.mydebts.R;
import com.dao.mydebts.entities.Contact;
import com.dao.mydebts.entities.Debt;
import com.dao.mydebts.misc.ImageCache;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Adapter holding contact cards.
 * Supports async loading of account profile images.
 *
 * @author Alexander Smolko, Oleg Chernovskiy
 */
public class AccountsAdapter extends RecyclerView.Adapter<AccountsAdapter.AccViewHolder> {

    private static final String AA_TAG = AccountsAdapter.class.getSimpleName();

    private final List<Contact> mContacts;
    private final Handler mObserver;

    private final ContactImageRetriever mImageLoader = new ContactImageRetriever();
    private final Context mContext;

    /**
     * Constructs new AccountsAdapter.
     * The adapter itself cannot modify arguments provided to it.
     *
     * @param context the Context
     * @param handler handler to send back notifications about click events
     * @param contacts contacts list to bould adapter items from
     */
    public AccountsAdapter(Context context, Handler handler, List<Contact> contacts) {
        this.mContacts = Collections.unmodifiableList(contacts);
        this.mObserver = handler;
        this.mContext = context;
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
        Contact it = mContacts.get(position);
        holder.name.setText(it.getDisplayName());
        if(!TextUtils.isEmpty(it.getImageUrl())) {
            mImageLoader.loadImage(it.getImageUrl(), new BadgeCallback(holder.badge));
        }
    }

    @Override
    public void onViewRecycled(AccViewHolder holder) {
        holder.name.setText("");
        holder.badge.setImageDrawable(null);
    }

    @Override
    public int getItemCount() {
        return mContacts.size();
    }

    class AccViewHolder extends RecyclerView.ViewHolder {
        private final ImageView badge;
        private final TextView name;

        public AccViewHolder(View view) {
            super(view);
            RelativeLayout rl = (RelativeLayout) view.findViewById(R.id.contact_item_root);
            name = (TextView) rl.findViewById(R.id.contact_name);
            badge = (ImageView) rl.findViewById(R.id.contact_img);

            view.setOnClickListener(new CreateDebtHandler());
        }

        private class CreateDebtHandler implements View.OnClickListener {
            @Override
            public void onClick(View v) {
                final EditText input = new EditText(v.getContext());
                final Contact who = mContacts.get(getLayoutPosition());
                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

                new AlertDialog.Builder(v.getContext())
                        .setView(input)
                        .setTitle(who.getDisplayName())
                        .setMessage(R.string.how_bad_is_it)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                String amount = input.getText().toString();
                                if(amount.isEmpty()) {
                                    return;
                                }

                                // convert to app entities
                                Debt toCreate = new Debt();
                                toCreate.setTo(who.toActor());
                                toCreate.setAmount(new BigDecimal(amount));
                                toCreate.setCreated(Calendar.getInstance());

                                // send back to activity
                                Message msg = Message.obtain(mObserver, DebtsListActivity.MSG_CREATE_DEBT, toCreate);
                                mObserver.sendMessage(msg);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create().show();

            }
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
            Drawable cached = ImageCache.getInstance(mContext).get(url);
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
            Log.w(AA_TAG, "Couldn't retrieve contact avatar", e);
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

                ImageCache.getInstance(null).put(call.request().url().toString(), rbd);
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
