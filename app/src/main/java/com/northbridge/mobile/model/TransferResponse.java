package com.northbridge.mobile.model;

public class TransferResponse {
    private String transferId;
    private String status;
    private String postedAt;
    private String businessDate;

    public TransferResponse() {
    }

    public TransferResponse(String transferId, String status, String postedAt, String businessDate) {
        this.transferId = transferId;
        this.status = status;
        this.postedAt = postedAt;
        this.businessDate = businessDate;
    }

    public String getTransferId() { return transferId; }
    public String getStatus() { return status; }
    public String getPostedAt() { return postedAt; }
    public String getBusinessDate() { return businessDate; }
}
