package com.dao.mydebts;

import android.accounts.AccountManager;
import android.animation.Animator;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.ProgressBar;
import android.widget.Toast;

import com.dao.mydebts.adapters.AccountsAdapter;
import com.dao.mydebts.adapters.DebtsAdapter;
import com.dao.mydebts.dto.DebtApprovalRequest;
import com.dao.mydebts.dto.DebtCreationRequest;
import com.dao.mydebts.dto.DebtDeleteRequest;
import com.dao.mydebts.dto.DebtsRequest;
import com.dao.mydebts.dto.DebtsResponse;
import com.dao.mydebts.dto.GenericResponse;
import com.dao.mydebts.entities.Contact;
import com.dao.mydebts.entities.Debt;
import com.dao.mydebts.misc.AbstractNetworkLoader;
import com.dao.mydebts.misc.AccountHolder;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.dao.mydebts.Constants.*;
import static com.google.android.gms.common.GooglePlayServicesUtil.showErrorDialogFragment;

public class DebtsListActivity extends AppCompatActivity {

    private static final String DLA_TAG = DebtsListActivity.class.getSimpleName();

    private static final int SIGN_IN_RETURN_CODE = 1050;

    public static final int MSG_CIRCLES_LOADED   = 0;
    public static final int MSG_DEBTS_LOADED     = 1;
    public static final int MSG_CREATE_DEBT      = 2;
    public static final int MSG_DEBT_CREATED     = 3;
    public static final int MSG_APPROVE_DEBT     = 4;
    public static final int MSG_DEBT_APPROVED    = 5;
    public static final int MSG_DELETE_DEBT      = 6;
    public static final int MSG_DEBT_DELETED     = 7;

    // GUI-related
    private RecyclerView mDebtList;
    private ProgressBar mProgress;
    private FloatingActionMenu mFloatingMenu;
    private FloatingActionButton mFloatingGetButton;
    private FloatingActionButton mFloatingGiveButton;

    private CardView mDebtAddForm;
    private RecyclerView mDebtPersonList;

    // Network-related
    private OkHttpClient mHttpClient = new OkHttpClient();
    private Gson mJsonSerializer = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'hh:mm:ssZ") // ISO 8601
            .create();
    private GoogleApiClient mGoogleApiClient;

    // Preferences-related
    /**
     * Should be strong link - see {@link SharedPreferences#registerOnSharedPreferenceChangeListener}
     */
    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPrefListener = new PrefChangeListener();
    /**
     * Currently active server sync endpoint
     */
    private String mServerEndpoint;

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
        setContentView(R.layout.activity_debt_list);

        // preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(mSharedPrefListener);
        mServerEndpoint = prefs.getString(PREF_SERVER_ADDRESS, DEFAULT_SERVER_ENDPOINT);

        // views
        mDebtList = (RecyclerView) findViewById(R.id.list);
        mDebtList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        DebtsAdapter adapter = new DebtsAdapter(this, mDebts, mContacts);
        mDebtList.setAdapter(adapter);

        mProgress = (ProgressBar) findViewById(R.id.loader);

        mFloatingMenu = (FloatingActionMenu) findViewById(R.id.floating_menu);
        mFloatingMenu.setOnMenuToggleListener(new FamClickListener());
        mFloatingGetButton = (FloatingActionButton) findViewById(R.id.floating_get_button);
        mFloatingGetButton.setOnClickListener(new FabClickListener(false));
        mFloatingGiveButton = (FloatingActionButton) findViewById(R.id.floating_give_button);
        mFloatingGiveButton.setOnClickListener(new FabClickListener(true));

        mDebtAddForm = (CardView) findViewById(R.id.debt_create_form);
        mDebtPersonList = (RecyclerView) findViewById(R.id.debt_create_contact_list);
        mDebtPersonList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        // background tasks
        HandlerThread thr = new HandlerThread("HttpBounce");
        thr.start();
        mBackgroundHandler = new Handler(thr.getLooper(), new BackgroundCallback());
        mUiHandler = new Handler(new UiCallback());

        // startup init
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

            mDebtPersonList.setAdapter(new AccountsAdapter(this, forAdapter));
            mProgress.animate().alpha(0.0f).setDuration(0).start();
            supportInvalidateOptionsMenu();

//            todo move to settings or some other manual action
//            requestVisiblePeople(AccountHolder.getSavedAccountName(this));
        } else {
            pickAccount();
        }

        getLoaderManager().initLoader(DEBT_REQUEST_LOADER, null, new LoadDebtsCallback());
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
        getMenuInflater().inflate(R.menu.menu_debt_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_refresh_debts:
                getLoaderManager().getLoader(DEBT_REQUEST_LOADER).onContentChanged();
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, DebtsPrefActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    public Handler getBackgroundHandler() {
        return mBackgroundHandler;
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

                        DebtsResponse dr = postServerRoundtrip(PATH_DEBTS, postData, DebtsResponse.class);
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
            getLoaderManager().getLoader(DEBT_REQUEST_LOADER).onContentChanged();
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
                    mFloatingMenu.showMenuButton(true);

                    // cache
                    List<Contact> forAdapter = new ArrayList<>(mContacts.values());
                    SugarRecord.deleteAll(Contact.class);
                    SugarRecord.saveInTx(forAdapter); // save our circles
                    SugarRecord.save(mCurrentPerson); // save us

                    mDebtPersonList.setAdapter(new AccountsAdapter(DebtsListActivity.this, forAdapter));
                    return true;
                case MSG_DEBT_CREATED:
                    mDebts.add(0, (Debt) msg.obj);
                    mDebtList.getAdapter().notifyItemInserted(0);

                    // hide contact list and FAM
                    mFloatingMenu.hideMenu(true);
                    toggleContactListVisibility();
                    return true;
            }

            return false;
        }
    }

    @Nullable
    private <REQ, RESP> RESP postServerRoundtrip(String pathSuffix, REQ request, Class<RESP> respClass) {
        Request postQuery = new Request.Builder()
                .url(String.format("http://%s/debt/%s", mServerEndpoint, pathSuffix))
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

    private void toggleContactListVisibility() {
        if (mDebtAddForm.getVisibility() == View.INVISIBLE) {
            mDebtAddForm.setVisibility(View.VISIBLE);
            Animator cr = ViewAnimationUtils.createCircularReveal(mDebtAddForm,
                    mDebtAddForm.getBottom(),
                    mDebtAddForm.getRight(),
                    0,
                    Math.max(mDebtAddForm.getWidth(), mDebtAddForm.getHeight()) * 2);

            cr.start();
        } else {
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

    /**
     * Callback for notifications that runs on background thread
     */
    private class BackgroundCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CREATE_DEBT: {
                    Debt toCreate = (Debt) msg.obj;
                    if (toCreate.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                        toCreate.setSrc(mCurrentPerson.toActor());
                        toCreate.setApprovedBySrc(true);
                        toCreate.setApprovedByDest(false);
                    } else {
                        toCreate.setSrc(toCreate.getDest());
                        toCreate.setDest(mCurrentPerson.toActor());
                        toCreate.setAmount(toCreate.getAmount().negate());
                        toCreate.setApprovedBySrc(false);
                        toCreate.setApprovedByDest(true);
                    }

                    // enclose it in request
                    DebtCreationRequest dcr = new DebtCreationRequest();
                    dcr.setCreated(toCreate);

                    GenericResponse gr = postServerRoundtrip(PATH_CREATE, dcr, GenericResponse.class);
                    if (gr != null && TextUtils.equals(gr.getResult(), "created")) {
                        toCreate.setId(gr.getNewId());
                        Message toUi = Message.obtain(msg);
                        toUi.what = MSG_DEBT_CREATED;
                        mUiHandler.sendMessage(toUi);
                        return true;
                    }
                    return false;
                }
                case MSG_APPROVE_DEBT: {
                    Debt toApprove = (Debt) msg.obj;

                    // enclose in request
                    DebtApprovalRequest dar = new DebtApprovalRequest();
                    dar.setDebtIdToApprove(toApprove.getId());
                    dar.setMe(mCurrentPerson.toActor());

                    GenericResponse gr = postServerRoundtrip(PATH_APPROVE, dar, GenericResponse.class);
                    if (gr != null && TextUtils.equals(gr.getResult(), "approved")) {
                        if (Objects.equals(dar.getMe().getId(), toApprove.getSrc().getId())) {
                            toApprove.setApprovedBySrc(true);
                        }
                        if (Objects.equals(dar.getMe().getId(), toApprove.getDest().getId())) {
                            toApprove.setApprovedByDest(true);
                        }
                        getLoaderManager().getLoader(DEBT_REQUEST_LOADER).onContentChanged();
                        return true;
                    }
                    return false;
                }
                case MSG_DELETE_DEBT: {
                    Debt toDelete = (Debt) msg.obj;
                    // enclose in request
                    DebtDeleteRequest ddr = new DebtDeleteRequest();
                    ddr.setDebtIdToDelete(toDelete.getId());
                    ddr.setMe(mCurrentPerson.toActor());

                    GenericResponse gr = postServerRoundtrip(PATH_DELETE, ddr, GenericResponse.class);
                    if (gr != null && TextUtils.equals(gr.getResult(), "deleted")) {
                        getLoaderManager().getLoader(DEBT_REQUEST_LOADER).onContentChanged();
                        return true;
                    }
                    return false;
                }
            }

            return false;
        }
    }

    private class FabClickListener implements OnClickListener {
        private boolean iAmDest;

        FabClickListener(boolean giveOrGet) {
            this.iAmDest = giveOrGet;
        }

        @Override
        public void onClick(View v) {
            mDebtPersonList.setTag(iAmDest);
            toggleContactListVisibility();
        }
    }

    private class FamClickListener implements FloatingActionMenu.OnMenuToggleListener {
        @Override
        public void onMenuToggle(boolean opened) {
            // contact list is visible but we closed the menu, hide it too
            if(!opened && mDebtAddForm.getVisibility() == View.VISIBLE) {
                toggleContactListVisibility();
            }
        }
    }

    /**
     * Listens for preferences changes and applies them to needed parts.
     * @see DebtsPrefActivity
     */
    private class PrefChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            switch (key) {
                case PREF_SERVER_ADDRESS:
                    mServerEndpoint = sharedPreferences.getString(key, DEFAULT_SERVER_ENDPOINT);
                    return;
            }
        }
    }
}
