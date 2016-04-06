package com.dao.mydebts.adapters;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import com.dao.mydebts.R;
import com.dao.mydebts.entities.Debt;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for RecyclerView for showing debts for current user.
 * Debts may react on click, e.g. delete/modify or approve.
 *
 * Created by Oleg Chernovskiy on 06.04.16.
 */
public class DebtsAdapter extends RecyclerView.Adapter<DebtsAdapter.AccViewHolder> {

    private final List<Debt> mObjects;

    public DebtsAdapter(List<Debt> mObjects) {
        this.mObjects = mObjects;
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
        Debt requested = mObjects.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        // holder.mContactBadge // TODO
        // holder.mContactNameText // TODO
        holder.mDebtDateText.setText(sdf.format(requested.getCreated().getTime()));
        holder.mDebtAmountText.setText(requested.getAmount().toPlainString());
        // TODO approval status
    }

    @Override
    public int getItemCount() {
        return mObjects.size();
    }

    class AccViewHolder extends RecyclerView.ViewHolder {
        private final QuickContactBadge mContactBadge;
        private final TextView mContactNameText;
        private final TextView mDebtDateText;
        private final TextView mDebtAmountText;

        public AccViewHolder(View view) {
            super(view);
            CardView main = (CardView) view.findViewById(R.id.debt_item_root); // TODO: add click events

            mContactBadge = (QuickContactBadge) main.findViewById(R.id.debt_item_contact_img);
            mContactNameText = (TextView) main.findViewById(R.id.debt_item_contact_name);
            mDebtDateText = (TextView) view.findViewById(R.id.debt_item_date);
            mDebtAmountText = (TextView) view.findViewById(R.id.debt_item_amount);
        }
    }
}
