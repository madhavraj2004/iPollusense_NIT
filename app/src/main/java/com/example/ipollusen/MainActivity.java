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
import android.view.MenuItem;
import android.view.View;
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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String CHANNEL_ID = "sensor_notification_channel";
    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private AppBarConfiguration appBarConfiguration;
    private UserViewModel userViewModel;
    private NavController navController;

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

        // Set up navigation and navController
        setupNavigation();

        // Set up back arrow in nav drawer header after navigation is ready
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        ImageButton backArrowButton = headerView.findViewById(R.id.backArrowButton);

        // Ensure the button is clickable and visible
        backArrowButton.setVisibility(View.VISIBLE);
        backArrowButton.setEnabled(true);
        backArrowButton.setClickable(true);

        // BOTH behaviors: Go to home if not already there, and always close the drawer
        backArrowButton.setOnClickListener(v -> {
            if (navController != null &&
                    navController.getCurrentDestination() != null &&
                    navController.getCurrentDestination().getId() != R.id.navigation_home) {
                navController.popBackStack(R.id.navigation_home, false);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        // Notification channel and permissions
        createNotificationChannel();
        requestNotificationPermission();
        disableBatteryOptimizations();
    }

    private void setupNavigation() {
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);
        navController = navHostFragment.getNavController();

        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_map)
                .setOpenableLayout(drawerLayout)
                .build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav_view);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);
    }

    // Request Notification Permission (Android 13+)
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    // Create Notification Channel
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

    // Disable Battery Optimizations (Android 6+)
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
        } else if (item.getItemId() == R.id.nav_wifi) {
            openWifiConfigureFragment();
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
    private void openWifiConfigureFragment() {
        Navigation.findNavController(this, R.id.nav_host_fragment_activity_main)
                .navigate(R.id.nav_wifi);
    }
}