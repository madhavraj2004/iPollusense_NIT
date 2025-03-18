package com.example.ipollusen;
import com.example.ipollusen.R;
import static android.app.PendingIntent.getActivity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
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
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import android.Manifest;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private Button notificationButton;
    private static final int REQUEST_READ_STORAGE = 1;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    private FirebaseAuth mAuth;
    private static final int PERMISSION_REQUEST_CODE = 101;
    private DrawerLayout drawerLayout;
    private AppBarConfiguration appBarConfiguration;
    private static final String CHANNEL_ID = "sensor_notification_channel";

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

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_person);

        // Set up custom buttons in the toolbar

        ImageButton notificationButton = findViewById(R.id.notification_button);
        ImageButton batteryIcon = findViewById(R.id.battery_icon);

        notificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MainActivity", "Notification button clicked, replacing fragment");


            }
        });



        // Set up click listeners for toolbar icons

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
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_map)
                .setOpenableLayout(drawerLayout)
                .build();

        // Set up NavigationUI with the toolbar and NavController
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Set up BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav_view);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        // Check storage permissions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            checkCsvAndNotify("/storage/emulated/0/Android/data/com.example.ipollusen/files/sensor_data.csv");
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);
        }
        scheduleCsvCheck();

        // Set up notification channel for API >= 26
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sensor Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }


    }


//    private void createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    CHANNEL_ID,
//                    "Sensor Data Notification Channel",
//                    NotificationManager.IMPORTANCE_HIGH
//            );
//            channel.setDescription("Notifications for when sensor data exceeds a threshold.");
//
//            NotificationManager notificationManager = getSystemService(NotificationManager.class);
//            if (notificationManager != null) {
//                notificationManager.createNotificationChannel(channel);
//            }
//        }
//    }

    private void checkCsvAndNotify(String filePath) {
        try {
            // Create a CSV reader
            CSVReader csvReader = new CSVReader(new FileReader(filePath));
            List<String[]> records = new ArrayList<>();
            String[] nextLine;

            // Read CSV lines and store them in a list
            boolean isFirstRow = true; // Flag to identify header row
            while ((nextLine = csvReader.readNext()) != null) {
                if (isFirstRow) {
                    isFirstRow = false; // Skip the header row
                    continue;
                }
                records.add(nextLine);
            }
            csvReader.close();

            // Check if records exist
            if (records.isEmpty()) {
                Log.d("CSV", "No data found in the CSV file.");
                return;
            }

            // Sort records by the timestamp column (assuming timestamp is in the first column)
            records.sort((a, b) -> {
                try {
                    long timestampA = Long.parseLong(a[0]); // Replace 0 with actual index if different
                    long timestampB = Long.parseLong(b[0]); // Replace 0 with actual index if different
                    return Long.compare(timestampB, timestampA);
                } catch (NumberFormatException e) {
                    return 0;
                }
            });

            // Get the last few records (e.g., the last 5)
            int numberOfRecordsToCheck = 5;
            List<String[]> recentRecords = records.subList(0, Math.min(numberOfRecordsToCheck, records.size()));

            // Iterate over the recent records and check for dust_status and co_status
            for (String[] record : recentRecords) {
                try {
                    // Validate and parse dust_status and co_status only if they are numeric
                    if (record.length > 16) { // Check if there are enough columns in the record
                        int dustStatus = Integer.parseInt(record[16]); // Replace 16 with actual index if different
                        int coStatus = Integer.parseInt(record[17]); // Replace 17 with actual index if different

                        // Check if dust_status or co_status is 1
                        if (dustStatus == -1 || coStatus == -1) {
                            String message = "Sensor Alert: ";
                            if (dustStatus == 1) {
                                message += "Dust sensor is faulty. ";
                            }
                            if (coStatus == 1) {
                                message += "CO sensor is faulty.";
                            }

                            // Send notification
                            sendNotification(message);
                            return; // Exit after sending notification, as only one is needed
                        }
                    }
                } catch (NumberFormatException e) {
                    Log.e("CSV", "Invalid data format in record: " + Arrays.toString(record), e);
                }
            }
        } catch (IOException e) {
            Log.e("CSV", "Error reading CSV file", e);
        } catch (CsvValidationException e) {
            Log.e("CSV", "CSV validation error occurred", e);
        }
    }


    private void scheduleCsvCheck() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleWithFixedDelay(() -> {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                checkCsvAndNotify("/storage/emulated/0/Android/data/com.example.ipollusen/files/sensor_data.csv");
            } else {
                // Optionally, log or show a toast if permission is not granted
                Log.d("MainActivity", "Permission not granted for reading storage.");
            }
        }, 0, 5, TimeUnit.MINUTES); // Check every 5 minutes
    }

    private void sendNotification(String message) {
        // Create a notification builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_notifications_24)
                .setContentTitle("Sensor Alert")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true); // Automatically dismiss the notification when clicked

        // Create an intent for opening an activity when the notification is clicked


        // Get the notification manager
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // If the Android version is Oreo or higher, create the notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sensor Notification Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Notify the user
        notificationManager.notify(1, builder.build());
    }


    private void showNotification(String title, String content) {
        // Check for notification permission for Android 13 (API level 33) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request the notification permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
                return; // Exit the method until permission is granted
            }
        }

        // Create an Intent to open the app when the notification is clicked
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Wrap the Intent in a PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        ? PendingIntent.FLAG_IMMUTABLE
                        : 0)
        );

        // Create a notification channel for Android 8.0 and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Notification Channel Name",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notification Channel Description");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_notifications_24) // Replace with your notification icon
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); // Dismiss notification on click

        // Show the notification
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(1, builder.build());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, show a notification or other logic
                showNotification("Permission Granted", "You can now receive notifications.");
            } else {
                // Permission denied, inform the user
                Toast.makeText(this, "Permission denied. You won't receive notifications.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void showErrorDialog(String message) {
        if (!isFinishing() && !isDestroyed()) {
            runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(message)
                        .setPositiveButton("OK", (dialog, id) -> dialog.dismiss());
                AlertDialog dialog = builder.create();
                dialog.show();
            });
        }

        // Create and show the dialog
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
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


}