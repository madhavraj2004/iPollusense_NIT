package com.example.ipollusen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "sensor_notification_channel";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d("FCM", "Message received: " + remoteMessage);

        String message = null;

        // ✅ Handle Data Payload (if exists)
        if (remoteMessage.getData().size() > 0) {
            Log.d("FCM", "Data Payload: " + remoteMessage.getData());
            if (remoteMessage.getData().containsKey("message")) {
                message = remoteMessage.getData().get("message");
            } else if (remoteMessage.getData().containsKey("body")) {
                message = remoteMessage.getData().get("body");
            }
        }

        // ✅ Handle Notification Payload (if exists)
        if (remoteMessage.getNotification() != null) {
            Log.d("FCM", "Notification Payload: " + remoteMessage.getNotification().getBody());
            message = remoteMessage.getNotification().getBody();
        }

        // ✅ Ensure message is not null before processing
        if (message != null && !message.isEmpty()) {
            sendLocalBroadcast(message);
            showNotification(message);
        } else {
            Log.e("FCM", "Received empty message, skipping notification.");
        }
    }

    private void showNotification(String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // ✅ Ensure Notification Manager is not null
        if (notificationManager == null) {
            Log.e("FCM", "NotificationManager is null, skipping notification.");
            return;
        }

        // ✅ Create Notification Channel (for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sensor Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for pollution data updates.");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        // ✅ Set notification sound
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // ✅ Build the notification
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("New Pollution Alert")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_baseline_notifications_24)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build();

        // ✅ Use a random ID to allow multiple notifications
        int notificationId = new Random().nextInt(1000);
        notificationManager.notify(notificationId, notification);
    }

    // ✅ Send a Local Broadcast for UI Updates
    private void sendLocalBroadcast(String message) {
        Intent intent = new Intent("com.example.ipollusen.NEW_NOTIFICATION");
        intent.putExtra("notification_message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d("FCM", "New FCM Token: " + token);

        // ✅ Ensure token is updated only if it has changed
        String storedToken = getStoredFCMToken();
        if (!token.equals(storedToken)) {
            saveFCMToken(token);
            Log.d("FCM", "FCM Token updated and saved.");
        } else {
            Log.d("FCM", "FCM Token remains the same, no update needed.");
        }
    }

    // ✅ Save the FCM Token to SharedPreferences and Firestore
    private void saveFCMToken(String token) {
        getSharedPreferences("FCM_PREF", MODE_PRIVATE)
                .edit()
                .putString("fcm_token", token)
                .apply();

        UserViewModel userViewModel = new UserViewModel();
        if (userViewModel.getUserId().getValue() != null) {
            userViewModel.saveFCMToken(token);
        }
    }

    // ✅ Retrieve the stored FCM token
    private String getStoredFCMToken() {
        return getSharedPreferences("FCM_PREF", MODE_PRIVATE)
                .getString("fcm_token", "");
    }
}
