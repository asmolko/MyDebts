package com.dao.mydebts.adapters;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
    private final DebtsListActivity.DebtClickListener debtClickListener;

    /**
     * Constructs new Debts Adapter.
     * The adapter itself cannot modify arguments provided to it.
     * Contact map is needed because server DTOs don't contain such fields as Display Name,
     * avatars, etc.
     * @param context the Context
     * @param debts debts to construct adapter from
     * @param contactsCache contacts to populate contact fields from
     */
    public DebtsAdapter(Context context, List<Debt> debts, Map<String, Contact> contactsCache, DebtsListActivity.DebtClickListener debtClickListener) {
        this.mDebts = Collections.unmodifiableList(debts);
        this.mContactsCache = Collections.unmodifiableMap(contactsCache);
        this.mContext = context;
        this.debtClickListener = debtClickListener;
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
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        Debt requested = mDebts.get(position);

        holder.date.setText(sdf.format(requested.getCreated().getTime()));
        Contact cached = null;

        // we're in debt
        if (TextUtils.equals(requested.getSrc().getId(), AccountHolder.getSavedGoogleId(mContext))) {
            holder.amount.setText(String.format(Locale.getDefault(), "-%s", requested.getAmount().toPlainString()));
            holder.amount.setTextColor(mContext.getResources().getColor(android.R.color.holo_red_dark));
            cached = mContactsCache.get(requested.getDest().getId());
        }

        // someone is in debt with us
        if (TextUtils.equals(requested.getDest().getId(), AccountHolder.getSavedGoogleId(mContext))) {
            holder.amount.setText(String.format(Locale.getDefault(), "+%s", requested.getAmount().toPlainString()));
            holder.amount.setTextColor(mContext.getResources().getColor(android.R.color.holo_green_dark));
            cached = mContactsCache.get(requested.getDest().getId());
        }

        if(cached == null) { // you have no such person in your Plus Circles
            holder.name.setText(R.string.unknown_person);
            holder.badge.setImageDrawable(null);
            return;
        }

        holder.name.setText(cached.getDisplayName());
        holder.badge.setImageDrawable(ImageCache.getInstance(mContext).get(cached.getImageUrl()));
    }

    @Override
    public void onViewRecycled(AccViewHolder holder) {
        holder.date.setText("");
        holder.amount.setText("");
        holder.name.setText("");
        holder.badge.setImageDrawable(null);
    }

    @Override
    public int getItemCount() {
        return mDebts.size();
    }

    class AccViewHolder extends RecyclerView.ViewHolder {
        private final ImageView badge;
        private final TextView name;
        private final TextView date;
        private final TextView amount;

        public AccViewHolder(View view) {
            super(view);
            CardView main = (CardView) view.findViewById(R.id.debt_item_root); // TODO: add click events
            main.setOnClickListener(debtClickListener);
            badge = (ImageView) main.findViewById(R.id.debt_item_contact_img);
            name = (TextView) main.findViewById(R.id.debt_item_contact_name);
            date = (TextView) view.findViewById(R.id.debt_item_date);
            amount = (TextView) view.findViewById(R.id.debt_item_amount);
        }
    }
}
