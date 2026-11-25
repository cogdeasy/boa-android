package com.bofa.mobile.ui.transactions;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bofa.mobile.R;
import com.bofa.mobile.api.ApiClient;
import com.bofa.mobile.api.ApiErrors;
import com.bofa.mobile.model.Account;
import com.bofa.mobile.model.Transaction;
import com.bofa.mobile.model.TransactionPage;
import com.bofa.mobile.ui.common.BaseActivity;
import com.bofa.mobile.util.BankDates;
import com.bofa.mobile.util.Money;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransactionHistoryActivity extends BaseActivity {

    public static final String EXTRA_ACCOUNT = "com.bofa.mobile.extra.ACCOUNT";
    private static final String TAG = "TxnHistory";
    private static final int PAGE_SIZE = 50;
    private static final int HISTORY_DAYS = 90;

    private Account account;
    private TransactionsAdapter adapter;
    private ProgressBar progress;
    private TextView emptyText;
    private TextView summaryText;
    private EditText searchField;
    private int page;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setElevation(0);
        }

        account = (Account) getIntent().getSerializableExtra(EXTRA_ACCOUNT);
        if (account == null) {
            finish();
            return;
        }
        setTitle(account.getNickname());
        ((TextView) findViewById(R.id.txn_account_name)).setText(account.getMaskedNumber() + "  \u00b7  Available");
        ((TextView) findViewById(R.id.txn_account_balance)).setText(Money.format(account.getAvailableBalance()));

        progress = findViewById(R.id.txn_progress);
        emptyText = findViewById(R.id.txn_empty);
        summaryText = findViewById(R.id.txn_summary);
        searchField = findViewById(R.id.txn_search);

        RecyclerView list = findViewById(R.id.txn_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapter = new TransactionsAdapter(new Runnable() {
            @Override
            public void run() {
                loadPage(page + 1);
            }
        });
        list.setAdapter(adapter);

        searchField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    runSearch(v.getText().toString().trim());
                    return true;
                }
                return false;
            }
        });

        loadPage(0);
    }

    private void loadPage(final int requestedPage) {
        setLoading(requestedPage == 0);
        Log.d(TAG, "Loading page " + requestedPage + " for " + account.getAccountNumber());
        ApiClient.coreBanking().getTransactions(account.getAccountNumber(),
                BankDates.daysAgo(HISTORY_DAYS), BankDates.today(), requestedPage, PAGE_SIZE)
                .enqueue(new Callback<TransactionPage>() {
                    @Override
                    public void onResponse(Call<TransactionPage> call, Response<TransactionPage> response) {
                        setLoading(false);
                        if (!response.isSuccessful() || response.body() == null) {
                            showSummary(ApiErrors.userMessage(TransactionHistoryActivity.this, response));
                            return;
                        }
                        TransactionPage body = response.body();
                        page = body.getPage();
                        if (requestedPage == 0) {
                            adapter.replace(body.getContent(), body.hasMore(), true);
                        } else {
                            adapter.append(body.getContent(), body.hasMore());
                        }
                        emptyText.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onFailure(Call<TransactionPage> call, Throwable t) {
                        setLoading(false);
                        showSummary(getString(R.string.error_network));
                    }
                });
    }

    private void runSearch(final String query) {
        hideKeyboard();
        if (TextUtils.isEmpty(query)) {
            summaryText.setVisibility(View.GONE);
            loadPage(0);
            return;
        }
        setLoading(true);
        ApiClient.coreBanking().searchTransactions(query, account.getAccountNumber())
                .enqueue(new Callback<List<Transaction>>() {
                    @Override
                    public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                        setLoading(false);
                        if (!response.isSuccessful() || response.body() == null) {
                            showSummary(ApiErrors.userMessage(TransactionHistoryActivity.this, response));
                            return;
                        }
                        List<Transaction> results = response.body();
                        adapter.replace(results, false, false);
                        showSummary(getString(R.string.activity_search_results, results.size(), query));
                        emptyText.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onFailure(Call<List<Transaction>> call, Throwable t) {
                        setLoading(false);
                        showSummary(getString(R.string.error_network));
                    }
                });
    }

    private void showSummary(String text) {
        summaryText.setText(text);
        summaryText.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            emptyText.setVisibility(View.GONE);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);
        }
    }
}
