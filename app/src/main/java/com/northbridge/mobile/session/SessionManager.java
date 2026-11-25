package com.northbridge.mobile.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.northbridge.mobile.model.Customer;

import java.nio.charset.Charset;

/**
 * Holds the signed-in customer and the credentials used for HTTP Basic auth against core-banking.
 * "Remember me" persists the Online ID and passcode so the customer is not prompted on every launch.
 */
public class SessionManager {

    private static final String PREFS = "nb_session";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER = "remember";
    private static final String KEY_CIF = "cif";
    private static final String KEY_DISPLAY_NAME = "display_name";

    private static SessionManager instance;

    private final SharedPreferences prefs;
    private String username;
    private String password;
    private Customer customer;
    private boolean expired;

    SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_REMEMBER, false)) {
            username = prefs.getString(KEY_USERNAME, null);
            password = prefs.getString(KEY_PASSWORD, null);
        }
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
    }

    public static SessionManager get() {
        return instance;
    }

    public void setCredentials(String username, String password, boolean remember) {
        this.username = username;
        this.password = password;
        this.expired = false;
        SharedPreferences.Editor editor = prefs.edit().putBoolean(KEY_REMEMBER, remember);
        if (remember) {
            editor.putString(KEY_USERNAME, username).putString(KEY_PASSWORD, password);
        } else {
            editor.remove(KEY_USERNAME).remove(KEY_PASSWORD);
        }
        editor.apply();
    }

    public void startSession(Customer customer) {
        this.customer = customer;
        prefs.edit()
                .putString(KEY_CIF, customer.getCif())
                .putString(KEY_DISPLAY_NAME, customer.getDisplayName())
                .apply();
    }

    public String getAuthorizationHeader() {
        if (username == null || password == null) {
            return null;
        }
        String raw = username + ":" + password;
        return "Basic " + Base64.encodeToString(raw.getBytes(Charset.forName("UTF-8")), Base64.NO_WRAP);
    }

    public String getRememberedUsername() {
        return prefs.getBoolean(KEY_REMEMBER, false) ? prefs.getString(KEY_USERNAME, null) : null;
    }

    public String getRememberedPassword() {
        return prefs.getBoolean(KEY_REMEMBER, false) ? prefs.getString(KEY_PASSWORD, null) : null;
    }

    public boolean isRemembered() {
        return prefs.getBoolean(KEY_REMEMBER, false);
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getCif() {
        return customer != null ? customer.getCif() : prefs.getString(KEY_CIF, null);
    }

    public String getDisplayName() {
        return customer != null ? customer.getDisplayName() : prefs.getString(KEY_DISPLAY_NAME, "");
    }

    public boolean isSignedIn() {
        return customer != null && !expired;
    }

    public void markExpired() {
        expired = true;
    }

    public void signOut() {
        customer = null;
        if (!isRemembered()) {
            username = null;
            password = null;
        }
        prefs.edit().remove(KEY_CIF).remove(KEY_DISPLAY_NAME).apply();
    }
}
