package com.bofa.mobile.model;

public class Payment {
    private String paymentId;
    private String type;
    private String fromAccount;
    private String payeeName;
    private String amount;
    private String status;
    private String createdAt;

    public Payment() {
    }

    public Payment(String paymentId, String type, String fromAccount, String payeeName, String amount,
                   String status, String createdAt) {
        this.paymentId = paymentId;
        this.type = type;
        this.fromAccount = fromAccount;
        this.payeeName = payeeName;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getPaymentId() { return paymentId; }
    public String getType() { return type; }
    public String getFromAccount() { return fromAccount; }
    public String getPayeeName() { return payeeName; }
    public String getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}
