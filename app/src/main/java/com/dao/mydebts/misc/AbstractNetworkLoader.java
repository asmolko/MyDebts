package com.dao.mydebts.misc;

import android.content.AsyncTaskLoader;
import android.content.Context;

/**
 * Quick and dirty template of what's needed for network querying
 *
 * @author Oleg Chernovskiy
 */
public abstract class AbstractNetworkLoader<D> extends AsyncTaskLoader<D> {

    private D mData;

    public AbstractNetworkLoader(Context context) {
        super(context);
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onStartLoading() {
        if (mData != null) {
            // Deliver any previously loaded data immediately.
            deliverResult(mData);
        }

        if (takeContentChanged() || mData == null) {
            forceLoad();
        }
    }

    @Override
    protected D onLoadInBackground() {
        mData = loadInBackground();
        return mData;
    }

    @Override
    public void deliverResult(D data) {
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            return;
        }

        if (isStarted()) {
            // If the Loader is in a started state, deliver the results to the
            // client. The superclass method does this for us.
            super.deliverResult(data);
        }
    }

    @Override
    protected void onReset() {
        mData = null;
    }
}
