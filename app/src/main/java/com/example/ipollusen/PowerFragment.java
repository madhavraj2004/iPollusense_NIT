package com.example.ipollusen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PowerFragment extends Fragment {

    private WebView webView;

    public PowerFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_power, container, false);
        webView = view.findViewById(R.id.webview_power);

        setupWebView();

        return view;
    }

    private void setupWebView() {
        webView.setWebViewClient(new WebViewClient()); // open links inside WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // enable JavaScript if needed

        webView.loadUrl("http://52.250.54.24:4050/power"); // your URL here
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
    }
}
