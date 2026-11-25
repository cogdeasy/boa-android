package com.bofa.mobile.model;

public class Customer {
    private String cif;
    private String firstName;
    private String lastName;
    private String email;
    private String segment;
    private String kycStatus;

    public Customer() {
    }

    public Customer(String cif, String firstName, String lastName, String email, String segment, String kycStatus) {
        this.cif = cif;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.segment = segment;
        this.kycStatus = kycStatus;
    }

    public String getCif() { return cif; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getSegment() { return segment; }
    public String getKycStatus() { return kycStatus; }

    public String getDisplayName() {
        return firstName + " " + lastName;
    }
}
