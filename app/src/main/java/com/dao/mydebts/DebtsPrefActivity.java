package com.dao.mydebts;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;

/**
 * Main preference activity for application. No groups for now.
 * @author Oleg Chernovskiy
 */
public class DebtsPrefActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        getFragmentManager().beginTransaction().replace(R.id.content_frame, new MainPreferences()).commit();
    }

    public static class MainPreferences extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.global_prefs);
        }
    }

}
