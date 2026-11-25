package com.bofa.mobile.model;

public class TransferRequest {
    private String fromAccount;
    private String toAccount;
    private String amount;
    private String memo;
    private String idempotencyKey;

    public TransferRequest() {
    }

    public TransferRequest(String fromAccount, String toAccount, String amount, String memo, String idempotencyKey) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.memo = memo;
        this.idempotencyKey = idempotencyKey;
    }

    public String getFromAccount() { return fromAccount; }
    public String getToAccount() { return toAccount; }
    public String getAmount() { return amount; }
    public String getMemo() { return memo; }
    public String getIdempotencyKey() { return idempotencyKey; }
}
