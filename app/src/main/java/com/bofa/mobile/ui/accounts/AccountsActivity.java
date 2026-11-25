package com.bofa.mobile.ui.accounts;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bofa.mobile.R;
import com.bofa.mobile.api.ApiClient;
import com.bofa.mobile.api.ApiErrors;
import com.bofa.mobile.model.Account;
import com.bofa.mobile.model.Customer;
import com.bofa.mobile.session.SessionManager;
import com.bofa.mobile.ui.common.AccountsCache;
import com.bofa.mobile.ui.common.BaseActivity;
import com.bofa.mobile.ui.help.HelpActivity;
import com.bofa.mobile.ui.payments.PaymentsActivity;
import com.bofa.mobile.ui.transactions.TransactionHistoryActivity;
import com.bofa.mobile.ui.transfer.TransferActivity;
import com.bofa.mobile.util.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountsActivity extends BaseActivity implements AccountsAdapter.OnAccountClickListener {

    private static final String TAG = "AccountsActivity";

    private SwipeRefreshLayout refresh;
    private AccountsAdapter adapter;
    private TextView totalText;
    private TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts);

        refresh = findViewById(R.id.accounts_refresh);
        totalText = findViewById(R.id.accounts_total);
        errorText = findViewById(R.id.accounts_error);
        RecyclerView list = findViewById(R.id.accounts_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AccountsAdapter(this);
        list.setAdapter(adapter);

        Customer customer = SessionManager.get().getCustomer();
        if (customer != null) {
            ((TextView) findViewById(R.id.accounts_greeting)).setText(greeting() + ", " + customer.getFirstName());
            ((TextView) findViewById(R.id.accounts_segment)).setText(customer.getSegment() + " CLIENT");
        }

        findViewById(R.id.accounts_transfer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AccountsActivity.this, TransferActivity.class));
            }
        });
        findViewById(R.id.accounts_pay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AccountsActivity.this, PaymentsActivity.class));
            }
        });
        refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadAccounts();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SessionManager.get().isSignedIn()) {
            loadAccounts();
        }
    }

    private void loadAccounts() {
        String cif = SessionManager.get().getCif();
        refresh.setRefreshing(true);
        ApiClient.coreBanking().getAccounts(cif).enqueue(new Callback<List<Account>>() {
            @Override
            public void onResponse(Call<List<Account>> call, Response<List<Account>> response) {
                refresh.setRefreshing(false);
                if (response.code() == 401) {
                    returnToLogin();
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    showError(errorText, ApiErrors.userMessage(AccountsActivity.this, response));
                    return;
                }
                showError(errorText, null);
                bind(response.body());
            }

            @Override
            public void onFailure(Call<List<Account>> call, Throwable t) {
                refresh.setRefreshing(false);
                showError(errorText, getString(R.string.error_network));
            }
        });
    }

    private void bind(List<Account> accounts) {
        AccountsCache.set(accounts);
        adapter.setAccounts(accounts);
        List<String> available = new ArrayList<>();
        for (Account a : accounts) {
            available.add(a.getAvailableBalance());
        }
        BigDecimal total = Money.sum(available);
        totalText.setText(Money.format(total));
    }

    @Override
    public void onAccountClick(Account account) {
        Log.d(TAG, "Opening activity for account " + account.getAccountNumber()
                + " available=" + account.getAvailableBalance());
        Intent intent = new Intent(this, TransactionHistoryActivity.class);
        intent.putExtra(TransactionHistoryActivity.EXTRA_ACCOUNT, account);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_accounts, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_sign_out) {
            SessionManager.get().signOut();
            AccountsCache.clear();
            returnToLogin();
            return true;
        }
        if (item.getItemId() == R.id.action_help) {
            startActivity(new Intent(this, HelpActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static String greeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            return "Good morning";
        }
        return hour < 18 ? "Good afternoon" : "Good evening";
    }
}
