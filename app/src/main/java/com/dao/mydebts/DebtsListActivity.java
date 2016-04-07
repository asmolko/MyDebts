package com.dao.mydebts;

import android.accounts.AccountManager;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.dao.mydebts.adapters.DebtsAdapter;
import com.dao.mydebts.dto.DebtsRequest;
import com.dao.mydebts.dto.DebtsResponse;
import com.dao.mydebts.entities.Debt;
import com.dao.mydebts.entities.Person;
import com.dao.mydebts.misc.AbstractNetworkLoader;
import com.dao.mydebts.misc.AccountHolder;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.Plus;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.google.android.gms.common.GooglePlayServicesUtil.showErrorDialogFragment;

public class DebtsListActivity extends AppCompatActivity {

    private static final String DLA_TAG = DebtsListActivity.class.getSimpleName();

    private static final int SIGN_IN_RETURN_CODE = 1050;

    private RecyclerView mGroupList;
    private OkHttpClient mHttpClient = new OkHttpClient();
    private Gson mJsonSerializer = new Gson();
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups_list);

        if (AccountHolder.isAccountNameSaved(this)) {
            requestVisiblePeople(AccountHolder.getSavedAccountName(this));
        } else {
            pickAccount();
        }

        mGroupList = (RecyclerView) findViewById(R.id.list);
        mGroupList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        // this will start debt list retrieval immediately after activity is in `started` state
        getLoaderManager().initLoader(Constants.DEBT_REQUEST_LOADER, null, new LoadDebtsCallback());
    }

    //can also be used for account change
    private void pickAccount() {
        Intent intent = AccountPicker.newChooseAccountIntent(
                null, null, new String[]{"com.google"},
                false, null, null, null, null);
        startActivityForResult(intent, SIGN_IN_RETURN_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SIGN_IN_RETURN_CODE) {
            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            AccountHolder.saveAccountName(this, accountName);
            requestVisiblePeople(accountName);
        }
    }

    private void requestVisiblePeople(String accountName) {
        PlusApiAsyncListener listener = new PlusApiAsyncListener();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, listener)
                .addConnectionCallbacks(listener)
                .setAccountName(accountName)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .build();

        mGoogleApiClient.connect(GoogleApiClient.SIGN_IN_MODE_REQUIRED);
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
        switch (id) {
            case  R.id.action_settings:
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class LoadDebtsCallback implements LoaderManager.LoaderCallbacks<List<Debt>> {
        @Override
        public Loader<List<Debt>> onCreateLoader(int i, Bundle args) {
            return new AbstractNetworkLoader<List<Debt>>(DebtsListActivity.this) {

                @NonNull
                @Override
                public List<Debt> loadInBackground() {
                    try {
                        // there may be some additional fields here, for now it only sets person
                        DebtsRequest postData = new DebtsRequest();
                        Person me = new Person();
                        me.setId(UUID.randomUUID().toString()); // TODO
                        postData.setMe(me);

                        Request postQuery = new Request.Builder()
                                .url(Constants.DEFAULT_SERVER_URL)
                                .post(RequestBody.create(Constants.JSON_MIME_TYPE, mJsonSerializer.toJson(postData)))
                                .build();

                        // send request across the network
                        Response response = mHttpClient.newCall(postQuery).execute();
                        if (response.isSuccessful()) {
                            String result = response.body().string();
                            try {
                                DebtsResponse answer = mJsonSerializer.fromJson(result, DebtsResponse.class);
                                if (answer != null && answer.getMe().getId().equals(me.getId())) {
                                    return answer.getDebts();
                                }
                            } catch (JsonSyntaxException ignore) {
                            }
                        }
                    } catch (IOException e) {
                        Log.e(DLA_TAG, "Couldn't request a debts list", e);
                    } catch (JsonSyntaxException e) {
                        Log.e(DLA_TAG, "Answer could not be deserialized to correct response class", e);
                    }

                    return Collections.emptyList();
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<List<Debt>> objectLoader, @NonNull List<Debt> results) {
            mGroupList.setAdapter(new DebtsAdapter(results));
        }

        @Override
        public void onLoaderReset(Loader<List<Debt>> objectLoader) {
            mGroupList.setAdapter(null);
        }
    }

    private class PlusApiAsyncListener implements GoogleApiClient.ConnectionCallbacks,
            ResultCallback<People.LoadPeopleResult>,
            GoogleApiClient.OnConnectionFailedListener
    {
        @Override
        public void onConnected(Bundle bundle) {
            Plus.PeopleApi.loadVisible(mGoogleApiClient, null).setResultCallback(this);
        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onResult(People.LoadPeopleResult loadPeopleResult) {
            //todo pasmapipa
            if(loadPeopleResult.getStatus().isSuccess()) {
                loadPeopleResult.getPersonBuffer();
            }
        }

        @Override
        public void onConnectionFailed(ConnectionResult fail) {
            Log.e(DLA_TAG, String.format("Couldn't connect to Google API due to %s", fail.getErrorMessage()));
            if (!fail.hasResolution()) {
                showErrorDialogFragment(fail.getErrorCode(), DebtsListActivity.this, new DialogFragment(), 0, null);
            }
        }
    }
}
