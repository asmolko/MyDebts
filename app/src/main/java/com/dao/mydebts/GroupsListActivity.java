package com.dao.mydebts;

import android.annotation.SuppressLint;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Loader;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.QuickContactBadge;
import android.widget.TextView;


import com.dao.mydebts.entities.Debt;
import com.dao.mydebts.misc.AbstractNetworkLoader;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;

import static android.provider.ContactsContract.Contacts;
import static android.provider.ContactsContract.Data;

public class GroupsListActivity extends AppCompatActivity {

    @SuppressLint("InlinedApi")
    private static final String[] PROJECTION = {Data._ID, Data.DISPLAY_NAME_PRIMARY,
            Data.LOOKUP_KEY, Contacts.PHOTO_THUMBNAIL_URI, Contacts.PHOTO_URI};

    private static final int DEBT_REQUEST_LOADER = 0;

    private RecyclerView mGroupList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups_list);
        mGroupList = (RecyclerView) findViewById(R.id.list);
        if (mGroupList == null) {
            return;
        }

        RecyclerView.LayoutManager layout = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mGroupList.setLayoutManager(layout);
        ContentResolver contentResolver = getContentResolver();
        final Cursor cursor = contentResolver.query(Contacts.CONTENT_URI, PROJECTION, null, null, null);
        if (cursor == null) {
            return;
        }

        mGroupList.setAdapter(new RecyclerView.Adapter<ViewHolder>() {

            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View itemView = LayoutInflater
                        .from(parent.getContext())
                        .inflate(R.layout.list_item, parent, false);
                return new ViewHolder(itemView);
            }

            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                cursor.moveToPosition(position);
                holder.textView.setText(cursor.getString(cursor.getColumnIndex(Data.DISPLAY_NAME_PRIMARY)));
                holder.quickContactBadge.setImageBitmap(loadContactPhotoThumbnail(cursor));
            }

            @Override
            public int getItemCount() {
                return cursor.getCount();
            }
        });

        getLoaderManager().initLoader(GroupsListActivity.DEBT_REQUEST_LOADER, null, new LoadDebtsCallback());
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        public QuickContactBadge quickContactBadge;
        public TextView textView;

        public ViewHolder(View view) {
            super(view);
            CardView cardView = (CardView) view.findViewById(R.id.card_view);
            cardView.setRadius(15);
            textView = (TextView) view.findViewById(R.id.item_text);
            quickContactBadge = (QuickContactBadge) view.findViewById(R.id.quickbadge);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_groups_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Bitmap loadContactPhotoThumbnail(Cursor cursor) {
        String string = cursor.getString(cursor.getColumnIndex(Contacts.PHOTO_URI));
        if(string == null){
            return null;//TODO make bitmap from name maybe
        }
        Uri thumbUri = Uri.parse(string);
        try (AssetFileDescriptor afd = this.getContentResolver().openAssetFileDescriptor(thumbUri, "r")) {
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

    private class LoadDebtsCallback implements LoaderManager.LoaderCallbacks<List<Debt>> {
        @Override
        public Loader<List<Debt>> onCreateLoader(int i, Bundle args) {
            return new AbstractNetworkLoader<List<Debt>>(GroupsListActivity.this) {

                @Nullable
                @Override
                public List<Debt> loadInBackground() {


                    return null;
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<List<Debt>> objectLoader, List<Debt> results) {
            if(results != null) {
                mGroupList.setAdapter(null);
            }
        }

        @Override
        public void onLoaderReset(Loader<List<Debt>> objectLoader) {

        }
    }
}
