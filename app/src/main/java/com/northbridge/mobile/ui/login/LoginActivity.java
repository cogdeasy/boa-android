package com.northbridge.mobile.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.northbridge.mobile.BuildConfig;
import com.northbridge.mobile.R;
import com.northbridge.mobile.api.ApiClient;
import com.northbridge.mobile.model.Customer;
import com.northbridge.mobile.session.SessionManager;
import com.northbridge.mobile.ui.accounts.AccountsActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText usernameField;
    private EditText passwordField;
    private CheckBox rememberBox;
    private TextView errorText;
    private Button submitButton;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameField = findViewById(R.id.login_username);
        passwordField = findViewById(R.id.login_password);
        rememberBox = findViewById(R.id.login_remember);
        errorText = findViewById(R.id.login_error);
        submitButton = findViewById(R.id.login_submit);
        progress = findViewById(R.id.login_progress);
        ((TextView) findViewById(R.id.login_environment)).setText(
                BuildConfig.ENVIRONMENT_LABEL + "  |  v" + BuildConfig.VERSION_NAME);

        SessionManager session = SessionManager.get();
        if (session.isRemembered()) {
            usernameField.setText(session.getRememberedUsername());
            passwordField.setText(session.getRememberedPassword());
            rememberBox.setChecked(true);
        }

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });
    }

    void attemptLogin() {
        String username = usernameField.getText().toString().trim();
        String password = passwordField.getText().toString();
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            showError(getString(R.string.login_error_required));
            return;
        }
        showError(null);
        setLoading(true);
        Log.d(TAG, "Signing in " + username + " remember=" + rememberBox.isChecked());

        SessionManager.get().setCredentials(username, password, rememberBox.isChecked());
        ApiClient.coreBanking().getCurrentCustomer().enqueue(new Callback<Customer>() {
            @Override
            public void onResponse(Call<Customer> call, Response<Customer> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    SessionManager.get().startSession(response.body());
                    startActivity(new Intent(LoginActivity.this, AccountsActivity.class));
                    finish();
                } else if (response.code() == 401) {
                    showError(getString(R.string.login_error_invalid));
                } else {
                    showError(getString(R.string.error_generic));
                }
            }

            @Override
            public void onFailure(Call<Customer> call, Throwable t) {
                setLoading(false);
                Log.e(TAG, "Login failed", t);
                showError(getString(R.string.error_network));
            }
        });
    }

    private void setLoading(boolean loading) {
        submitButton.setEnabled(!loading);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(message == null ? View.GONE : View.VISIBLE);
    }
}
