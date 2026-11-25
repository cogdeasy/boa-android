package com.bofa.mobile.api;

import android.annotation.SuppressLint;
import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.bofa.mobile.BuildConfig;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Process-wide Retrofit clients for core-banking and the payments hub. */
public final class ApiClient {

    private static ApiClient instance;

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final CoreBankingService coreBanking;
    private final PaymentsService payments;

    private ApiClient(Context context) {
        gson = new GsonBuilder().create();

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor())
                .addInterceptor(logging);

        if (BuildConfig.ALLOW_INSECURE_TLS) {
            trustAllCertificates(builder);
        }

        BackendInterceptors.install(builder, context);
        httpClient = builder.build();

        coreBanking = new Retrofit.Builder()
                .baseUrl(BuildConfig.CORE_BANKING_BASE_URL)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(CoreBankingService.class);

        payments = new Retrofit.Builder()
                .baseUrl(BuildConfig.PAYMENTS_BASE_URL)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(PaymentsService.class);
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new ApiClient(context.getApplicationContext());
        }
    }

    public static ApiClient get() {
        if (instance == null) {
            throw new IllegalStateException("ApiClient.init() has not been called");
        }
        return instance;
    }

    public static CoreBankingService coreBanking() {
        return get().coreBanking;
    }

    public static PaymentsService payments() {
        return get().payments;
    }

    public Gson gson() {
        return gson;
    }

    // UAT and debug builds talk to environments fronted by internally-issued certificates.
    @SuppressLint({"TrustAllX509TrustManager", "BadHostnameVerifier"})
    private static void trustAllCertificates(OkHttpClient.Builder builder) {
        try {
            X509TrustManager trustAll = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustAll}, new SecureRandom());
            builder.sslSocketFactory(sslContext.getSocketFactory(), trustAll);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        } catch (Exception e) {
            throw new IllegalStateException("Unable to configure TLS", e);
        }
    }
}
