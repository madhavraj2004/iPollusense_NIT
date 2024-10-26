package com.example.ipollusen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PingBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "PingBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Handle the received ping event
        Log.d(TAG, "Ping broadcast received");
        // You can perform any actions you need when the ping is received
    }
}
