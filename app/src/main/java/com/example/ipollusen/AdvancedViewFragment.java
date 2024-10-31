package com.example.ipollusen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;



import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import info.mqtt.android.service.MqttAndroidClient;

public class AdvancedViewFragment extends Fragment {

    private static final String TAG = "AdvancedViewFragment";
    private MqttAndroidClient mqttClient;
    private TextView textViewStatus, textViewReceivedMessages;
    private EditText editTextTopic, editTextMessage;
    private Button buttonPublish;
    private MqttViewModel mqttViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_advanced_view, container, false);

        // Initialize UI components
        textViewStatus = view.findViewById(R.id.textViewStatus);
        textViewReceivedMessages = view.findViewById(R.id.textViewReceivedMessages);
        editTextTopic = view.findViewById(R.id.editTextTopic);
        editTextMessage = view.findViewById(R.id.editTextMessage);
        buttonPublish = view.findViewById(R.id.buttonPublish);

        // Initialize ViewModel
        mqttViewModel = new ViewModelProvider(this).get(MqttViewModel.class);
        mqttClient = mqttViewModel.getMqttClient(requireContext().getApplicationContext());

        setupMqttClient();

        buttonPublish.setOnClickListener(v -> {
            String topic = editTextTopic.getText().toString();
            String message = editTextMessage.getText().toString();
            publishMessage(topic, message);
        });

        return view;
    }

    private void setupMqttClient() {
        if (mqttClient == null) {
            Log.e(TAG, "MQTT client is null");
            return;
        }

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);

        try {
            IMqttToken token = mqttClient.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    textViewStatus.setText("Connected");
                    Log.d(TAG, "Connected to broker");
                    subscribeToTopic("test/topic"); // Replace with your topic
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    textViewStatus.setText("Connection failed");
                    Log.d(TAG, "Connection failed: " + exception.toString());
                }
            });
        } catch (Exception e) { // Change to catch a generic Exception
            Log.e(TAG, "Exception while connecting: " + e.getMessage());
        }

        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                if (cause != null) {
                    Log.e(TAG, "Connection lost: " + cause.toString());
                } else {
                    Log.e(TAG, "Connection lost: Cause is null");
                }

                // Any other reconnection logic or handling can go here
            }


            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String msg = new String(message.getPayload());
                textViewReceivedMessages.append("\n" + topic + ": " + msg);
                Log.d(TAG, "Message received: " + msg);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(TAG, "Delivery complete");
            }
        });
    }

    private void subscribeToTopic(String topic) {
        int qos = 1; // Quality of Service level
        try {
            IMqttToken subToken = mqttClient.subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    textViewStatus.setText("Subscribed to " + topic);
                    Log.d(TAG, "Subscribed to topic: " + topic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    textViewStatus.setText("Subscription failed");
                    Log.d(TAG, "Subscription failed: " + exception.toString());
                }
            });
        } catch (Exception e) { // Change to catch a generic Exception
            Log.e(TAG, "Exception while subscribing: " + e.getMessage());
        }
    }

    private void publishMessage(String topic, String payload) {
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes());
            mqttClient.publish(topic, message);
            Log.d(TAG, "Message published to topic: " + topic);
        } catch (Exception e) { // Change to catch a generic Exception
            Log.e(TAG, "Exception while publishing message: " + e.getMessage());
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mqttViewModel != null) {
            mqttViewModel.disconnectClient(); // Disconnect using the ViewModel
        }
    }

}
