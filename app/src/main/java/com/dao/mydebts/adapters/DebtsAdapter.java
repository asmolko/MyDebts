package com.dao.mydebts.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.dao.mydebts.DebtsListActivity;
import com.dao.mydebts.R;
import com.dao.mydebts.entities.Contact;
import com.dao.mydebts.entities.Debt;
import com.dao.mydebts.misc.AccountHolder;
import com.dao.mydebts.misc.ImageCache;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter for RecyclerView for showing debts for current user.
 * Debts may react on click, e.g. delete/modify or approve.
 *
 * Created by Oleg Chernovskiy on 06.04.16.
 */
public class DebtsAdapter extends RecyclerView.Adapter<DebtsAdapter.AccViewHolder> {

    private final List<Debt> mDebts;
    private final Map<String, Contact> mContactsCache;
    private final Context mContext;

    /**
     * Constructs new Debts Adapter.
     * The adapter itself cannot modify arguments provided to it.
     * Contact map is needed because server DTOs don't contain such fields as Display Name,
     * avatars, etc.
     * @param context the Context
     * @param debts debts to construct adapter from
     * @param contactsCache contacts to populate contact fields from
     */
    public DebtsAdapter(Context context, List<Debt> debts, Map<String, Contact> contactsCache) {
        this.mDebts = Collections.unmodifiableList(debts);
        this.mContactsCache = Collections.unmodifiableMap(contactsCache);
        this.mContext = context;
    }

    @Override
    public DebtsAdapter.AccViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.debts_list_item, parent, false);
        return new AccViewHolder(itemView);
    }

    @SuppressWarnings("deprecation") // getColor(int, Theme) for API23 only
    @Override
    public void onBindViewHolder(DebtsAdapter.AccViewHolder holder, int position) {
        Debt requested = mDebts.get(position);
        holder.bind(requested);
    }

    @Override
    public void onViewRecycled(AccViewHolder holder) {
        holder.clear();
    }

    @Override
    public int getItemCount() {
        return mDebts.size();
    }

    class AccViewHolder extends RecyclerView.ViewHolder {
        private final ImageView srcBadge;
        private final TextView srcName;
        private final TextView srcApproval;
        private final ImageView destBadge;
        private final TextView destName;
        private final TextView destApproval;
        private final TextView amount;
        private final TextView date;
        private final ImageView overflowButton;

        private Debt item;

        public AccViewHolder(View view) {
            super(view);
            view.setTag(this);
            CardView main = (CardView) view.findViewById(R.id.debt_item_root);
            view.setOnClickListener(new DebtApproveListener());
            overflowButton = (ImageView) view.findViewById(R.id.debt_item_popup_menu);
            overflowButton.setTag(this);
            overflowButton.setOnClickListener(new DebtMenuListener());

            srcBadge = (ImageView) main.findViewById(R.id.debt_item_src_img);
            srcName = (TextView) main.findViewById(R.id.debt_item_src_name);
            srcApproval = (TextView) main.findViewById(R.id.debt_item_src_approval_status);
            destBadge = (ImageView) main.findViewById(R.id.debt_item_dest_img);
            destName = (TextView) main.findViewById(R.id.debt_item_dest_name);
            destApproval = (TextView) main.findViewById(R.id.debt_item_dest_approval_status);
            date = (TextView) view.findViewById(R.id.debt_item_date);
            amount = (TextView) view.findViewById(R.id.debt_item_amount);
        }

        public void bind(Debt item) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            this.item = item;

            String me = AccountHolder.getSavedGoogleId(mContext);
            Contact src = mContactsCache.get(item.getSrc().getId());
            Contact dest = mContactsCache.get(item.getDest().getId());

            // common
            date.setText(sdf.format(item.getCreated().getTime()));
            srcApproval.setText(item.isApprovedBySrc()
                    ? R.string.approved
                    : R.string.not_approved);
            srcApproval.setTextColor(item.isApprovedBySrc()
                    ? mContext.getResources().getColor(R.color.approved_debt)
                    : mContext.getResources().getColor(R.color.not_approved_debt));
            destApproval.setText(item.isApprovedByDest()
                    ? R.string.approved
                    : R.string.not_approved);
            destApproval.setTextColor(item.isApprovedByDest()
                    ? mContext.getResources().getColor(R.color.approved_debt)
                    : mContext.getResources().getColor(R.color.not_approved_debt));

            if (src != null) {
                ImageCache.getInstance(mContext).loadImage(src.getImageUrl(), srcBadge);
                srcName.setText(src.getDisplayName());
            } else {
                srcBadge.setImageDrawable(null);
                srcName.setText(R.string.unknown_person);
            }
            if (dest != null) {
                ImageCache.getInstance(mContext).loadImage(dest.getImageUrl(), destBadge);
                destName.setText(dest.getDisplayName());
            } else {
                destBadge.setImageDrawable(null);
                destName.setText(R.string.unknown_person);
            }

            // specific: we're in debt
            if (TextUtils.equals(item.getSrc().getId(), me)) {
                srcName.setText(R.string.you);
                amount.setText(String.format(Locale.getDefault(), "-%s", item.getAmount().toPlainString()));
                amount.setTextColor(mContext.getResources().getColor(android.R.color.holo_red_dark));
            }

            // specific: someone is in debt with us
            if (TextUtils.equals(item.getDest().getId(), me)) {
                destName.setText(R.string.you);
                amount.setText(String.format(Locale.getDefault(), "+%s", item.getAmount().toPlainString()));
                amount.setTextColor(mContext.getResources().getColor(android.R.color.holo_green_dark));
            }
        }

        public void clear() {
            date.setText("");
            amount.setText("");
            srcName.setText("");
            srcApproval.setText("");
            srcBadge.setImageDrawable(null);
            destName.setText("");
            destApproval.setText("");
            destBadge.setImageDrawable(null);
        }
    }

    private class DebtApproveListener implements View.OnClickListener {

        private Debt toApprove;

        private AlertDialog buildApproveDialog() {
            AlertDialog dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(R.string.approve_dialog_title);
            builder.setMessage(R.string.approve_dialog_message);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // send back to activity
                    DebtsListActivity casted = (DebtsListActivity) mContext;
                    Handler bHandler = casted.getBackgroundHandler();
                    Message msg = Message.obtain(bHandler, DebtsListActivity.MSG_APPROVE_DEBT, toApprove);
                    bHandler.sendMessage(msg);
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            dialog = builder.create();
            return dialog;
        }

        @Override
        public void onClick(View v) {
            AccViewHolder holder = (AccViewHolder) v.getTag();
            String me = AccountHolder.getSavedGoogleId(mContext);
            Debt toApprove = holder.item;

            // sanity checks
            if (TextUtils.equals(toApprove.getDest().getId(), me) && toApprove.isApprovedByDest()) {
                return;
            }

            if (TextUtils.equals(toApprove.getSrc().getId(), me) && toApprove.isApprovedBySrc()) {
                return;
            }

            this.toApprove = toApprove;
            buildApproveDialog().show();
        }
    }

    private class DebtMenuListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            AccViewHolder holder = (AccViewHolder) v.getTag();
            final Debt requested = holder.item;

            PopupMenu pm = new PopupMenu(v.getContext(), v);
            pm.inflate(R.menu.popup_debt_item);
            if(requested.isApprovedByDest() && requested.isApprovedBySrc()) { // approved debt, can't delete
                pm.getMenu().findItem(R.id.delete_debt).setVisible(false);
            }
            pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    DebtsListActivity casted = (DebtsListActivity) mContext;
                    Handler bHandler = casted.getBackgroundHandler();
                    switch (item.getItemId()) {
                        case R.id.delete_debt:
                            Message del = Message.obtain(bHandler, DebtsListActivity.MSG_DELETE_DEBT, requested);
                            bHandler.sendMessage(del);
                            return true;
                        case R.id.show_audit:
                            Message audit = Message.obtain(bHandler, DebtsListActivity.MSG_AUDIT_DEBT, requested);
                            bHandler.sendMessage(audit);
                            return true;
                    }
                    return false;
                }
            });
            pm.show();
        }
    }
}
