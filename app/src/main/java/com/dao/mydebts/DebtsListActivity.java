package com.dao.mydebts;

import android.accounts.AccountManager;
import android.animation.Animator;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
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
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.dao.mydebts.adapters.AccountsAdapter;
import com.dao.mydebts.adapters.DebtsAdapter;
import com.dao.mydebts.dto.DebtCreationRequest;
import com.dao.mydebts.dto.DebtsRequest;
import com.dao.mydebts.dto.DebtsResponse;
import com.dao.mydebts.entities.Contact;
import com.dao.mydebts.entities.Debt;
import com.dao.mydebts.misc.AbstractNetworkLoader;
import com.dao.mydebts.misc.AccountHolder;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.google.android.gms.common.GooglePlayServicesUtil.showErrorDialogFragment;

public class DebtsListActivity extends AppCompatActivity {

    private static final String DLA_TAG = DebtsListActivity.class.getSimpleName();

    private static final int SIGN_IN_RETURN_CODE = 1050;

    public static final int MSG_CIRCLES_LOADED = 0;
    public static final int MSG_DEBTS_LOADED   = 1;
    public static final int MSG_CREATE_DEBT    = 2;
    public static final int MSG_DEBT_CREATED   = 3;

    // GUI-related
    private RecyclerView mDebtList;
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
     * Contact map retrieved and cached from Google+ circles
     * Keys are ids, values are Contact objects
     */
    private final Map<String, Contact> mContacts = new HashMap<>();
    private Contact mCurrentPerson;

    /**
     * Debts retrieved and cached from server
     */
    private final List<Debt> mDebts = new ArrayList<>();

    /**
     * Runs on UI thread and delivers custom notifications as requested by background threads
     */
    private Handler mUiHandler;
    /**
     * Runs on background thread
     * @see #onCreate(Bundle)
     */
    private Handler mBackgroundHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups_list);

        mDebtList = (RecyclerView) findViewById(R.id.list);
        mDebtList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mDebtList.setAdapter(new DebtsAdapter(mDebts, mContacts));

        mProgress = (ProgressBar) findViewById(R.id.loader);
        mFloatingButton = (FloatingActionButton) findViewById(R.id.floating_add_button);
        mFloatingButton.setOnClickListener(new FabClickListener());
        mFloatingButton.hide();

        mDebtAddForm = (CardView) findViewById(R.id.debt_create_form);
        mDebtAddPersonName = (EditText) findViewById(R.id.debt_create_search_edit);
        mDebtPersonList = (RecyclerView) findViewById(R.id.debt_create_contact_list);
        mDebtPersonList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        HandlerThread thr = new HandlerThread("HttpBounce");
        thr.start();
        mBackgroundHandler = new Handler(thr.getLooper(), new BackgroundCallback());
        mUiHandler = new Handler(new UiCallback());

        if (AccountHolder.isAccountNameSaved(this)) {
            requestVisiblePeople(AccountHolder.getSavedAccountName(this));
        } else {
            pickAccount();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBackgroundHandler.getLooper().quitSafely();
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
                        postData.setMe(mCurrentPerson.toActor());

                        Request postQuery = new Request.Builder()
                                .url(Constants.DEFAULT_SERVER_URL)
                                .header("X-DTO-Class", postData.getClass().toString())
                                .post(RequestBody.create(Constants.JSON_MIME_TYPE, mJsonSerializer.toJson(postData)))
                                .build();

                        // send request across the network
                        Response response = mHttpClient.newCall(postQuery).execute();
                        if (response.isSuccessful()) {
                            String result = response.body().string();
                            try {
                                DebtsResponse answer = mJsonSerializer.fromJson(result, DebtsResponse.class);
                                if (answer != null && answer.getMe().getId().equals(mCurrentPerson.getId())) {
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
            mDebtList.getAdapter().notifyDataSetChanged();
        }

        @Override
        public void onLoaderReset(Loader<List<Debt>> objectLoader) {
            mDebtList.setAdapter(null);
        }
    }

    /**
     * Loads Plus Persons from your circles at the activity start.
     */
    private class PlusApiAsyncListener implements GoogleApiClient.ConnectionCallbacks,
            ResultCallback<People.LoadPeopleResult>,
            GoogleApiClient.OnConnectionFailedListener
    {
        @Override
        public void onConnected(Bundle bundle) {
            // Docs suggest to use SignIn API for this, but we can't, as we use Plus
            // noinspection deprecation
            mCurrentPerson = new Contact(Plus.PeopleApi.getCurrentPerson(mGoogleApiClient));

            // Now we have current person account, it's safe to load debts to main window
            getLoaderManager().initLoader(Constants.DEBT_REQUEST_LOADER, null, new LoadDebtsCallback());

            // Meanwhile, preload circles in background
            Plus.PeopleApi.loadVisible(mGoogleApiClient, null).setResultCallback(this);
        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onResult(@NonNull People.LoadPeopleResult loadPeopleResult) {
            try {
                if (loadPeopleResult.getStatus().isSuccess()) {
                    for (Person person : loadPeopleResult.getPersonBuffer()) {
                        mContacts.put(person.getId(), new Contact(person));
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
                loadPeopleResult.release();
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

                    List<Contact> forAdapter = new ArrayList<>(mContacts.values());
                    mDebtPersonList.setAdapter(new AccountsAdapter(mBackgroundHandler, forAdapter));
                    return true;
                case MSG_DEBT_CREATED:
                    //getLoaderManager().getLoader(Constants.DEBT_REQUEST_LOADER).onContentChanged();
                    mDebts.add(0, (Debt) msg.obj);
                    mDebtList.getAdapter().notifyItemInserted(0);

                    // hide FAB
                    mFloatingButton.callOnClick();
                    return true;
            }

            return false;
        }
    }

    /**
     * Callback for notifications that runs on background thread
     */
    private class BackgroundCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CREATE_DEBT:
                    Debt toCreate = (Debt) msg.obj;
                    toCreate.setFrom(mCurrentPerson.toActor());

                    // enclose it in request
                    DebtCreationRequest dcr = new DebtCreationRequest();
                    dcr.setCreated(toCreate);

                    // TODO: now send it to the server and comment this out
                    Message toUi = Message.obtain(msg);
                    toUi.what = MSG_DEBT_CREATED;
                    mUiHandler.sendMessage(toUi);
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
