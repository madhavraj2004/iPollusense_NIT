package com.example.ipollusen;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String CHANNEL_ID = "sensor_notification_channel";
    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private AppBarConfiguration appBarConfiguration;
    private UserViewModel userViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        // Initialize UserViewModel
        userViewModel = new UserViewModel();

        // Check if the user is authenticated
        if (isUserNotAuthenticated()) {
            redirectToLoginActivity();
            return;
        }

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_person);

        // Set up custom buttons in the toolbar
        ImageButton notificationButton = findViewById(R.id.notification_button);
        notificationButton.setOnClickListener(v -> {
            Log.d("MainActivity", "Notification button clicked, opening notification fragment");
            openNotificationFragment();
        });

        // Set up DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Set up NavHostFragment for navigation
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);
        NavController navController = navHostFragment.getNavController();

        // Set up AppBarConfiguration for navigation
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_map)
                .setOpenableLayout(drawerLayout)
                .build();

        // Set up NavigationUI with the toolbar and NavController
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Set up BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav_view);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);


        // Fetch the FCM token and store it in Firestore
        fetchFCMToken();
    }

    private void fetchFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("MainActivity", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get the new FCM token
                    String newToken = task.getResult();
                    Log.d("MainActivity", "FCM Token: " + newToken);

                    // Fetch the last stored token from Firestore or SharedPreferences
                    String storedToken = getStoredFCMToken();

                    // Save only if the token has changed
                    if (!newToken.equals(storedToken)) {
                        saveFCMToken(newToken);
                        Log.d("MainActivity", "FCM Token updated and saved.");
                    } else {
                        Log.d("MainActivity", "FCM Token remains the same, not updating.");
                    }
                });
    }

    // Save the FCM token to SharedPreferences and Firestore
    private void saveFCMToken(String token) {
        getSharedPreferences("FCM_PREF", MODE_PRIVATE)
                .edit()
                .putString("fcm_token", token)
                .apply();

        if (userViewModel.getUserId().getValue() != null) {
            userViewModel.saveFCMToken(token);
        }
    }

    // Retrieve the stored FCM token
    private String getStoredFCMToken() {
        return getSharedPreferences("FCM_PREF", MODE_PRIVATE)
                .getString("fcm_token", "");
    }


    private boolean isUserNotAuthenticated() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        return currentUser == null;
    }

    private void redirectToLoginActivity() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void handleLogout() {
        mAuth.signOut();
        redirectToLoginActivity();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int backButtonId = getResources().getIdentifier("ic_baseline_arrow_back_24", "drawable", getPackageName());
        if (item.getItemId() == backButtonId) {
            toggleDrawer();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            toggleDrawer();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        int itemId = item.getItemId();

        if (itemId == R.id.navigation_home) {
            navController.navigate(R.id.navigation_home);
        } else if (itemId == R.id.navigation_dashboard) {
            navController.navigate(R.id.navigation_dashboard);
        } else if (itemId == R.id.navigation_map) {
            navController.navigate(R.id.navigation_map);
        } else if (itemId == R.id.nav_settings) {
            navController.navigate(R.id.nav_settings);
        } else if (itemId == R.id.nav_logout) {
            handleLogout();
        } else {
            return false;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // Method to open the NotificationFragment
    private void openNotificationFragment() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        navController.navigate(R.id.navigation_notification); // Replace with actual ID of NotificationFragment
    }
}
