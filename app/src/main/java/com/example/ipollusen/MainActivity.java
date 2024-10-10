package com.example.ipollusen;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        // Check if the user is authenticated
        if (isUserNotAuthenticated()) {
            redirectToLoginActivity();
            return;
        }

        // Set up NavHostFragment for navigation
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main); // Use correct fragment ID here

        // Check if navHostFragment is not null
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            // You can set up the NavController if needed here
        } else {
            showErrorDialog("Navigation host fragment not found.");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(this::onAuthStateChanged);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(this::onAuthStateChanged);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isNetworkAvailable()) {
            showErrorDialog("No network connection available.");
        }
    }

    private void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        if (isUserNotAuthenticated()) {
            redirectToLoginActivity();
        }
    }

    private boolean isUserNotAuthenticated() {
        return mAuth.getCurrentUser() == null;
    }

    private void redirectToLoginActivity() {
        startActivity(new Intent(this, LoginActivity.class));
        finish(); // Finish MainActivity to prevent returning to it without logging in
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            } else {
                // For devices with API level < 23
                return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
            }
        }
        return false;
    }
}
