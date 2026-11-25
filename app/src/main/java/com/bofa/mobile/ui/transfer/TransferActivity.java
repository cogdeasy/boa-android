package com.bofa.mobile.ui.transfer;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.bofa.mobile.R;
import com.bofa.mobile.api.ApiClient;
import com.bofa.mobile.api.ApiErrors;
import com.bofa.mobile.model.Account;
import com.bofa.mobile.model.TransferRequest;
import com.bofa.mobile.model.TransferResponse;
import com.bofa.mobile.ui.common.AccountSpinners;
import com.bofa.mobile.ui.common.AccountsCache;
import com.bofa.mobile.ui.common.BaseActivity;
import com.bofa.mobile.util.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Book transfer between the customer's own accounts (core-banking POST /transfers).
 * Also opened from bofa://transfer?from=...&to=...&amount=... links in alerts and e-mails.
 */
public class TransferActivity extends BaseActivity {

    private static final String TAG = "TransferActivity";

    private List<Account> accounts;
    private Spinner fromSpinner;
    private Spinner toSpinner;
    private EditText amountField;
    private EditText memoField;
    private TextView errorText;
    private Button submitButton;
    private ProgressBar progress;
    private String idempotencyKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        fromSpinner = findViewById(R.id.transfer_from);
        toSpinner = findViewById(R.id.transfer_to);
        amountField = findViewById(R.id.transfer_amount);
        memoField = findViewById(R.id.transfer_memo);
        errorText = findViewById(R.id.transfer_error);
        submitButton = findViewById(R.id.transfer_submit);
        progress = findViewById(R.id.transfer_progress);

        accounts = AccountsCache.get();
        AccountSpinners.bind(this, fromSpinner, accounts);
        AccountSpinners.bind(this, toSpinner, accounts);
        if (accounts.size() > 1) {
            toSpinner.setSelection(1);
        }
        applyDeepLink(getIntent());

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                review();
            }
        });
    }

    private void applyDeepLink(Intent intent) {
        Uri data = intent.getData();
        if (data == null) {
            return;
        }
        int from = AccountSpinners.indexOf(accounts, data.getQueryParameter("from"));
        int to = AccountSpinners.indexOf(accounts, data.getQueryParameter("to"));
        if (from >= 0) {
            fromSpinner.setSelection(from);
        }
        if (to >= 0) {
            toSpinner.setSelection(to);
        }
        if (data.getQueryParameter("amount") != null) {
            amountField.setText(data.getQueryParameter("amount"));
        }
        if (data.getQueryParameter("memo") != null) {
            memoField.setText(data.getQueryParameter("memo"));
        }
    }

    private void review() {
        if (accounts.isEmpty()) {
            showError(errorText, getString(R.string.error_generic));
            return;
        }
        final Account from = accounts.get(fromSpinner.getSelectedItemPosition());
        final Account to = accounts.get(toSpinner.getSelectedItemPosition());
        if (from.getAccountNumber().equals(to.getAccountNumber())) {
            showError(errorText, getString(R.string.transfer_error_same));
            return;
        }
        final BigDecimal amount;
        try {
            amount = Money.parse(amountField.getText().toString());
        } catch (NumberFormatException e) {
            showError(errorText, getString(R.string.transfer_error_amount));
            return;
        }
        if (amount.signum() <= 0) {
            showError(errorText, getString(R.string.transfer_error_amount));
            return;
        }
        showError(errorText, null);

        new AlertDialog.Builder(this)
                .setTitle(R.string.transfer_confirm_title)
                .setMessage(getString(R.string.transfer_confirm_body, Money.format(amount),
                        from.getPickerLabel(), to.getPickerLabel()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.transfer_submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        submit(from, to, amount);
                    }
                })
                .show();
    }

    private void submit(Account from, Account to, final BigDecimal amount) {
        if (idempotencyKey == null) {
            idempotencyKey = UUID.randomUUID().toString();
        }
        TransferRequest request = new TransferRequest(from.getAccountNumber(), to.getAccountNumber(),
                Money.toWire(amount), memoField.getText().toString().trim(), idempotencyKey);
        Log.i(TAG, "POST /transfers from=" + from.getAccountNumber() + " to=" + to.getAccountNumber()
                + " amount=" + request.getAmount());

        setLoading(true);
        ApiClient.coreBanking().createTransfer(request).enqueue(new Callback<TransferResponse>() {
            @Override
            public void onResponse(Call<TransferResponse> call, Response<TransferResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    idempotencyKey = null;
                    showSuccess(amount, response.body());
                } else {
                    if (response.code() != 409) {
                        idempotencyKey = null;
                    }
                    showError(errorText, ApiErrors.userMessage(TransferActivity.this, response));
                }
            }

            @Override
            public void onFailure(Call<TransferResponse> call, Throwable t) {
                setLoading(false);
                showError(errorText, getString(R.string.error_network));
            }
        });
    }

    private void showSuccess(BigDecimal amount, TransferResponse body) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.transfer_success_title)
                .setMessage(getString(R.string.transfer_success_body, Money.format(amount), body.getTransferId()))
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
    }

    private void setLoading(boolean loading) {
        submitButton.setEnabled(!loading);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
