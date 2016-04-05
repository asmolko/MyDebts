package com.dao.mydebts;

import android.annotation.SuppressLint;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.QuickContactBadge;
import android.widget.TextView;


import com.dao.mydebts.adapters.AccountsAdapter;
import com.dao.mydebts.dto.DebtsRequest;
import com.dao.mydebts.dto.DebtsResponse;
import com.dao.mydebts.entities.Debt;
import com.dao.mydebts.entities.Person;
import com.dao.mydebts.misc.AbstractNetworkLoader;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.provider.ContactsContract.Contacts;
import static android.provider.ContactsContract.Data;

public class DebtsListActivity extends AppCompatActivity {

    @SuppressLint("InlinedApi")
    private static final String[] ACCOUNTS_PROJECTION = {Data._ID, Data.DISPLAY_NAME_PRIMARY,
            Data.LOOKUP_KEY, Contacts.PHOTO_THUMBNAIL_URI, Contacts.PHOTO_URI};

    private static final String DLA_TAG = DebtsListActivity.class.getSimpleName();

    private RecyclerView mGroupList;
    private OkHttpClient mHttpClient = new OkHttpClient();
    private Gson mJsonSerializer = new Gson();

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
        final Cursor cursor = contentResolver.query(Contacts.CONTENT_URI, ACCOUNTS_PROJECTION, null, null, null);
        if (cursor == null) {
            return;
        }

        mGroupList.setAdapter(new AccountsAdapter(this, cursor));

        // this will start debt list retrieval immediately after activity is in `started` state
        getLoaderManager().initLoader(Constants.DEBT_REQUEST_LOADER, null, new LoadDebtsCallback());
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

    private class LoadDebtsCallback implements LoaderManager.LoaderCallbacks<List<Debt>> {
        @Override
        public Loader<List<Debt>> onCreateLoader(int i, Bundle args) {
            return new AbstractNetworkLoader<List<Debt>>(DebtsListActivity.this) {

                @Nullable
                @Override
                public List<Debt> loadInBackground() {
                    try {
                        // there may be some additional fields here, for now it only sets person
                        DebtsRequest postData = new DebtsRequest();
                        Person me = new Person();
                        me.setId(UUID.randomUUID().toString());
                        postData.setMe(me);

                        Request postQuery = new Request.Builder()
                                .url(Constants.DEFAULT_SERVER_URL)
                                .post(RequestBody.create(Constants.JSON_MIME_TYPE, mJsonSerializer.toJson(postData)))
                                .build();

                        // send request across the network
                        Response response = mHttpClient.newCall(postQuery).execute();
                        if (response.isSuccessful()) {
                            String result = response.body().string();
                            DebtsResponse answer = mJsonSerializer.fromJson(result, DebtsResponse.class);
                            if(answer != null && answer.getMe().getId().equals(me.getId())) {
                                return answer.getDebts();
                            }
                        }
                    } catch (IOException e) {
                        Log.e(DLA_TAG, "Couldn't request a debts list", e);
                    }

                    return Collections.emptyList();
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
