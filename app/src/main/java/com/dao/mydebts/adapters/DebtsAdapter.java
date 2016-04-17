package com.dao.mydebts.adapters;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import com.dao.mydebts.R;
import com.dao.mydebts.entities.Contact;
import com.dao.mydebts.entities.Debt;
import com.dao.mydebts.misc.ImageCache;

import java.text.SimpleDateFormat;
import java.util.Collection;
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

    /**
     * Constructs new Debts Adapter.
     * The adapter itself cannot modify arguments provided to it.
     * Contact map is needed because server DTOs don't contain such fields as Display Name,
     * avatars, etc.
     * @param debts debts to construct adapter from
     * @param contactsCache contacts to populate contact fields from
     */
    public DebtsAdapter(List<Debt> debts, Map<String, Contact> contactsCache) {
        this.mDebts = Collections.unmodifiableList(debts);
        this.mContactsCache = Collections.unmodifiableMap(contactsCache);
    }

    @Override
    public DebtsAdapter.AccViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.debts_list_item, parent, false);
        return new AccViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(DebtsAdapter.AccViewHolder holder, int position) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        Debt requested = mDebts.get(position);

        holder.date.setText(sdf.format(requested.getCreated().getTime()));
        holder.amount.setText(requested.getAmount().toPlainString());

        Contact cached = mContactsCache.get(requested.getTo().getId());
        if(cached == null) { // you have no such person in your Plus Circles
            holder.name.setText(R.string.unknown_person);
            holder.badge.setImageDrawable(null);
            return;
        }

        holder.name.setText(cached.getDisplayName());
        holder.badge.setImageDrawable(ImageCache.getInstance().get(cached.getImageUrl()));
        // TODO approval status
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

            badge = (ImageView) main.findViewById(R.id.debt_item_contact_img);
            name = (TextView) main.findViewById(R.id.debt_item_contact_name);
            date = (TextView) view.findViewById(R.id.debt_item_date);
            amount = (TextView) view.findViewById(R.id.debt_item_amount);
        }
    }
}
