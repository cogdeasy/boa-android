package com.bofa.mobile.model;

public class Transaction {
    private long id;
    private String accountNumber;
    private String postedAt;
    private String businessDate;
    private String type;
    private String channel;
    private String amount;
    private String runningBalance;
    private String description;
    private String status;

    public Transaction() {
    }

    public Transaction(long id, String accountNumber, String postedAt, String businessDate, String type,
                       String channel, String amount, String runningBalance, String description, String status) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.postedAt = postedAt;
        this.businessDate = businessDate;
        this.type = type;
        this.channel = channel;
        this.amount = amount;
        this.runningBalance = runningBalance;
        this.description = description;
        this.status = status;
    }

    public long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getPostedAt() { return postedAt; }
    public String getBusinessDate() { return businessDate; }
    public String getType() { return type; }
    public String getChannel() { return channel; }
    public String getAmount() { return amount; }
    public String getRunningBalance() { return runningBalance; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }

    public boolean isCredit() {
        return "CREDIT".equals(type);
    }
}
