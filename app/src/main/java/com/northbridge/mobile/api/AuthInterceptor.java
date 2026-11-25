package com.northbridge.mobile.api;

import com.northbridge.mobile.BuildConfig;
import com.northbridge.mobile.session.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** Adds HTTP Basic credentials and the mobile client headers to every call. */
public class AuthInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder()
                .header("X-NB-Client-Id", BuildConfig.NB_CLIENT_ID)
                .header("X-NB-Client-Key", BuildConfig.NB_CLIENT_KEY)
                .header("X-NB-App-Version", BuildConfig.VERSION_NAME)
                .header("Accept", "application/json");

        String authorization = SessionManager.get().getAuthorizationHeader();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        Response response = chain.proceed(builder.build());
        if (response.code() == 401) {
            SessionManager.get().markExpired();
        }
        return response;
    }
}
