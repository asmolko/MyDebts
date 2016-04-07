package com.dao.mydebts.misc;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * @author Smolko on 07.04.2016.
 */
public class AccountHolder {

    private static final String DEBTS_SHARED_PREFERENCES_NAME = "DEBTS_SHARED_PREFERENCES_NAME";
    private static final String ACCOUNT_NAME_PREFERENCE_KEY = "ACCOUNT_NAME_PREFERENCE_KEY";
    private static final String DEFAULT_ACCOUNT_NAME_VALUE = "STUB";

    public static String getSavedAccountName(Context context) {
        SharedPreferences settings = context.getSharedPreferences(DEBTS_SHARED_PREFERENCES_NAME, 0);
        return settings.getString(ACCOUNT_NAME_PREFERENCE_KEY, DEFAULT_ACCOUNT_NAME_VALUE);
    }

    public static void saveAccountName(Context context, String accountName) {
        SharedPreferences settings = context.getSharedPreferences(DEBTS_SHARED_PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(ACCOUNT_NAME_PREFERENCE_KEY, accountName);
        editor.apply();
    }

    public static boolean isAccountNameSaved(Context context) {
        SharedPreferences settings = context.getSharedPreferences(DEBTS_SHARED_PREFERENCES_NAME, 0);
        return settings.contains(ACCOUNT_NAME_PREFERENCE_KEY);
    }
}
