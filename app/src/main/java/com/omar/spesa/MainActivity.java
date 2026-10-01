package com.omar.spesa;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.view.Gravity;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getWindow().setStatusBarColor(Color.rgb(11,13,16));
            getWindow().setNavigationBarColor(Color.rgb(11,13,16));

            webView = new WebView(getApplicationContext());
            WebSettings s = webView.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setAllowFileAccess(true);
            s.setAllowContentAccess(true);
            s.setBuiltInZoomControls(false);
            s.setDisplayZoomControls(false);

            webView.setWebViewClient(new WebViewClient());
            webView.setWebChromeClient(new WebChromeClient());
            webView.setBackgroundColor(Color.rgb(11,13,16));
            setContentView(webView);
            webView.loadUrl("file:///android_asset/index.html");
        } catch (Throwable e) {
            TextView error = new TextView(this);
            error.setText("Spesa non riesce ad avviarsi.\n\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
            error.setTextColor(Color.WHITE);
            error.setBackgroundColor(Color.rgb(11,13,16));
            error.setTextSize(16);
            error.setGravity(Gravity.CENTER);
            error.setPadding(40,40,40,40);
            setContentView(error);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }
}
