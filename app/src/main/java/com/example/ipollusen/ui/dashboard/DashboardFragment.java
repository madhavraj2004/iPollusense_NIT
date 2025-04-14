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
import androidx.navigation.Navigation;

import com.example.ipollusen.R;
import com.google.android.material.card.MaterialCardView;

public class DashboardFragment extends Fragment {

    private static final String WEBPAGE_URL = "https://ipollusense-annotate.web.app";
    private static final String WEBSITE_URL = "http://52.250.54.24:4050/";

    private WebView webView;
    private MaterialCardView annotationCard, recommendationCard, websiteCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        annotationCard = view.findViewById(R.id.annotationCard);
        recommendationCard = view.findViewById(R.id.recommendationCard);
        websiteCard = view.findViewById(R.id.websiteCard);  // New Card
        webView = view.findViewById(R.id.webViewGoogleForm);

        if (annotationCard != null) {
            annotationCard.setOnClickListener(v -> loadUrlInWebView(WEBPAGE_URL));
        }

        if (websiteCard != null) {
            websiteCard.setOnClickListener(v -> loadUrlInWebView(WEBSITE_URL));
        }

        if (recommendationCard != null) {
            recommendationCard.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.action_navigation_dashboard_to_recommendationFragment)
            );
        }

        return view;
    }

    private void loadUrlInWebView(String url) {
        if (webView != null) {
            webView.setVisibility(View.VISIBLE);
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webView.setWebViewClient(new WebViewClient());
            webView.loadUrl(url);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webView != null) {
            webView.destroy();
        }
    }
}
