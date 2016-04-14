package com.dao.mydebts;

import android.accounts.AccountManager;
import android.animation.Animator;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.dao.mydebts.adapters.AccountsAdapter;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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

    private static final int MSG_CIRCLES_LOADED = 0;
    private static final int MSG_DEBTS_LOADED   = 1;

    // GUI-related
    private RecyclerView mGroupList;
    private ProgressBar mProgress;
    private FloatingActionButton mFloatingButton;

    private CardView mDebtAddForm;
    private EditText mDebtAddPersonName;
    private RecyclerView mDebtPersonList;

    // Network-related
    private OkHttpClient mHttpClient = new OkHttpClient();
    private Gson mJsonSerializer = new Gson();
    private GoogleApiClient mGoogleApiClient;

    // Custom
    /**
     * Contact list retrieved from Google+ circles
     */
    private List<com.google.android.gms.plus.model.people.Person> mContacts = new ArrayList<>();

    /**
     * Runs on UI thread and delivers custom notifications as requested by background threads
     */
    private Handler mUiHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups_list);

        mGroupList = (RecyclerView) findViewById(R.id.list);
        mGroupList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mProgress = (ProgressBar) findViewById(R.id.loader);
        mFloatingButton = (FloatingActionButton) findViewById(R.id.floating_add_button);
        mFloatingButton.setOnClickListener(new FabClickListener());
        mFloatingButton.hide();

        mDebtAddForm = (CardView) findViewById(R.id.debt_create_form);
        mDebtAddPersonName = (EditText) findViewById(R.id.debt_create_search_edit);
        mDebtPersonList = (RecyclerView) findViewById(R.id.debt_create_contact_list);
        mDebtPersonList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        mUiHandler = new Handler(new UiCallback());

        if (AccountHolder.isAccountNameSaved(this)) {
            requestVisiblePeople(AccountHolder.getSavedAccountName(this));
        } else {
            pickAccount();
        }

        // this will start debt list retrieval immediately after activity is in `started` state
        getLoaderManager().initLoader(Constants.DEBT_REQUEST_LOADER, null, new LoadDebtsCallback());
    }

    @Override
    protected void onStart() {
        super.onStart();
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
        public void onResult(@NonNull People.LoadPeopleResult loadPeopleResult) {
            try {
                if (loadPeopleResult.getStatus().isSuccess()) {
                    for (com.google.android.gms.plus.model.people.Person person : loadPeopleResult.getPersonBuffer()) {
                        mContacts.add(person);
                    }

                    if (!TextUtils.isEmpty(loadPeopleResult.getNextPageToken())) {
                        Plus.PeopleApi.loadVisible(mGoogleApiClient, loadPeopleResult.getNextPageToken())
                                .setResultCallback(this);
                        return;
                    }

                    mUiHandler.sendEmptyMessage(MSG_CIRCLES_LOADED);
                    return;
                }

                Toast.makeText(DebtsListActivity.this, R.string.cannot_load_circles, Toast.LENGTH_SHORT).show();
            } finally {
                //loadPeopleResult.release();
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

    /**
     * Callback for notifications that runs on UI thread
     */
    private class UiCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CIRCLES_LOADED:
                    mProgress.animate().alpha(0.0f).setDuration(500).start();
                    supportInvalidateOptionsMenu();
                    mFloatingButton.show();

                    mDebtPersonList.setAdapter(new AccountsAdapter(DebtsListActivity.this, mContacts));
                    return true;
            }

            return false;
        }
    }

    private class FabClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if(mDebtAddForm.getVisibility() == View.INVISIBLE) {
                mFloatingButton.animate().rotation(45f).setDuration(300).start();
                mDebtAddForm.setVisibility(View.VISIBLE);
                Animator cr =  ViewAnimationUtils.createCircularReveal(mDebtAddForm,
                        mDebtAddForm.getBottom(),
                        mDebtAddForm.getRight(),
                        0,
                        Math.max(mDebtAddForm.getWidth(), mDebtAddForm.getHeight()) * 2);

                cr.start();
            } else {
                mFloatingButton.animate().rotation(0f).setDuration(300).start();
                Animator cr =  ViewAnimationUtils.createCircularReveal(mDebtAddForm,
                        mDebtAddForm.getBottom(),
                        mDebtAddForm.getRight(),
                        Math.max(mDebtAddForm.getWidth(), mDebtAddForm.getHeight()) * 2,
                        0);
                cr.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mDebtAddForm.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                cr.start();
            }
        }
    }
}
