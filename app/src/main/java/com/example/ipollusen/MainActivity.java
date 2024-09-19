package com.example.ipollusen;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.ipollusen.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use view binding to access UI elements
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example: Fetching username from SharedPreferences or directly from a source
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "User"); // Fallback to "User" if not available

        // Access the NavigationView header and set the greeting dynamically
        View headerView = binding.navView.getHeaderView(0);
        TextView greetingTextView = headerView.findViewById(R.id.greetingTextView);
        greetingTextView.setText("Hi, " + username);

        // Set up the toolbar as the ActionBar
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);  // Hide default title

        // Setup DrawerLayout and Navigation components
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // Configure the AppBar with the DrawerLayout and Navigation
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home)  // Add more top-level destinations here if needed
                .setOpenableLayout(drawer)
                .build();

        // Set up NavController and link it with navigation components
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Handle the User Button (opens the navigation drawer)
        binding.userButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    drawer.openDrawer(GravityCompat.START);
                }
            }
        });

        // Handle the Notification Button
        binding.notificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Notifications clicked", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Handle the Battery Icon
        binding.batteryIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Battery status clicked", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // FloatingActionButton click action
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "FAB clicked", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Remove the default drawer toggle icon from the toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
