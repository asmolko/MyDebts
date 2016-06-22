package com.dao.mydebts;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.dao.mydebts.entities.AuditEntry;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Shows audit event log for requested debt. Audit event is basically an action that server performed
 * on behalf of a group of people, e.g. debt mutual settlement or merging.
 *
 * @author Oleg Chernovskiy
 */
public class AuditLogListFragment extends DialogFragment {
    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());

    private List<AuditEntry> mDebtHistory;

    public AuditLogListFragment() {
    }

    /**
     * IDK why Google requests the static method to instantiate Fragments
     * @return new instance of {@link AuditLogListFragment} to be used in {@link FragmentManager}
     */
    public static AuditLogListFragment newInstance(List<AuditEntry> debtHistory) {
        AuditLogListFragment hlf = new AuditLogListFragment();
        hlf.mDebtHistory = debtHistory;
        return hlf;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ListView mListView = new ListView(getActivity());
        mListView.setAdapter(new AuditLogAdapter(mDebtHistory));

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
}
