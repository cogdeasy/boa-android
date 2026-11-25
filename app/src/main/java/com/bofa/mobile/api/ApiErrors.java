package com.bofa.mobile.api;

import android.content.Context;

import com.google.gson.Gson;
import com.bofa.mobile.R;
import com.bofa.mobile.model.ApiError;

import okhttp3.ResponseBody;
import retrofit2.Response;

public final class ApiErrors {

    private ApiErrors() {
    }

    public static ApiError parse(Gson gson, Response<?> response) {
        ResponseBody body = response.errorBody();
        if (body == null) {
            return new ApiError("HTTP_" + response.code(), null);
        }
        try {
            ApiError error = gson.fromJson(body.charStream(), ApiError.class);
            return error != null ? error : new ApiError("HTTP_" + response.code(), null);
        } catch (Exception e) {
            return new ApiError("HTTP_" + response.code(), null);
        }
    }

    /** Customer-facing message for a failed call. */
    public static String userMessage(Context context, Response<?> response) {
        if (response.code() == 401) {
            return context.getString(R.string.error_session_expired);
        }
        ApiError error = parse(ApiClient.get().gson(), response);
        if (error.getMessage() != null) {
            return error.getMessage();
        }
        return context.getString(R.string.error_generic);
    }
}
