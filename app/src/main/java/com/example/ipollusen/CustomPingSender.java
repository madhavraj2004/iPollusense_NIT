package com.example.ipollusen;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.ipollusen.PingBroadcastReceiver;

import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

public class CustomPingSender implements MqttPingSender {

    private static final String TAG = "CustomPingSender";
    private Context context;
    private ClientComms clientComms;
    private Timer pingTimer;
    private final long keepAliveInterval;
    private PendingIntent pendingIntent;

    public CustomPingSender(Context context, long keepAliveInterval) {
        this.context = context;
        this.keepAliveInterval = keepAliveInterval;

        // Create an Intent for the PendingIntent
        Intent intent = new Intent(context, PingBroadcastReceiver.class); // Ensure this class exists
        // Example modification in the AlarmPingSender class of Paho MQTT (pseudo-code)
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void init(ClientComms clientComms) {
        this.clientComms = clientComms;
        this.pingTimer = new Timer();
        Log.d(TAG, "Ping sender initialized");
    }

    @Override
    public void start() {
        schedule(keepAliveInterval);
        Log.d(TAG, "Ping sender started");
    }

    @Override
    public void stop() {
        if (pingTimer != null) {
            pingTimer.cancel();
            pingTimer = null;
            Log.d(TAG, "Ping sender stopped");
        }
    }

    @Override
    public void schedule(long delayInMilliseconds) {
        if (pingTimer != null) {
            pingTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    sendPing();
                }
            }, delayInMilliseconds, keepAliveInterval);
        }
    }

    private void sendPing() {
        try {
            Method checkForActivity = clientComms.getClass().getDeclaredMethod("checkForActivity");
            checkForActivity.setAccessible(true);
            checkForActivity.invoke(clientComms);
            Log.d(TAG, "Ping sent to broker");
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Method checkForActivity not found", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Illegal access to method checkForActivity", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "Invocation target exception", e);
        } catch (Exception e) {
            Log.e(TAG, "Error sending ping", e);
        }
    }
}
