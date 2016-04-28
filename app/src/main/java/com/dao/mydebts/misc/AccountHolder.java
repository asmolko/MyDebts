package com.dao.mydebts.misc;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

/**
 * Account holder that allows to check/pick application-wide Google+ account.
 * The underlying implementation uses {@link SharedPreferences} as its engine.
 *
 * @author Alexander Smolko on 07.04.2016.
 */
public class AccountHolder {

    private static final String ACCOUNT_NAME_PREFERENCE_KEY = "account-name";  // XML tag in shared_prefs.xml
    private static final String ACCOUNT_GOOGLE_ID_PREFERENCE_KEY = "account-google-id";
    private static final String DEFAULT_ACCOUNT_NAME_VALUE = "STUB";

    @NonNull
    public static String getSavedAccountName(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getString(ACCOUNT_NAME_PREFERENCE_KEY, DEFAULT_ACCOUNT_NAME_VALUE);
    }

    public static void saveAccountName(Context context, String accountName) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(ACCOUNT_NAME_PREFERENCE_KEY, accountName);
        editor.apply();
    }

    public static void saveGoogleId(Context context, String accountId) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(ACCOUNT_GOOGLE_ID_PREFERENCE_KEY, accountId);
        editor.apply();
    }

    @NonNull
    public static String getSavedGoogleId(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getString(ACCOUNT_GOOGLE_ID_PREFERENCE_KEY, DEFAULT_ACCOUNT_NAME_VALUE);
    }

    public static boolean isAccountNameSaved(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.contains(ACCOUNT_NAME_PREFERENCE_KEY);
    }
}
