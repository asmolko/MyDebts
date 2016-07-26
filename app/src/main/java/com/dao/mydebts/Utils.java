package com.dao.mydebts;

import android.app.Activity;
import android.widget.Toast;

/**
 * Created by adonai on 26.07.16.
 */

public class Utils {

    private Utils() {
    }

    public static void showToastFromAnyThread(final Activity target, final String toShow) {
        // can't show a toast from a thread without looper
        if(target == null) // probably called from detached fragment (app hidden)
            return;

        target.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(target, toShow, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
