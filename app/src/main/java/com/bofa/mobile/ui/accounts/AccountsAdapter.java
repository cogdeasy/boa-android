package com.bofa.mobile.ui.accounts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bofa.mobile.R;
import com.bofa.mobile.model.Account;
import com.bofa.mobile.util.Money;

import java.util.ArrayList;
import java.util.List;

public class AccountsAdapter extends RecyclerView.Adapter<AccountsAdapter.Holder> {

    public interface OnAccountClickListener {
        void onAccountClick(Account account);
    }

    private final List<Account> accounts = new ArrayList<>();
    private final OnAccountClickListener listener;

    public AccountsAdapter(OnAccountClickListener listener) {
        this.listener = listener;
    }

    public void setAccounts(List<Account> items) {
        accounts.clear();
        accounts.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_account, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        final Account account = accounts.get(position);
        holder.name.setText(account.getNickname());
        holder.number.setText(account.getMaskedNumber());
        holder.available.setText(Money.format(account.getAvailableBalance()));
        holder.ledger.setText(holder.itemView.getContext().getString(R.string.accounts_ledger,
                Money.format(account.getLedgerBalance())));
        if (Money.isPositive(account.getHoldAmount())) {
            holder.hold.setText(holder.itemView.getContext().getString(R.string.accounts_hold,
                    Money.format(account.getHoldAmount())));
            holder.hold.setVisibility(View.VISIBLE);
        } else {
            holder.hold.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onAccountClick(account);
            }
        });
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView number;
        final TextView available;
        final TextView ledger;
        final TextView hold;

        Holder(View v) {
            super(v);
            name = v.findViewById(R.id.account_name);
            number = v.findViewById(R.id.account_number);
            available = v.findViewById(R.id.account_available);
            ledger = v.findViewById(R.id.account_ledger);
            hold = v.findViewById(R.id.account_hold);
        }
    }
}
