package com.dao.mydebts;

import android.app.Application;

import com.orm.SugarContext;

/**
 * @author Alexander Smolko
 */
public class MyDebtsApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SugarContext.init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        SugarContext.terminate();
    }
}
