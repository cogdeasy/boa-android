package com.bofa.mobile.ui.common;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.bofa.mobile.model.Account;
import com.bofa.mobile.util.Money;

import java.util.List;

public final class AccountSpinners {

    private AccountSpinners() {
    }

    public static void bind(Context context, Spinner spinner, final List<Account> accounts) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
        for (Account a : accounts) {
            adapter.add(a.getPickerLabel() + "  (" + Money.format(a.getAvailableBalance()) + ")");
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    public static int indexOf(List<Account> accounts, String accountNumber) {
        if (accountNumber == null) {
            return -1;
        }
        for (int i = 0; i < accounts.size(); i++) {
            if (accountNumber.equals(accounts.get(i).getAccountNumber())) {
                return i;
            }
        }
        return -1;
    }
}
