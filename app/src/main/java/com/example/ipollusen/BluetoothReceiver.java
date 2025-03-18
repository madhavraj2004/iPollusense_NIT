package com.example.ipollusense;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    Log.d("BluetoothReceiver", "Bluetooth is ON");
                    break;
                case BluetoothAdapter.STATE_OFF:
                    Log.d("BluetoothReceiver", "Bluetooth is OFF");
                    break;
            }
        }
    }
}
