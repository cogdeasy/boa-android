package com.bofa.mobile;

import android.app.Application;

import com.bofa.mobile.api.ApiClient;
import com.bofa.mobile.session.SessionManager;

public class BofaApp extends Application {

    private static BofaApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        SessionManager.init(this);
        ApiClient.init(this);
    }

    public static BofaApp get() {
        return instance;
    }
}
