package com.bofa.mobile.model;

/** Error body returned by core-banking and the payments hub, e.g. {"code":"NSF","message":"..."} */
public class ApiError {
    public static final String NSF = "NSF";
    public static final String LIMIT_EXCEEDED = "LIMIT_EXCEEDED";
    public static final String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
    public static final String DUPLICATE = "DUPLICATE";

    private String code;
    private String message;

    public ApiError() {
    }

    public ApiError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
}
