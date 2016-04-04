package com.dao.mydebts.adapters;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import com.dao.mydebts.R;

import java.io.FileDescriptor;
import java.io.IOException;

/**
 * Adapter holding contact cards
 *
 * @author Alexander Smolko
 */
public class AccountsAdapter extends RecyclerView.Adapter<AccountsAdapter.AccViewHolder> {

    private final Cursor mCursor;
    private final Context mContext;

    public AccountsAdapter(Context context, Cursor cursor) {
        this.mContext = context;
        this.mCursor = cursor;
    }

    @Override
    public AccViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        return new AccViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(AccViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        holder.textView.setText(mCursor.getString(mCursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME_PRIMARY)));
        holder.quickContactBadge.setImageBitmap(loadContactPhotoThumbnail(mCursor));
    }

    private Bitmap loadContactPhotoThumbnail(Cursor cursor) {
        String string = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI));
        if (string == null) {
            return null; //TODO make bitmap from name maybe
        }
        Uri thumbUri = Uri.parse(string);
        try (AssetFileDescriptor afd = mContext.getContentResolver().openAssetFileDescriptor(thumbUri, "r")) {
            if (afd == null) {
                return null;
            }
            FileDescriptor fileDescriptor = afd.getFileDescriptor();
            if (fileDescriptor == null) {
                return null;
            }
            return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    class AccViewHolder extends RecyclerView.ViewHolder {
        public QuickContactBadge quickContactBadge;
        public TextView textView;

        public AccViewHolder(View view) {
            super(view);
            CardView cardView = (CardView) view.findViewById(R.id.card_view);
            cardView.setRadius(15);
            textView = (TextView) view.findViewById(R.id.item_text);
            quickContactBadge = (QuickContactBadge) view.findViewById(R.id.quickbadge);
        }
    }
}
