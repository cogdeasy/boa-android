package com.northbridge.mobile.model;

public class Payee {
    private String payeeId;
    private String name;
    private String category;
    private String accountRef;

    public Payee() {
    }

    public Payee(String payeeId, String name, String category, String accountRef) {
        this.payeeId = payeeId;
        this.name = name;
        this.category = category;
        this.accountRef = accountRef;
    }

    public String getPayeeId() { return payeeId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getAccountRef() { return accountRef; }

    @Override
    public String toString() {
        return name + " " + accountRef;
    }
}
