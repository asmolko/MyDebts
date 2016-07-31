package com.dao.mydebts;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.dao.mydebts.entities.AuditEntry;
import com.dao.mydebts.entities.Contact;
import com.dao.mydebts.misc.ImageCache;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.dao.mydebts.DebtsListActivity.MSG_AUDIT_GROUP;

/**
 * Shows audit event log for requested debt. Audit event is basically an action that server performed
 * on behalf of a group of people, e.g. debt mutual settlement or merging.
 *
 * @author Oleg Chernovskiy
 */
public class AuditLogListFragment extends DialogFragment {
    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());

    private List<AuditEntry> mDebtHistory;
    private Map<String, Contact> mContactsCache;
    private boolean extendedFormat;

    public AuditLogListFragment() {
    }

    /**
     * IDK why Google requests the static method to instantiate Fragments
     * @return new instance of {@link AuditLogListFragment} to be used in {@link FragmentManager}
     */
    public static AuditLogListFragment newInstance(Map<String, Contact> cache,
                                                   List<AuditEntry> debtHistory,
                                                   boolean extendedFormat) {
        AuditLogListFragment hlf = new AuditLogListFragment();
        hlf.mContactsCache = cache;
        hlf.mDebtHistory = debtHistory;
        hlf.extendedFormat = extendedFormat;
        return hlf;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ListView mListView = new ListView(getActivity());
        mListView.setAdapter(extendedFormat ? new AuditDetailLogAdapter(mDebtHistory) : new AuditLogAdapter(mDebtHistory));
        mListView.setOnItemClickListener(new LogDetailsClickListener());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.audit);
        builder.setView(mListView);
        return builder.create();
    }

    private class AuditLogAdapter extends ArrayAdapter<AuditEntry> {

        public AuditLogAdapter(List<AuditEntry> mDebtHistory) {
            super(getActivity(), 0, mDebtHistory);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null) {
                v = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_expandable_list_item_2, parent, false);
            } else {
                v = convertView;
            }
            AuditEntry item = getItem(position);

            TextView title = (TextView) v.findViewById(android.R.id.text1);
            TextView text = (TextView) v.findViewById(android.R.id.text2);

            title.setText(sdf.format(item.getCreated()));
            text.setText(item.getAmount().toPlainString());
            if (item.getAmount().signum() > 0) {
                text.setTextColor(getActivity().getResources().getColor(android.R.color.holo_green_dark));
            } else {
                text.setTextColor(getActivity().getResources().getColor(android.R.color.holo_red_dark));
            }

            return v;
        }
    }

    private class AuditDetailLogAdapter extends ArrayAdapter<AuditEntry> {

        public AuditDetailLogAdapter(List<AuditEntry> mDebtHistory) {
            super(getActivity(), 0, mDebtHistory);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null) {
                v = LayoutInflater.from(getContext()).inflate(R.layout.audit_list_item, parent, false);
            } else {
                v = convertView;
            }
            ImageView srcBadge = (ImageView) v.findViewById(R.id.contact_img_src);
            ImageView destBadge = (ImageView) v.findViewById(R.id.contact_img_dst);
            TextView origAmount = (TextView) v.findViewById(R.id.original_amount);
            TextView settleAmount = (TextView) v.findViewById(R.id.settle_amount);
            TextView settleDate = (TextView) v.findViewById(R.id.settle_date);

            AuditEntry item = getItem(position);
            Contact src = mContactsCache.get(item.getSettled().getSrc().getId());
            if (src != null) {
                ImageCache.getInstance(getContext()).loadImage(src.getImageUrl(), srcBadge);
            } else {
                srcBadge.setImageDrawable(null);
            }
            Contact dest = mContactsCache.get(item.getSettled().getDest().getId());
            if (dest != null) {
                ImageCache.getInstance(getContext()).loadImage(dest.getImageUrl(), destBadge);
            } else {
                destBadge.setImageDrawable(null);
            }

            settleDate.setText(sdf.format(item.getSettled().getCreated()));
            origAmount.setText(item.getOriginalAmount().toPlainString());
            if (item.getAmount().signum() > 0) {
                settleAmount.setText(String.format(Locale.getDefault(), "+%s", item.getAmount().toPlainString()));
                settleAmount.setTextColor(getActivity().getResources().getColor(android.R.color.holo_green_dark));
            } else {
                settleAmount.setText(item.getAmount().toPlainString()); // it already has minus prepended
                settleAmount.setTextColor(getActivity().getResources().getColor(android.R.color.holo_red_dark));
            }

            return v;
        }
    }

    /**
     * Sends message for whole relaxation retrieval for group of selected log entry
     */
    private class LogDetailsClickListener implements android.widget.AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
            Handler bgHandler = ((DebtsListActivity) getActivity()).getBackgroundHandler();
            AuditEntry selected = (AuditEntry) listView.getAdapter().getItem(position);
            bgHandler.sendMessage(bgHandler.obtainMessage(MSG_AUDIT_GROUP, selected));
        }
    }
}
