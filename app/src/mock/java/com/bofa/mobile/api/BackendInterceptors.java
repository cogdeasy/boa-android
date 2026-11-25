package com.bofa.mobile.api;

import android.content.Context;

import com.bofa.mobile.mock.MockBackendInterceptor;

import okhttp3.OkHttpClient;

/** Mock flavour: core-banking and the payments hub are served in-process from seed data. */
public final class BackendInterceptors {

    private BackendInterceptors() {
    }

    public static void install(OkHttpClient.Builder builder, Context context) {
        builder.addInterceptor(new MockBackendInterceptor());
    }
}
