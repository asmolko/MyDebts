package com.dao.mydebts;

import android.accounts.AccountManager;
import android.animation.Animator;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.view.View.OnClickListener;
import android.view.ViewAnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dao.mydebts.adapters.AccountsAdapter;
import com.dao.mydebts.adapters.DebtsAdapter;
import com.dao.mydebts.dto.DebtCreationRequest;
import com.dao.mydebts.dto.DebtsRequest;
import com.dao.mydebts.dto.DebtsResponse;
import com.dao.mydebts.dto.GenericResponse;
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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.orm.SugarRecord;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
    public static final int MSG_DEBTS_LOADED = 1;
    public static final int MSG_CREATE_DEBT = 2;
    public static final int MSG_DEBT_CREATED = 3;

    // GUI-related
    private RecyclerView mDebtList;
    private ProgressBar mProgress;
    private FloatingActionButton mFloatingButton;

    private CardView mDebtAddForm;
    private CardView mDebtApproveForm;
    private EditText mDebtAddPersonName;
    private RecyclerView mDebtPersonList;

    // Network-related
    private OkHttpClient mHttpClient = new OkHttpClient();
    private Gson mJsonSerializer = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'hh:mm:ssZ") // ISO 8601
            .create();
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
     *
     * @see #onCreate(Bundle)
     */
    private Handler mBackgroundHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups_list);

        mDebtList = (RecyclerView) findViewById(R.id.list);
        mDebtList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mDebtApproveForm = (CardView) findViewById(R.id.debt_approve_form);
        DebtClickListener debtClickListener = new DebtClickListener(mDebtApproveForm);
        DebtsAdapter adapter = new DebtsAdapter(this, mDebts, mContacts, debtClickListener);
        mDebtList.setAdapter(adapter);

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

        if (AccountHolder.isAccountNameSaved(this)) { // circles were already loaded
            String ourGoogleId = AccountHolder.getSavedGoogleId(this);
            List<Contact> forAdapter = SugarRecord.listAll(Contact.class);
            ListIterator<Contact> itr = forAdapter.listIterator();

            // remove us from list, also populate contacts map
            while (itr.hasNext()) {
                Contact c = itr.next();
                mContacts.put(c.getGoogleId(), c);
                if (c.getGoogleId().equals(ourGoogleId)) {
                    mCurrentPerson = c;
                    itr.remove();
                }
            }

            mDebtPersonList.setAdapter(new AccountsAdapter(DebtsListActivity.this, mBackgroundHandler, forAdapter));
            mProgress.animate().alpha(0.0f).setDuration(0).start();
            supportInvalidateOptionsMenu();
            mFloatingButton.show();

//            todo move to settings or some other manual action
//            requestVisiblePeople(AccountHolder.getSavedAccountName(this));
        } else {
            pickAccount();
        }

        getLoaderManager().initLoader(Constants.DEBT_REQUEST_LOADER, null, new LoadDebtsCallback());
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
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class LoadDebtsCallback implements LoaderManager.LoaderCallbacks<List<Debt>> {
        @Override
        public Loader<List<Debt>> onCreateLoader(int i, Bundle args) {
            return new AbstractNetworkLoader<List<Debt>>(DebtsListActivity.this) {

                @Override
                protected void onStartLoading() {
                    if (mCurrentPerson != null) {
                        super.onStartLoading();
                    }
                }

                @NonNull
                @Override
                public List<Debt> loadInBackground() {
                    try {
                        // there may be some additional fields here, for now it only sets person
                        DebtsRequest postData = new DebtsRequest();
                        postData.setMe(mCurrentPerson.toActor());

                        DebtsResponse dr = postServerRoundtrip(Constants.SERVER_ENDPOINT_DEBTS, postData, DebtsResponse.class);
                        if (dr != null && dr.getMe().getId().equals(mCurrentPerson.getGoogleId())) {
                            return dr.getDebts();
                        }
                    } catch (JsonSyntaxException e) {
                        Log.e(DLA_TAG, "Answer could not be deserialized to correct response class", e);
                    }

                    return Collections.emptyList();
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<List<Debt>> objectLoader, @NonNull List<Debt> results) {
            mDebts.clear();
            mDebts.addAll(results);

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
            GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnected(Bundle bundle) {
            // Docs suggest to use SignIn API for this, but we can't, as we use Plus
            // noinspection deprecation
            mCurrentPerson = new Contact(Plus.PeopleApi.getCurrentPerson(mGoogleApiClient));
            AccountHolder.saveGoogleId(DebtsListActivity.this, mCurrentPerson.getGoogleId());

            // Meanwhile, preload circles in background
            Plus.PeopleApi.loadVisible(mGoogleApiClient, null).setResultCallback(this);

            // update visible debts
            getLoaderManager().getLoader(Constants.DEBT_REQUEST_LOADER).onContentChanged();
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
        public void onConnectionFailed(@NonNull ConnectionResult fail) {
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

                    // cache
                    List<Contact> forAdapter = new ArrayList<>(mContacts.values());
                    SugarRecord.deleteAll(Contact.class);
                    SugarRecord.saveInTx(forAdapter); // save our circles
                    SugarRecord.save(mCurrentPerson); // save us

                    mDebtPersonList.setAdapter(new AccountsAdapter(DebtsListActivity.this, mBackgroundHandler, forAdapter));
                    return true;
                case MSG_DEBT_CREATED:
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
                    toCreate.setSrc(mCurrentPerson.toActor());

                    // enclose it in request
                    DebtCreationRequest dcr = new DebtCreationRequest();
                    dcr.setCreated(toCreate);

                    GenericResponse gr = postServerRoundtrip(Constants.SERVER_ENDPOINT_CREATE, dcr, GenericResponse.class);
                    if(gr != null && TextUtils.isDigitsOnly(gr.getResult())) {
                        toCreate.setId(gr.getResult());
                        Message toUi = Message.obtain(msg);
                        toUi.what = MSG_DEBT_CREATED;
                        mUiHandler.sendMessage(toUi);
                        return true;
                    }
            }

            return false;
        }
    }

    @Nullable
    private <REQ, RESP> RESP postServerRoundtrip(String endpoint, REQ request, Class<RESP> respClass) {
        Request postQuery = new Request.Builder()
                .url(endpoint)
                .post(RequestBody.create(Constants.JSON_MIME_TYPE, mJsonSerializer.toJson(request)))
                .build();

        // send request across the network
        try {
            Response answer = mHttpClient.newCall(postQuery).execute();
            if (answer.isSuccessful()) {
                return mJsonSerializer.fromJson(answer.body().string(), respClass);
            }
        } catch (IOException e) {
            Log.e(DLA_TAG, "Server sync failed, object: " + request, e);
        }

        // if we fall this far then something is wrong
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), R.string.server_sync_failed, Toast.LENGTH_SHORT).show();
            }
        });
        return null;
    }

    private class FabClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (mDebtAddForm.getVisibility() == View.INVISIBLE) {
                mFloatingButton.animate().rotation(45f).setDuration(300).start();
                mDebtAddForm.setVisibility(View.VISIBLE);
                Animator cr = ViewAnimationUtils.createCircularReveal(mDebtAddForm,
                        mDebtAddForm.getBottom(),
                        mDebtAddForm.getRight(),
                        0,
                        Math.max(mDebtAddForm.getWidth(), mDebtAddForm.getHeight()) * 2);

                cr.start();
            } else {
                mFloatingButton.animate().rotation(0f).setDuration(300).start();
                Animator cr = ViewAnimationUtils.createCircularReveal(mDebtAddForm,
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

    public class DebtClickListener implements OnClickListener {
        private final CardView mDebtApproveForm;
        private final TextView mContactNameView;
        private final ImageView mContactImageView;
        private final TextView mAmountTextView;

        public DebtClickListener(CardView mDebtApproveForm) {
            this.mDebtApproveForm = mDebtApproveForm;
            mContactImageView = (ImageView) mDebtApproveForm.findViewById(R.id.contact_img);
            mContactNameView = (TextView) mDebtApproveForm.findViewById(R.id.contact_name);
            mAmountTextView = (TextView) mDebtApproveForm.findViewById(R.id.amount_text);
        }

        @Override
        public void onClick(View v) {
            if (mDebtApproveForm.getVisibility() == View.INVISIBLE) {
//                ImageView badge = (ImageView) v.findViewById(R.id.debt_item_contact_img);
                TextView name = (TextView) v.findViewById(R.id.debt_item_contact_name);
//                mContactImageView.setImageDrawable(badge.getDrawable());
                mContactImageView.setImageDrawable(new ColorDrawable(Color.BLACK));
                mContactNameView.setText(name.getText());

                mDebtApproveForm.setVisibility(View.VISIBLE);
            } else {
                mDebtApproveForm.setVisibility(View.INVISIBLE);
            }
        }
    }
}
