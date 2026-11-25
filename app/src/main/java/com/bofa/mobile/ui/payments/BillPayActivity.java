package com.bofa.mobile.ui.payments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bofa.mobile.R;
import com.bofa.mobile.api.ApiClient;
import com.bofa.mobile.api.ApiErrors;
import com.bofa.mobile.model.Account;
import com.bofa.mobile.model.BillPaymentRequest;
import com.bofa.mobile.model.Payee;
import com.bofa.mobile.model.PaymentResponse;
import com.bofa.mobile.session.SessionManager;
import com.bofa.mobile.ui.common.AccountSpinners;
import com.bofa.mobile.ui.common.AccountsCache;
import com.bofa.mobile.ui.common.BaseActivity;
import com.bofa.mobile.util.BankDates;
import com.bofa.mobile.util.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BillPayActivity extends BaseActivity {

    private final List<Payee> payees = new ArrayList<>();
    private List<Account> accounts;
    private Spinner payeeSpinner;
    private Spinner fromSpinner;
    private EditText amountField;
    private EditText sendOnField;
    private TextView errorText;
    private Button submitButton;
    private ProgressBar progress;
    private String sendOn = BankDates.today();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_pay);

        payeeSpinner = findViewById(R.id.bill_payee);
        fromSpinner = findViewById(R.id.bill_from);
        amountField = findViewById(R.id.bill_amount);
        sendOnField = findViewById(R.id.bill_send_on);
        errorText = findViewById(R.id.bill_error);
        submitButton = findViewById(R.id.bill_submit);
        progress = findViewById(R.id.bill_progress);

        accounts = AccountsCache.get();
        AccountSpinners.bind(this, fromSpinner, accounts);
        sendOnField.setText(BankDates.display(sendOn));
        sendOnField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickDate();
            }
        });
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });
        loadPayees();
    }

    private void loadPayees() {
        ApiClient.payments().getPayees(SessionManager.get().getCif()).enqueue(new Callback<List<Payee>>() {
            @Override
            public void onResponse(Call<List<Payee>> call, Response<List<Payee>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    showError(errorText, ApiErrors.userMessage(BillPayActivity.this, response));
                    return;
                }
                payees.clear();
                payees.addAll(response.body());
                ArrayAdapter<Payee> adapter = new ArrayAdapter<>(BillPayActivity.this,
                        android.R.layout.simple_spinner_item, payees);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                payeeSpinner.setAdapter(adapter);
            }

            @Override
            public void onFailure(Call<List<Payee>> call, Throwable t) {
                showError(errorText, getString(R.string.error_network));
            }
        });
    }

    private void pickDate() {
        Calendar c = Calendar.getInstance(BankDates.BANK_TZ);
        DatePickerDialog dialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                sendOn = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day);
                sendOnField.setText(BankDates.display(sendOn));
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(c.getTimeInMillis());
        dialog.show();
    }

    private void submit() {
        if (payees.isEmpty() || accounts.isEmpty()) {
            showError(errorText, getString(R.string.error_generic));
            return;
        }
        BigDecimal amount;
        try {
            amount = Money.parse(amountField.getText().toString());
        } catch (NumberFormatException e) {
            amount = BigDecimal.ZERO;
        }
        if (amount.signum() <= 0) {
            showError(errorText, getString(R.string.transfer_error_amount));
            return;
        }
        showError(errorText, null);
        Payee payee = payees.get(payeeSpinner.getSelectedItemPosition());
        Account from = accounts.get(fromSpinner.getSelectedItemPosition());
        BillPaymentRequest request = new BillPaymentRequest(SessionManager.get().getCif(), from.getAccountNumber(),
                payee.getPayeeId(), Money.toWire(amount), sendOn);

        setLoading(true);
        ApiClient.payments().payBill(request).enqueue(new Callback<PaymentResponse>() {
            @Override
            public void onResponse(Call<PaymentResponse> call, Response<PaymentResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(BillPayActivity.this, getString(R.string.bill_success,
                            response.body().getPaymentId(), BankDates.display(sendOn)), Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    showError(errorText, ApiErrors.userMessage(BillPayActivity.this, response));
                }
            }

            @Override
            public void onFailure(Call<PaymentResponse> call, Throwable t) {
                setLoading(false);
                showError(errorText, getString(R.string.error_network));
            }
        });
    }

    private void setLoading(boolean loading) {
        submitButton.setEnabled(!loading);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
