package com.bofa.mobile.ui.help;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.bofa.mobile.R;
import com.bofa.mobile.session.SessionManager;

/**
 * Help centre. Shows bundled content by default; marketing and servicing links open
 * bofa://help?url=... to show a specific article.
 */
public class HelpActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "url";
    private static final String DEFAULT_URL = "file:///android_asset/help/index.html";

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        WebView webView = findViewById(R.id.help_webview);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new HelpBridge(), "BoaBridge");

        webView.loadUrl(resolveUrl());
    }

    private String resolveUrl() {
        Uri data = getIntent().getData();
        if (data != null && data.getQueryParameter("url") != null) {
            return data.getQueryParameter("url");
        }
        String extra = getIntent().getStringExtra(EXTRA_URL);
        return extra != null ? extra : DEFAULT_URL;
    }

    /** Lets help articles personalise content and pre-fill the secure-message form. */
    static class HelpBridge {
        @JavascriptInterface
        public String getDisplayName() {
            return SessionManager.get().getDisplayName();
        }

        @JavascriptInterface
        public String getCif() {
            return SessionManager.get().getCif();
        }

        @JavascriptInterface
        public String getAuthorization() {
            return SessionManager.get().getAuthorizationHeader();
        }
    }
}
