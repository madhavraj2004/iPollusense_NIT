package com.example.ipollusen.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.ipollusen.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.material.textview.MaterialTextView;
import com.polidea.rxandroidble3.RxBleClient;
import com.polidea.rxandroidble3.RxBleDevice;
import com.polidea.rxandroidble3.RxBleConnection;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.UUID;

import info.mqtt.android.service.MqttAndroidClient;
import info.mqtt.android.service.Ack;

public class HomeFragment extends Fragment {

    private static final int PERMISSION_REQUEST_CODE = 1;

    private static final String SERVICE_UUID = "00001800-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTIC_UUID = "0000fef4-0000-1000-8000-00805f9b34fb";
    private static final String TARGET_DEVICE_MAC = "7C:DF:A1:EE:D4:96";
    private static final String MQTT_TOPIC = "topic/test"; // Default topic

    private TextView statusTextView;
    private TextView label1; // For JSON data
    private TextView tempValue; // Temperature
    private TextView humValue; // Humidity
    private TextView no2Value; // NO2
    private TextView c2h5ohValue; // C2H5OH
    private TextView vocValue; // VOC
    private TextView coValue; // CO
    private TextView pm1Value; // PM1
    private TextView pm2Value; // PM2.5
    private TextView pm10Value; // PM10
    private TextView textViewReceivedMessages; // For received MQTT messages
    private EditText editTextTopic; // For MQTT topic input
    private EditText editTextMessage; // For MQTT message input

    private RxBleClient rxBleClient;
    private RxBleDevice selectedDevice;
    private RxBleConnection connection;
    private Disposable connectionDisposable;
    private Disposable scanDisposable;

    private MqttAndroidClient mqttClient;
    private Handler handler;
    private Runnable updateTask;

    private BarChart barChartPM1;
    private BarChart barChartPM2_5;
    private BarChart barChartPM10;
    private BarChart barChartCO;
    private BarChart barChartNO2;
    private BarChart barChartVOC;
    private BarChart barChartC2H5OH;
    private BarChart barChartTemperature;
    private BarChart barChartHumidity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        statusTextView = view.findViewById(R.id.statusTextView);
        label1 = view.findViewById(R.id.label1);
        tempValue = view.findViewById(R.id.temp_value);
        humValue = view.findViewById(R.id.hum_value);
        no2Value = view.findViewById(R.id.no2_value);
        c2h5ohValue = view.findViewById(R.id.c2h5oh_value);
        vocValue = view.findViewById(R.id.voc_value);
        coValue = view.findViewById(R.id.co_value);
        pm1Value = view.findViewById(R.id.pm10_value);
        pm2Value = view.findViewById(R.id.pm2_value);
        pm10Value = view.findViewById(R.id.pm10_value);
        textViewReceivedMessages = view.findViewById(R.id.textViewReceivedMessages);
        MaterialTextView editTextTopic = view.findViewById(R.id.editTextTopic);
        MaterialTextView editTextMessage = view.findViewById(R.id.editTextMessage);



        Button scanButton = view.findViewById(R.id.scanButton);
        Button connectButton = view.findViewById(R.id.connectButton);
        Button readButton = view.findViewById(R.id.readButton);
        Button buttonPublish = view.findViewById(R.id.buttonPublish); // Button to publish messages

        scanButton.setOnClickListener(v -> startScan());
        connectButton.setOnClickListener(v -> connectToDevice());
        readButton.setOnClickListener(v -> readCharacteristic());
        buttonPublish.setOnClickListener(v -> {
            String topic = editTextTopic.getText().toString().isEmpty() ? MQTT_TOPIC : editTextTopic.getText().toString();
            String message = editTextMessage.getText().toString();
            publishMessage(topic, message);
        });

        checkPermissions();
        rxBleClient = RxBleClient.create(requireContext());
        startScan(); // Automatically start scanning when the fragment is opened
        setupMqttClient(); // Set up MQTT client

        // Initialize handler for periodic updates
        handler = new Handler();
        updateTask = new Runnable() {
            @Override
            public void run() {
                readCharacteristic(); // Read characteristic periodically
                handler.postDelayed(this, 5000); // Schedule next execution in 5 seconds
            }
        };
        barChartPM1 = view.findViewById(R.id.barChartPM1);
        barChartPM2_5 = view.findViewById(R.id.barChartPM2_5);
        barChartPM10 = view.findViewById(R.id.barChartPM10);
        barChartCO = view.findViewById(R.id.barChartCO);
        barChartNO2 = view.findViewById(R.id.barChartNO2);
        barChartVOC = view.findViewById(R.id.barChartVOC);
        barChartC2H5OH = view.findViewById(R.id.barChartC2H5OH);
        barChartTemperature = view.findViewById(R.id.barChartTemperature);
        barChartHumidity = view.findViewById(R.id.barChartHumidity);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(updateTask);
        if (scanDisposable != null && !scanDisposable.isDisposed()) {
            scanDisposable.dispose();
        }
        if (connectionDisposable != null && !connectionDisposable.isDisposed()) {
            connectionDisposable.dispose();
        }
        if (mqttClient != null) {
            mqttClient.disconnect();
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        }
    }

    private void startScan() {
        statusTextView.setText("Scanning...");
        scanDisposable = rxBleClient.scanBleDevices()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(scanResult -> {
                    if (scanResult.getBleDevice().getMacAddress().equals(TARGET_DEVICE_MAC)) {
                        selectedDevice = scanResult.getBleDevice();
                        statusTextView.setText("Target device found: " + selectedDevice.getName() + " - " + selectedDevice.getMacAddress());
                        Log.d("BLE", "Target device found: " + selectedDevice.getName() + " - " + selectedDevice.getMacAddress());
                        connectToDevice(); // Automatically connect to the device
                        if (scanDisposable != null && !scanDisposable.isDisposed()) {
                            scanDisposable.dispose(); // Stop scanning when the target device is found
                        }
                    }
                }, throwable -> {
                    statusTextView.setText("Scan failed.");
                    Log.e("BLE", "Scan failed: " + throwable.toString());
                });
    }

    private void connectToDevice() {
        if (selectedDevice == null) {
            statusTextView.setText("No device selected.");
            Log.d("BLE", "No device selected.");
            return;
        }

        connectionDisposable = selectedDevice.establishConnection(false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(rxBleConnection -> {
                    connection = rxBleConnection;
                    statusTextView.setText("Connected.");
                    Log.d("BLE", "Connected to device.");

                    // Start the periodic update after connecting
                    handler.postDelayed(updateTask, 5000); // Start updates after 5 seconds

                }, throwable -> {
                    statusTextView.setText("Connection failed.");
                    Log.e("BLE", "Connection failed: " + throwable.toString());
                });
    }

    private void readCharacteristic() {
        if (connection == null) {
            statusTextView.setText("Connection is null.");
            Log.e("BLE", "Connection is null.");
            return;
        }

        connection.readCharacteristic(UUID.fromString(CHARACTERISTIC_UUID))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bytes -> {
                    String jsonString = new String(bytes);
                    Log.d("BLE", "Data received: " + jsonString);
                    updateUIWithData(jsonString);
                    publishMessage(MQTT_TOPIC, jsonString); // Publish data to the MQTT server
                }, throwable -> {
                    statusTextView.setText("Failed to read characteristic.");
                    Log.e("BLE", "Failed to read characteristic: " + throwable.toString());
                });
    }

    private void setupMqttClient() {
        String clientId = UUID.randomUUID().toString();
        mqttClient = new MqttAndroidClient(requireContext().getApplicationContext(), "tcp://broker.hivemq.com:1883", clientId, Ack.AUTO_ACK);


        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);

        try {
            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("MQTT", "Connected to MQTT broker");
                    subscribeToTopic(MQTT_TOPIC); // Subscribe to the topic on connection
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("MQTT", "Failed to connect to MQTT broker: " + exception.toString());
                }
            });

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e("MQTT", "Connection lost: " + cause.toString());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String receivedMessage = new String(message.getPayload());
                    textViewReceivedMessages.setText(receivedMessage); // Display received message
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Do nothing for delivery complete
                }
            });

        } catch (Exception e) {
            Log.e("MQTT", "Error setting up MQTT client: " + e.toString());
        }
    }

    private void subscribeToTopic(String topic) {
        try {
            mqttClient.subscribe(topic, 0);
            Log.d("MQTT", "Subscribed to topic: " + topic);
        } catch (Exception e) {
            Log.e("MQTT", "Error subscribing to topic: " + e.toString());
        }
    }

    private void publishMessage(String topic, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttClient.publish(topic, mqttMessage);
            Log.d("MQTT", "Message published to topic: " + topic + ", message: " + message);
        } catch (Exception e) {
            Log.e("MQTT", "Error publishing message: " + e.toString());
        }
    }

    private void updateUIWithData(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);

            // Update TextViews
            tempValue.setText(jsonObject.getString("temperature"));
            humValue.setText(jsonObject.getString("humidity"));
            no2Value.setText(jsonObject.getString("no2"));
            c2h5ohValue.setText(jsonObject.getString("c2h5oh"));
            vocValue.setText(jsonObject.getString("voc"));
            coValue.setText(jsonObject.getString("co"));
            pm1Value.setText(jsonObject.getString("pm1"));
            pm2Value.setText(jsonObject.getString("pm2_5")); // Adjusted to match key from JSON
            pm10Value.setText(jsonObject.getString("pm10"));

            // Update BarCharts
            updateBarChart(barChartPM1, jsonObject.getDouble("pm1"));
            updateBarChart(barChartPM2_5, jsonObject.getDouble("pm2_5"));
            updateBarChart(barChartPM10, jsonObject.getDouble("pm10"));
            updateBarChart(barChartCO, jsonObject.getDouble("co"));
            updateBarChart(barChartNO2, jsonObject.getDouble("no2"));
            updateBarChart(barChartVOC, jsonObject.getDouble("voc"));
            updateBarChart(barChartC2H5OH, jsonObject.getDouble("c2h5oh"));
            updateBarChart(barChartTemperature, jsonObject.getDouble("temperature"));
            updateBarChart(barChartHumidity, jsonObject.getDouble("humidity"));

        } catch (JSONException e) {
            Log.e("JSON", "Error parsing JSON: " + e.toString());
        }
    }


    private void updateBarChart(BarChart barChart, double value) {
        // Create data entry
        BarEntry barEntry = new BarEntry(0, (float) value);
        BarDataSet barDataSet = new BarDataSet(Collections.singletonList(barEntry), "Value");

        // Customize the BarDataSet (e.g., color)
        barDataSet.setColor(Color.BLUE); // Set your preferred color

        // Create BarData
        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);
        barChart.invalidate(); // Refresh the chart
    }
}