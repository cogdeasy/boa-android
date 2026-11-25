package com.northbridge.mobile.model;

public class P2pPaymentRequest {
    private String cif;
    private String fromAccount;
    private Recipient recipient;
    private String amount;
    private String memo;

    public P2pPaymentRequest(String cif, String fromAccount, Recipient recipient, String amount, String memo) {
        this.cif = cif;
        this.fromAccount = fromAccount;
        this.recipient = recipient;
        this.amount = amount;
        this.memo = memo;
    }

    public String getCif() { return cif; }
    public String getFromAccount() { return fromAccount; }
    public Recipient getRecipient() { return recipient; }
    public String getAmount() { return amount; }
    public String getMemo() { return memo; }

    public static class Recipient {
        private String email;
        private String phone;

        public static Recipient email(String email) {
            Recipient r = new Recipient();
            r.email = email;
            return r;
        }

        public static Recipient phone(String phone) {
            Recipient r = new Recipient();
            r.phone = phone;
            return r;
        }

        public String getEmail() { return email; }
        public String getPhone() { return phone; }

        public String display() {
            return email != null ? email : phone;
        }
    }
}
