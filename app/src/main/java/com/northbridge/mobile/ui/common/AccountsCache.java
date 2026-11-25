package com.northbridge.mobile.ui.common;

import com.northbridge.mobile.model.Account;

import java.util.ArrayList;
import java.util.List;

/** Last accounts list loaded on the overview, reused by the money-movement screens. */
public final class AccountsCache {

    private static final List<Account> ACCOUNTS = new ArrayList<>();

    private AccountsCache() {
    }

    public static synchronized void set(List<Account> accounts) {
        ACCOUNTS.clear();
        if (accounts != null) {
            ACCOUNTS.addAll(accounts);
        }
    }

    public static synchronized List<Account> get() {
        return new ArrayList<>(ACCOUNTS);
    }

    public static synchronized Account find(String accountNumber) {
        for (Account a : ACCOUNTS) {
            if (a.getAccountNumber().equals(accountNumber)) {
                return a;
            }
        }
        return null;
    }

    public static synchronized void clear() {
        ACCOUNTS.clear();
    }
}
