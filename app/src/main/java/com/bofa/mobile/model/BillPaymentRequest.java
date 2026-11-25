package com.bofa.mobile.model;

public class BillPaymentRequest {
    private String cif;
    private String fromAccount;
    private String payeeId;
    private String amount;
    private String sendOn;

    public BillPaymentRequest(String cif, String fromAccount, String payeeId, String amount, String sendOn) {
        this.cif = cif;
        this.fromAccount = fromAccount;
        this.payeeId = payeeId;
        this.amount = amount;
        this.sendOn = sendOn;
    }

    public String getCif() { return cif; }
    public String getFromAccount() { return fromAccount; }
    public String getPayeeId() { return payeeId; }
    public String getAmount() { return amount; }
    public String getSendOn() { return sendOn; }
}
