package com.example.ipollusen.ui.assistant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;


import com.example.ipollusen.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Locale;

public class AssistantFragment extends Fragment {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    private Button tapToSpeakButton;
    private TextView speechToTextView;
    private SpeechRecognizer speechRecognizer;
    private WebView webView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_assistant, container, false);

        tapToSpeakButton = view.findViewById(R.id.button_tap_to_speak);
        speechToTextView = view.findViewById(R.id.edittext_speech_to_text);

        // Initialize WebView and hide it initially
        webView = view.findViewById(R.id.webview_chatbot); // Add a WebView in XML layout if not done already
        webView.setWebViewClient(new WebViewClient());
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setVisibility(View.GONE); // Initially hide the WebView

        tapToSpeakButton.setOnClickListener(v -> {
            if (permissionToRecordAccepted) {
                startListening();
            } else {
                Toast.makeText(getContext(), "Recording permission is required", Toast.LENGTH_SHORT).show();
                requestAudioPermission();
            }
        });

        // Set up FloatingActionButton to show WebView
        FloatingActionButton fab = view.findViewById(R.id.fab_assistant);
        fab.setOnClickListener(v -> {
            webView.setVisibility(View.VISIBLE);
            webView.loadUrl("https://madhavraj2004.github.io/chatbot-integration/");
        });

        return view;
    }

    private void checkPermissions() {
        permissionToRecordAccepted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        if (!permissionToRecordAccepted) {
            requestAudioPermission();
        }
    }

    private void requestAudioPermission() {
        ActivityCompat.requestPermissions(requireActivity(), permissions, REQUEST_RECORD_AUDIO_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!permissionToRecordAccepted) {
                Toast.makeText(getContext(), "Recording permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Toast.makeText(getContext(), "Listening...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBeginningOfSpeech() { }

            @Override
            public void onRmsChanged(float rmsdB) { }

            @Override
            public void onBufferReceived(byte[] buffer) { }

            @Override
            public void onEndOfSpeech() {
                Toast.makeText(getContext(), "Processing...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int error) {
                Toast.makeText(getContext(), "Error recognizing speech", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    speechToTextView.setText(matches.get(0));
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) { }

            @Override
            public void onEvent(int eventType, Bundle params) { }
        });

        speechRecognizer.startListening(intent);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}
