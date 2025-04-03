package com.example.ipollusen;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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

        // Initialize ViewModel
        userViewModel = new UserViewModel();

        // Check authentication
        if (isUserNotAuthenticated()) {
            redirectToLoginActivity();
            return;
        }

        // Set up Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_person);

        // Handle notification button click
        ImageButton notificationButton = findViewById(R.id.notification_button);
        notificationButton.setOnClickListener(v -> openNotificationFragment());

        // Set up navigation
        setupNavigation();

        // Create notification channel
        createNotificationChannel();

        // Request notification permission (Android 13+)
        requestNotificationPermission();

        // Fetch FCM Token
        fetchFCMToken();

        // Handle battery optimization issues
        disableBatteryOptimizations();
    }



    private void setupNavigation() {
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);
        NavController navController = navHostFragment.getNavController();

        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_map)
                .setOpenableLayout(drawerLayout)
                .build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav_view);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);
    }

    // ✅ Step 1: Request Notification Permission (Android 13+)
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    // ✅ Step 2: Create Notification Channel
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sensor Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for sensor alerts");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // ✅ Step 3: Fetch and Store FCM Token
    private void fetchFCMToken() {
        Log.d("FCM_DEBUG", "Fetching FCM token...");

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM_DEBUG", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    String newToken = task.getResult();
                    Log.d("FCM_DEBUG", "New FCM Token: " + newToken);

                    if (newToken == null || newToken.isEmpty()) {
                        Log.e("FCM_DEBUG", "FCM Token is null or empty!");
                        return;
                    }

                    // Fetch stored token
                    String storedToken = getStoredFCMToken();
                    Log.d("FCM_DEBUG", "Stored FCM Token: " + storedToken);

                    if (!newToken.equals(storedToken)) {
                        saveFCMToken(newToken);
                        Log.d("FCM_DEBUG", "FCM Token updated and saved.");
                    } else {
                        Log.d("FCM_DEBUG", "FCM Token remains the same, not updating.");
                    }
                });
    }

    private void saveFCMToken(String token) {
        // Save token locally in SharedPreferences
        getSharedPreferences("FCM_PREF", MODE_PRIVATE)
                .edit()
                .putString("fcm_token", token)
                .apply();

        // Get the current user ID from ViewModel
        String userId = userViewModel.getUserId().getValue();
        if (userId != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db.collection("users").document(userId);

            // Update only the FCM token field
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("fcm_token", token);

            // Update Firestore with the new FCM token
            userRef.update(updateData)
                    .addOnSuccessListener(aVoid -> Log.d("FCM_DEBUG", "FCM token updated in Firestore"))
                    .addOnFailureListener(e -> Log.e("FCM_DEBUG", "Error updating FCM token", e));
        } else {
            Log.e("FCM_DEBUG", "User ID is null, cannot update FCM token in Firestore.");
        }
    }

    private String getStoredFCMToken() {
        return getSharedPreferences("FCM_PREF", MODE_PRIVATE)
                .getString("fcm_token", "");
    }


    // ✅ Step 4: Disable Battery Optimizations (Android 14+)
    private void disableBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
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
        if (item.getItemId() == android.R.id.home) {
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
        if (item.getItemId() == R.id.nav_logout) {
            handleLogout();
        } else {
            navController.navigate(item.getItemId());
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void openNotificationFragment() {
        Navigation.findNavController(this, R.id.nav_host_fragment_activity_main)
                .navigate(R.id.navigation_notification);
    }
}
