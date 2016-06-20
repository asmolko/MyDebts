package com.dao.mydebts.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dao.mydebts.DebtsListActivity;
import com.dao.mydebts.R;
import com.dao.mydebts.entities.Contact;
import com.dao.mydebts.entities.Debt;
import com.dao.mydebts.misc.ImageCache;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Adapter holding contact cards.
 * Supports async loading of account profile images.
 *
 * @author Alexander Smolko, Oleg Chernovskiy
 */
public class AccountsAdapter extends RecyclerView.Adapter<AccountsAdapter.AccViewHolder> {

    private static final String AA_TAG = AccountsAdapter.class.getSimpleName();

    private final List<Contact> mContacts;
    private final Context mContext;
    private RecyclerView mParentView;

    /**
     * Constructs new AccountsAdapter.
     * The adapter itself cannot modify arguments provided to it.
     *
     * @param context the Context
     * @param contacts contacts list to build adapter items from
     */
    public AccountsAdapter(Context context, List<Contact> contacts) {
        this.mContacts = Collections.unmodifiableList(contacts);
        this.mContext = context;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        mParentView = recyclerView;
    }

    @Override
    public AccViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.contact_list_item, parent, false);
        return new AccViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(AccViewHolder holder, int position) {
        Contact it = mContacts.get(position);
        holder.name.setText(it.getDisplayName());
        if(!TextUtils.isEmpty(it.getImageUrl())) {
            ImageCache.getInstance(mContext).loadImage(it.getImageUrl(), holder.badge);
        }
    }

    @Override
    public void onViewRecycled(AccViewHolder holder) {
        holder.name.setText("");
        holder.badge.setImageDrawable(null);
    }

    @Override
    public int getItemCount() {
        return mContacts.size();
    }

    class AccViewHolder extends RecyclerView.ViewHolder {
        private final ImageView badge;
        private final TextView name;

        public AccViewHolder(View view) {
            super(view);
            RelativeLayout rl = (RelativeLayout) view.findViewById(R.id.contact_item_root);
            name = (TextView) rl.findViewById(R.id.contact_name);
            badge = (ImageView) rl.findViewById(R.id.contact_img);

            view.setOnClickListener(new CreateDebtHandler());
        }

        private class CreateDebtHandler implements View.OnClickListener {
            @Override
            public void onClick(View v) {
                final EditText input = new EditText(v.getContext());
                final Contact who = mContacts.get(getLayoutPosition());
                final boolean iAmDest = (boolean) mParentView.getTag();
                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

                new AlertDialog.Builder(v.getContext())
                        .setView(input)
                        .setTitle(who.getDisplayName())
                        .setMessage(iAmDest ? R.string.give_dialog_text : R.string.get_dialog_text)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                String amount = input.getText().toString();
                                if (amount.isEmpty()) {
                                    return;
                                }

                                // convert to app entities
                                Debt toCreate = new Debt();
                                toCreate.setDest(who.toActor());
                                BigDecimal parsed = new BigDecimal(amount);
                                toCreate.setAmount(iAmDest ? parsed.negate() : parsed);

                                // send back to activity
                                DebtsListActivity casted = (DebtsListActivity) mContext;
                                Handler bHandler = casted.getBackgroundHandler();
                                Message msg = Message.obtain(bHandler, DebtsListActivity.MSG_CREATE_DEBT, toCreate);
                                bHandler.sendMessage(msg);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create().show();

            }
        }
    }
}
