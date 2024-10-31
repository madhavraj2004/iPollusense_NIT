package com.example.ipollusen;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.ViewModel;

import org.eclipse.paho.client.mqttv3.MqttClient;

import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;

public class MqttViewModel extends ViewModel {
    private static final String TAG = "MqttViewModel";
    private MqttAndroidClient mqttClient;

    public MqttAndroidClient getMqttClient(Context context) {
        if (mqttClient == null) {
            String clientId = MqttClient.generateClientId();
            // Remove the Ack parameter if not required by your version
            mqttClient = new MqttAndroidClient(context, "tcp://broker.hivemq.com:1883",clientId, Ack.AUTO_ACK);
            Log.d(TAG, "MQTT client created: " + clientId);
        } else {
            Log.d(TAG, "MQTT client already exists: " + mqttClient.getClientId());
        }
        return mqttClient;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disconnectClient(); // Ensure the client is disconnected when the ViewModel is cleared
    }

    public void disconnectClient() {
        try {
            if (mqttClient != null) {
                if (mqttClient.isConnected()) {
                    mqttClient.disconnect();
                    Log.d(TAG, "MQTT client disconnected");
                } else {
                    Log.d(TAG, "MQTT client is not connected");
                }
            } else {
                Log.d(TAG, "MQTT client is null");
            }
        } catch (Exception e) { // Catch a more generic exception
            Log.e(TAG, "Error while disconnecting: " + e.getMessage());
        }
    }
}
