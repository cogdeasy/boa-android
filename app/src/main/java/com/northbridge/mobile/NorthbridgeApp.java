package com.northbridge.mobile;

import android.app.Application;

import com.northbridge.mobile.api.ApiClient;
import com.northbridge.mobile.session.SessionManager;

public class NorthbridgeApp extends Application {

    private static NorthbridgeApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        SessionManager.init(this);
        ApiClient.init(this);
    }

    public static NorthbridgeApp get() {
        return instance;
    }
}
