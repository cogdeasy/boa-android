package com.bofa.mobile.api;

import android.content.Context;

import okhttp3.OkHttpClient;

/** Live flavour: requests go to the configured core-banking and payments hosts unchanged. */
public final class BackendInterceptors {

    private BackendInterceptors() {
    }

    public static void install(OkHttpClient.Builder builder, Context context) {
        // no-op
    }
}
