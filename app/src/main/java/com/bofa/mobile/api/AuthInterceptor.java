package com.bofa.mobile.api;

import com.bofa.mobile.BuildConfig;
import com.bofa.mobile.session.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** Adds HTTP Basic credentials and the mobile client headers to every call. */
public class AuthInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder()
                .header("X-BOA-Client-Id", BuildConfig.BOA_CLIENT_ID)
                .header("X-BOA-Client-Key", BuildConfig.BOA_CLIENT_KEY)
                .header("X-BOA-App-Version", BuildConfig.VERSION_NAME)
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
