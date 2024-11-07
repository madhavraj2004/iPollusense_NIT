package com.example.ipollusen;

import static android.app.PendingIntent.getActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Import FAB
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent serviceIntent = new Intent(this, BluetoothService.class);
        startService(serviceIntent);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        // Check if the user is authenticated
        if (isUserNotAuthenticated()) {
            redirectToLoginActivity();
            return;
        }

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configure toolbar to allow toggling the drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_person); // Changed to a menu icon

        // Set up custom buttons in the toolbar
        ImageButton userButton = findViewById(R.id.user_button);
        ImageButton notificationButton = findViewById(R.id.notification_button);
        ImageButton batteryIcon = findViewById(R.id.battery_icon);

        // Set up click listeners for toolbar icons
        userButton.setOnClickListener(v -> toggleDrawer());
        notificationButton.setOnClickListener(v -> showErrorDialog("Notification button clicked!"));
        batteryIcon.setOnClickListener(v -> showErrorDialog("Battery icon clicked!"));

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
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_assistant, R.id.navigation_map)
                .setOpenableLayout(drawerLayout)
                .build();

        // Set up NavigationUI with the toolbar and NavController
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Set up BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav_view);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        // Setup FAB to open BluetoothFragment


        // Close the drawer by default
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Get the back button ID from the drawable
        int backButtonId = getResources().getIdentifier("ic_baseline_arrow_back_24", "drawable", getPackageName());

        // Check if the clicked item is the back button or the home button
        if (item.getItemId() == backButtonId) {  // Check for the back button ID
            toggleDrawer();  // Toggle drawer on back button click
            return true;
        } else if (item.getItemId() == android.R.id.home) {  // Handle the default home button action (if needed)
            toggleDrawer();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
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

        // Handle navigation items
        if (itemId == R.id.navigation_home) {
            navController.navigate(R.id.navigation_home);
        } else if (itemId == R.id.navigation_dashboard) {
            navController.navigate(R.id.navigation_dashboard);
        } else if (itemId == R.id.navigation_assistant) {
            navController.navigate(R.id.navigation_assistant);
        } else if (itemId == R.id.navigation_map) {
            navController.navigate(R.id.navigation_map);
        }else if (itemId == R.id.nav_logout) {
            handleLogout();
        } else {
            return false;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
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
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
        redirectToLoginActivity();
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}