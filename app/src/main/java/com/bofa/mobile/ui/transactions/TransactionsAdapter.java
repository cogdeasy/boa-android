package com.bofa.mobile.ui.transactions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bofa.mobile.R;
import com.bofa.mobile.model.Transaction;
import com.bofa.mobile.util.BankDates;
import com.bofa.mobile.util.Money;

import java.util.ArrayList;
import java.util.List;

public class TransactionsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ROW = 0;
    private static final int TYPE_LOAD_MORE = 1;

    private final List<Transaction> items = new ArrayList<>();
    private final Runnable onLoadMore;
    private boolean showLoadMore;
    private boolean showRunningBalance = true;

    public TransactionsAdapter(Runnable onLoadMore) {
        this.onLoadMore = onLoadMore;
    }

    public void replace(List<Transaction> transactions, boolean hasMore, boolean runningBalance) {
        items.clear();
        items.addAll(transactions);
        showLoadMore = hasMore;
        showRunningBalance = runningBalance;
        notifyDataSetChanged();
    }

    public void append(List<Transaction> transactions, boolean hasMore) {
        items.addAll(transactions);
        showLoadMore = hasMore;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position == items.size() ? TYPE_LOAD_MORE : TYPE_ROW;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_LOAD_MORE) {
            View v = inflater.inflate(R.layout.item_load_more, parent, false);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onLoadMore.run();
                }
            });
            return new RecyclerView.ViewHolder(v) { };
        }
        return new RowHolder(inflater.inflate(R.layout.item_transaction, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (!(holder instanceof RowHolder)) {
            return;
        }
        RowHolder row = (RowHolder) holder;
        Transaction t = items.get(position);
        row.description.setText(t.getDescription());
        row.meta.setText(BankDates.display(t.getBusinessDate()) + "  \u00b7  " + t.getChannel());
        row.amount.setText(Money.format(t.getAmount()));
        row.amount.setTextColor(ContextCompat.getColor(row.itemView.getContext(),
                t.isCredit() ? R.color.boa_credit : R.color.boa_debit));
        if (showRunningBalance) {
            row.balance.setText(Money.format(t.getRunningBalance()));
            row.balance.setVisibility(View.VISIBLE);
        } else {
            row.balance.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size() + (showLoadMore ? 1 : 0);
    }

    static class RowHolder extends RecyclerView.ViewHolder {
        final TextView description;
        final TextView meta;
        final TextView amount;
        final TextView balance;

        RowHolder(View v) {
            super(v);
            description = v.findViewById(R.id.txn_description);
            meta = v.findViewById(R.id.txn_meta);
            amount = v.findViewById(R.id.txn_amount);
            balance = v.findViewById(R.id.txn_balance);
        }
    }
}
