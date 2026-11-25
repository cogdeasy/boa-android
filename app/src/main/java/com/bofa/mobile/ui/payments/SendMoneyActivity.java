package com.bofa.mobile.ui.payments;

import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.bofa.mobile.R;
import com.bofa.mobile.api.ApiClient;
import com.bofa.mobile.api.ApiErrors;
import com.bofa.mobile.model.Account;
import com.bofa.mobile.model.P2pPaymentRequest;
import com.bofa.mobile.model.PaymentResponse;
import com.bofa.mobile.session.SessionManager;
import com.bofa.mobile.ui.common.AccountSpinners;
import com.bofa.mobile.ui.common.AccountsCache;
import com.bofa.mobile.ui.common.BaseActivity;
import com.bofa.mobile.util.Money;

import java.math.BigDecimal;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Person-to-person payments through the payments hub (POST /p2p). */
public class SendMoneyActivity extends BaseActivity {

    private List<Account> accounts;
    private RadioGroup typeGroup;
    private TextInputLayout recipientLayout;
    private EditText recipientField;
    private Spinner fromSpinner;
    private EditText amountField;
    private EditText memoField;
    private TextView errorText;
    private Button submitButton;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_money);

        typeGroup = findViewById(R.id.p2p_type);
        recipientLayout = findViewById(R.id.p2p_recipient_layout);
        recipientField = findViewById(R.id.p2p_recipient);
        fromSpinner = findViewById(R.id.p2p_from);
        amountField = findViewById(R.id.p2p_amount);
        memoField = findViewById(R.id.p2p_memo);
        errorText = findViewById(R.id.p2p_error);
        submitButton = findViewById(R.id.p2p_submit);
        progress = findViewById(R.id.p2p_progress);

        accounts = AccountsCache.get();
        AccountSpinners.bind(this, fromSpinner, accounts);

        typeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                boolean email = checkedId == R.id.p2p_type_email;
                recipientLayout.setHint(getString(email ? R.string.p2p_recipient_email : R.string.p2p_recipient_phone));
                recipientField.setInputType(email ? InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | InputType.TYPE_CLASS_TEXT
                        : InputType.TYPE_CLASS_PHONE);
                recipientField.setText("");
            }
        });
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });
    }

    private void submit() {
        boolean email = typeGroup.getCheckedRadioButtonId() == R.id.p2p_type_email;
        String recipient = recipientField.getText().toString().trim();
        if (email ? !Patterns.EMAIL_ADDRESS.matcher(recipient).matches()
                : recipient.replaceAll("[^0-9]", "").length() != 10) {
            showError(errorText, getString(R.string.p2p_error_recipient));
            return;
        }
        BigDecimal amount;
        try {
            amount = Money.parse(amountField.getText().toString());
        } catch (NumberFormatException e) {
            amount = BigDecimal.ZERO;
        }
        if (amount.signum() <= 0 || accounts.isEmpty()) {
            showError(errorText, getString(R.string.transfer_error_amount));
            return;
        }
        showError(errorText, null);

        Account from = accounts.get(fromSpinner.getSelectedItemPosition());
        P2pPaymentRequest.Recipient r = email ? P2pPaymentRequest.Recipient.email(recipient)
                : P2pPaymentRequest.Recipient.phone(recipient.replaceAll("[^0-9]", ""));
        final String display = Money.format(amount);
        P2pPaymentRequest request = new P2pPaymentRequest(SessionManager.get().getCif(), from.getAccountNumber(), r,
                Money.toWire(amount), memoField.getText().toString().trim());

        setLoading(true);
        ApiClient.payments().sendP2p(request).enqueue(new Callback<PaymentResponse>() {
            @Override
            public void onResponse(Call<PaymentResponse> call, Response<PaymentResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(SendMoneyActivity.this,
                            getString(R.string.p2p_success, display, response.body().getPaymentId()),
                            Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    showError(errorText, ApiErrors.userMessage(SendMoneyActivity.this, response));
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
