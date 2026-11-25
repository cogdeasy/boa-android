package com.northbridge.mobile.ui.payments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.northbridge.mobile.R;
import com.northbridge.mobile.api.ApiClient;
import com.northbridge.mobile.model.Payment;
import com.northbridge.mobile.session.SessionManager;
import com.northbridge.mobile.ui.common.BaseActivity;
import com.northbridge.mobile.util.AccountNumbers;
import com.northbridge.mobile.util.BankDates;
import com.northbridge.mobile.util.Money;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Payments hub entry point: bill pay, send money and recent payment history. */
public class PaymentsActivity extends BaseActivity {

    private final List<Payment> payments = new ArrayList<>();
    private RecyclerView.Adapter<RecyclerView.ViewHolder> adapter;
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payments);
        emptyText = findViewById(R.id.payments_empty);

        findViewById(R.id.payments_bill).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PaymentsActivity.this, BillPayActivity.class));
            }
        });
        findViewById(R.id.payments_p2p).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PaymentsActivity.this, SendMoneyActivity.class));
            }
        });

        RecyclerView list = findViewById(R.id.payments_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
                return new RecyclerView.ViewHolder(v) { };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                Payment p = payments.get(position);
                View v = holder.itemView;
                ((TextView) v.findViewById(R.id.txn_description)).setText(p.getPayeeName());
                ((TextView) v.findViewById(R.id.txn_meta)).setText(("BILL".equals(p.getType()) ? "Bill pay" : "Sent")
                        + "  \u00b7  " + BankDates.display(p.getCreatedAt())
                        + "  \u00b7  from ..." + AccountNumbers.lastFour(p.getFromAccount()));
                ((TextView) v.findViewById(R.id.txn_amount)).setText(Money.format(p.getAmount()));
                ((TextView) v.findViewById(R.id.txn_balance)).setText(p.getStatus());
            }

            @Override
            public int getItemCount() {
                return payments.size();
            }
        };
        list.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SessionManager.get().isSignedIn()) {
            loadHistory();
        }
    }

    private void loadHistory() {
        ApiClient.payments().getHistory(SessionManager.get().getCif()).enqueue(new Callback<List<Payment>>() {
            @Override
            public void onResponse(Call<List<Payment>> call, Response<List<Payment>> response) {
                payments.clear();
                if (response.isSuccessful() && response.body() != null) {
                    payments.addAll(response.body());
                }
                adapter.notifyDataSetChanged();
                emptyText.setVisibility(payments.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(Call<List<Payment>> call, Throwable t) {
                emptyText.setText(R.string.error_network);
                emptyText.setVisibility(View.VISIBLE);
            }
        });
    }
}
