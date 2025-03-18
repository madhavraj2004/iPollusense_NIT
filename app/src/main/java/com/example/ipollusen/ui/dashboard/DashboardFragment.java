package com.example.ipollusen.ui.dashboard;

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

import com.example.ipollusen.R;

public class DashboardFragment extends Fragment {

    private static final String WEBPAGE_URL = "https://ipollusense-annotate.web.app"; // Updated URL

    private WebView webView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Initialize the WebView
        webView = view.findViewById(R.id.webViewGoogleForm);

        if (webView != null) {
            // Enable JavaScript for the WebView
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);

            // Set WebViewClient to open links within the WebView
            webView.setWebViewClient(new WebViewClient());

            // Load the new webpage URL
            webView.loadUrl(WEBPAGE_URL); // Updated URL
        } else {
            // Log or handle the case where WebView is not found
            throw new IllegalStateException("WebView with ID webViewGoogleForm not found in fragment_dashboard layout.");
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Ensure proper WebView cleanup
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
    }
}
