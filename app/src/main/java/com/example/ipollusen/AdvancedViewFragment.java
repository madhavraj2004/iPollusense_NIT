package com.example.ipollusen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.ipollusen.R;

public class AdvancedViewFragment extends Fragment { // Extend Fragment instead of AppCompatActivity
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_advanced_view, container, false);

        WebView webView = view.findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); // Enable DOM storage
        webSettings.setLoadWithOverviewMode(true); // Load webpage in overview mode
        webSettings.setUseWideViewPort(true); // Enable viewport to fit the webpage

        // Load the URL of your MQTT broker page
        webView.loadUrl("https://madhavraj2004.github.io/mqtt-broker/");

        return view; // Return the inflated view
    }
}
