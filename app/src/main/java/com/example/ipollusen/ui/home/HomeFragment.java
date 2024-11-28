package com.example.ipollusen.ui.home;


import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipollusen.DeviceAdapter;
import com.example.ipollusen.R;
import com.example.ipollusen.databinding.FragmentHomeBinding;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
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

import android.widget.CheckBox;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import android.content.Context;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
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
    private TextView co2Value; // NO2
    private TextView pressValue; // Pressure
    private TextView vocValue; // VOC
    private TextView coValue; // CO
    private TextView pm1Value; // PM1
    private TextView pm2Value; // PM2.5
    private TextView pm10Value; // PM10
    private TextView textViewReceivedMessages; // For received MQTT messages
   // private EditText editTextTopic; // For MQTT topic input
   // private EditText editTextMessage; // For MQTT message input


    private FragmentHomeBinding binding;
    private LineChart lineChart;
    private CheckBox temperatureCheckbox, humidityCheckbox, co2Checkbox, pressureCheckbox, vocCheckbox, coCheckbox, pm1Checkbox, pm2Checkbox, pm10Checkbox;
    private LineData lineData;
    private List<LineDataSet> dataSetList;

    private RxBleClient rxBleClient;
    private RxBleDevice selectedDevice;
    private RxBleConnection connection;
    private Disposable connectionDisposable;
    private Disposable scanDisposable;

    private MqttAndroidClient mqttClient;

    private RecyclerView deviceRecyclerView;
    private DeviceAdapter deviceAdapter;

    private Map<String, String> devices = new HashMap<>(); // Map of MAC Address to Nickname

    private Handler handler = new Handler();
    private Runnable updateTask = new Runnable() {
        @Override
        public void run() {
            if (connection != null) {
                readCharacteristic();
                handler.postDelayed(this, 5000);
            }
        }
    };
    private CompositeDisposable disposables = new CompositeDisposable();
    private File csvFile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        setupChart();
        statusTextView = view.findViewById(R.id.statusTextView);
        label1 = view.findViewById(R.id.label1);
        tempValue = view.findViewById(R.id.temp_value);
        humValue = view.findViewById(R.id.hum_value);
        pressValue = view.findViewById(R.id.press_value);
        vocValue = view.findViewById(R.id.voc_value);
        pm1Value = view.findViewById(R.id.pm1_value);
        pm2Value = view.findViewById(R.id.pm2_value);
        pm10Value = view.findViewById(R.id.pm10_value);
        coValue = view.findViewById(R.id.co_value);
        co2Value = view.findViewById(R.id.co2_value);
        textViewReceivedMessages = view.findViewById(R.id.textViewReceivedMessages);
        MaterialTextView editTextTopic = view.findViewById(R.id.editTextTopic);
        MaterialTextView editTextMessage = view.findViewById(R.id.editTextMessage);

        CardView deviceCard = view.findViewById(R.id.DeviceCard);
        deviceCard.setOnClickListener(v -> showAddDeviceDialog());


        deviceRecyclerView = view.findViewById(R.id.deviceRecyclerView);
        deviceRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        deviceAdapter = new DeviceAdapter(new ArrayList<>(devices.entrySet()));
        deviceRecyclerView.setAdapter(deviceAdapter);

        // Initially hide the RecyclerView
        deviceRecyclerView.setVisibility(View.GONE);

        // Set up the dropdown icon click listener
        ImageView dropdownIcon = view.findViewById(R.id.dropdownIcon);
        dropdownIcon.setOnClickListener(v -> {
            if (deviceRecyclerView.getVisibility() == View.GONE) {
                // Show the RecyclerView if it's hidden
                deviceRecyclerView.setVisibility(View.VISIBLE);
            } else {
                // Hide the RecyclerView if it's visible
                deviceRecyclerView.setVisibility(View.GONE);
            }
        });
        Button exportButton = view.findViewById(R.id.exportButton);
        Button scanButton = view.findViewById(R.id.scanButton);
        Button connectButton = view.findViewById(R.id.connectButton);
        Button buttonPublish = view.findViewById(R.id.buttonPublish); // Button to publish messages
        exportButton.setOnClickListener(v -> exportDataToCSV());
        scanButton.setOnClickListener(v -> startScan());
        connectButton.setOnClickListener(v -> connectToDevice());
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

        lineChart = view.findViewById(R.id.exposureLineChart);

        // Load CSV data


        CheckBox tempCheckbox = view.findViewById(R.id.checkboxTemperature);
        CheckBox humCheckbox = view.findViewById(R.id.checkboxHumidity);
        CheckBox co2Checkbox = view.findViewById(R.id.checkboxCO2);
        CheckBox pressCheckbox = view.findViewById(R.id.checkboxPressure);
        CheckBox vocCheckbox = view.findViewById(R.id.checkboxVOC);
        CheckBox coCheckbox = view.findViewById(R.id.checkboxCO);
        CheckBox pm1Checkbox = view.findViewById(R.id.checkboxPM1);
        CheckBox pm2Checkbox = view.findViewById(R.id.checkboxPM2_5);
        CheckBox pm10Checkbox = view.findViewById(R.id.checkboxPM10);

        // Set listeners for each checkbox
        tempCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateChart());
        humCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateChart());
        co2Checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateChart());
        pressCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateChart());
        vocCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateChart());
        coCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateChart());
        pm1Checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateChart());
        pm2Checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateChart());
        pm10Checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateChart());



        return view;
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Safe to interact with views here
        updateChart();
    }
    private void setupChart() {
        if (lineChart != null) {
            // Enable dragging (panning)
            lineChart.setDragEnabled(true);

            // Enable scaling (zooming)
            lineChart.setScaleEnabled(true);

            // Optionally, enable pinch-to-zoom in both directions (x and y axes)
            lineChart.setPinchZoom(true);

            // Set the chart's X and Y axes to be movable (optional)
            XAxis xAxis = lineChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);  // Optional: Position X-axis at the bottom
            YAxis leftAxis = lineChart.getAxisLeft();
            leftAxis.setDrawGridLines(true);  // Optional: Enable grid lines for better visual representation
            YAxis rightAxis = lineChart.getAxisRight();
            rightAxis.setEnabled(false);  // Optional: Disable the right axis (if not needed)

            // Setting up viewPort offset to ensure the chart is visible with proper padding
            lineChart.setViewPortOffsets(50f, 50f, 50f, 50f);

            // Optionally, set the minimum/maximum values for X and Y axes (this does not restrict zoom, but sets axis limits)
            lineChart.getXAxis().setAxisMinimum(0f); // X-axis minimum value
            lineChart.getXAxis().setAxisMaximum(10f); // X-axis maximum value
            lineChart.getAxisLeft().setAxisMinimum(0f); // Y-axis minimum value
            lineChart.getAxisLeft().setAxisMaximum(100f); // Y-axis maximum value
        }
    }



    public void updateChart() {
        if (lineChart != null && getView() != null) {
            List<ILineDataSet> dataSets = new ArrayList<>();

            // Get the checkboxes for each sensor type
            CheckBox checkboxTemperature = getView().findViewById(R.id.checkboxTemperature);
            CheckBox checkboxHumidity = getView().findViewById(R.id.checkboxHumidity);
            CheckBox checkboxCO2 = getView().findViewById(R.id.checkboxCO2);
            CheckBox checkboxPressure = getView().findViewById(R.id.checkboxPressure);
            CheckBox checkboxVOC = getView().findViewById(R.id.checkboxVOC);
            CheckBox checkboxCO = getView().findViewById(R.id.checkboxCO);
            CheckBox checkboxPM1 = getView().findViewById(R.id.checkboxPM1);
            CheckBox checkboxPM2_5 = getView().findViewById(R.id.checkboxPM2_5);
            CheckBox checkboxPM10 = getView().findViewById(R.id.checkboxPM10);

            // Set the limit for data points (e.g., limit to 100 data points)
            int dataLimit = 100;

            // Fetch data for each selected checkbox and limit it
            if (checkboxTemperature.isChecked()) {
                ArrayList<Entry> tempEntries = SensorDataUtil.getSensorData(getContext(), "sensor_data.csv", 1, dataLimit);
                LineDataSet tempDataSet = new LineDataSet(tempEntries, "Temperature");
                tempDataSet.setColor(getResources().getColor(R.color.colorPrimary));
                dataSets.add(tempDataSet);
            }

            if (checkboxHumidity.isChecked()) {
                ArrayList<Entry> humEntries = SensorDataUtil.getSensorData(getContext(), "sensor_data.csv", 2, dataLimit);
                LineDataSet humDataSet = new LineDataSet(humEntries, "Humidity");
                humDataSet.setColor(getResources().getColor(R.color.colorAccent));
                dataSets.add(humDataSet);
            }

            if (checkboxCO2.isChecked()) {
                ArrayList<Entry> co2Entries = SensorDataUtil.getSensorData(getContext(), "sensor_data.csv", 3, dataLimit);
                LineDataSet co2DataSet = new LineDataSet(co2Entries, "CO2");
                co2DataSet.setColor(getResources().getColor(R.color.colorSecondary));
                dataSets.add(co2DataSet);
            }

            if (checkboxPressure.isChecked()) {
                ArrayList<Entry> pressEntries = SensorDataUtil.getSensorData(getContext(), "sensor_data.csv", 4, dataLimit);
                LineDataSet pressDataSet = new LineDataSet(pressEntries, "Pressure");
                pressDataSet.setColor(getResources().getColor(R.color.colorTertiary));
                dataSets.add(pressDataSet);
            }

            if (checkboxVOC.isChecked()) {
                ArrayList<Entry> vocEntries = SensorDataUtil.getSensorData(getContext(), "sensor_data.csv", 5, dataLimit);
                LineDataSet vocDataSet = new LineDataSet(vocEntries, "VOC");
                vocDataSet.setColor(getResources().getColor(R.color.colorQuaternary));
                dataSets.add(vocDataSet);
            }

            if (checkboxCO.isChecked()) {
                ArrayList<Entry> coEntries = SensorDataUtil.getSensorData(getContext(), "sensor_data.csv", 6, dataLimit);
                LineDataSet coDataSet = new LineDataSet(coEntries, "CO");
                coDataSet.setColor(getResources().getColor(R.color.colorFifth));
                dataSets.add(coDataSet);
            }

            if (checkboxPM1.isChecked()) {
                ArrayList<Entry> pm1Entries = SensorDataUtil.getSensorData(getContext(), "sensor_data.csv", 7, dataLimit);
                LineDataSet pm1DataSet = new LineDataSet(pm1Entries, "PM1");
                pm1DataSet.setColor(getResources().getColor(R.color.colorSixth));
                dataSets.add(pm1DataSet);
            }

            if (checkboxPM2_5.isChecked()) {
                ArrayList<Entry> pm2Entries = SensorDataUtil.getSensorData(getContext(), "sensor_data.csv", 8, dataLimit);
                LineDataSet pm2DataSet = new LineDataSet(pm2Entries, "PM2.5");
                pm2DataSet.setColor(getResources().getColor(R.color.colorSeventh));
                dataSets.add(pm2DataSet);
            }

            if (checkboxPM10.isChecked()) {
                ArrayList<Entry> pm10Entries = SensorDataUtil.getSensorData(getContext(), "sensor_data.csv", 9, dataLimit);
                LineDataSet pm10DataSet = new LineDataSet(pm10Entries, "PM10");
                pm10DataSet.setColor(getResources().getColor(R.color.colorEighth));
                dataSets.add(pm10DataSet);
            }

            // Update chart with the new data
            LineData lineData = new LineData(dataSets);
            lineChart.setData(lineData);
            lineChart.invalidate(); // Refresh the chart
        }
    }

    // Helper function to calculate average for limited data
    private List<Entry> getAverageEntries(ArrayList<Float> values, int limit, String label) {
        List<Entry> entries = new ArrayList<>();
        int totalValues = Math.min(values.size(), limit);
        float sum = 0;

        // Sum up values from the selected data
        for (int i = 0; i < totalValues; i++) {
            sum += values.get(i);
        }

        // Calculate the average
        float average = sum / totalValues;

        // Create a single entry with the average value (e.g., X-axis is time or index)
        entries.add(new Entry(0, average)); // The '0' is a placeholder for X-axis value (time or index)
        return entries;
    }



    private void showAddDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add New Device");

        // Inflate the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_device, null);
        EditText macAddressInput = dialogView.findViewById(R.id.macAddressInput);
        EditText nicknameInput = dialogView.findViewById(R.id.nicknameInput);

        builder.setView(dialogView);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String macAddress = macAddressInput.getText().toString().trim();
            String nickname = nicknameInput.getText().toString().trim();

            if (!macAddress.isEmpty() && !nickname.isEmpty()) {
                // Save the MAC address and nickname to your data structure
                devices.put(macAddress, nickname);

                // Optionally, save the data persistently (e.g., using SharedPreferences)
                saveDeviceData(macAddress, nickname);

                Toast.makeText(requireContext(), "Device added", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Please enter both MAC address and nickname", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }
    private void saveDeviceData(String macAddress, String nickname) {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("DeviceData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // Save the nickname using the MAC address as the key
        editor.putString(macAddress, nickname);
        editor.apply();
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
        Disposable scanDisposable = rxBleClient.scanBleDevices()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(scanResult -> {
                    if (scanResult.getBleDevice().getMacAddress().equals(TARGET_DEVICE_MAC)) {
                        selectedDevice = scanResult.getBleDevice();
                        statusTextView.setText("Target device found: " + selectedDevice.getName());
                    }
                }, throwable -> {
                    statusTextView.setText("Scan failed.");
                    Log.e("BLE", "Scan failed: " + throwable.toString());
                });
        disposables.add(scanDisposable);
    }

    private void connectToDevice() {
        if (selectedDevice == null) {
            statusTextView.setText("No device selected.");
            return;
        }

        connectionDisposable = selectedDevice.establishConnection(false)
                .doOnDispose(() -> Log.d("BLE", "Disconnected"))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(rxBleConnection -> {
                    connection = rxBleConnection;
                    statusTextView.setText("Connected.");
                    handler.postDelayed(updateTask, 5000);
                }, throwable -> {
                    statusTextView.setText("Connection failed.");
                    Log.e("BLE", "Connection failed: " + throwable.toString());
                });
        disposables.add(connectionDisposable);
    }
    private void readCharacteristic() {
        Disposable readDisposable = connection.readCharacteristic(UUID.fromString(CHARACTERISTIC_UUID))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(characteristicValue -> updateUIWithData(new String(characteristicValue)),
                        throwable -> {
                            statusTextView.setText("Read failed.");
                            Log.e("BLE", "Read failed: " + throwable.toString());
                        });
        disposables.add(readDisposable);
    }

    private void saveDataToCSV(JSONObject jsonObject, String timestamp) {
        try {
            if (csvFile == null) {
                csvFile = new File(requireContext().getExternalFilesDir(null), "sensor_data.csv");

            }

            boolean isNewFile = !csvFile.exists() || csvFile.length() == 0;

            try (FileWriter writer = new FileWriter(csvFile, true)) {
                if (isNewFile) {
                    // Write headers for a new file including all metrics from data, calculated, predicted, and status
                    writer.append("Timestamp,Temperature,Humidity,Pressure,PM1,PM2.5,PM10,CO,VOC,CO2,aqi_dust,aqi_co,aqi_voc,aqi_co2,aqi_dust_predicted,aqi_co_predicted,dust_status,co_status\n");
                }

                JSONObject data = jsonObject.getJSONObject("data");
                JSONObject calculated = jsonObject.getJSONObject("calculated");
                JSONObject predicted = jsonObject.getJSONObject("predicted");
                JSONObject status = jsonObject.getJSONObject("status");



                // Build the row with values from all parts
                String row = timestamp + "," +
                        data.getDouble("temperature") + "," +
                        data.getDouble("humidity") + "," +
                        data.getDouble("pressure") + "," +
                        data.getInt("pm1") + "," +
                        data.getInt("pm2_5") + "," +
                        data.getInt("pm10") + "," +
                        data.getInt("co") + "," +
                        data.getInt("voc") + "," +
                        data.getInt("co2") + "," +
                        calculated.getInt("aqi_dust") + "," +
                        calculated.getInt("aqi_co") + "," +
                        calculated.getInt("aqi_voc") + "," +
                        calculated.getInt("aqi_co2") + "," +
                        predicted.getInt("aqi_dust") + "," +
                        predicted.getInt("aqi_co") + "," +
                        status.getInt("dust") + "," +
                        status.getInt("co") + "\n";

                writer.append(row);
            }


        } catch (IOException | JSONException e) {
            Log.e("CSV", "Error writing to CSV: " + e.toString());
        }
    }


    private void exportDataToCSV() {
        if (csvFile != null && csvFile.exists()) {
            Uri fileUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", csvFile);


            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            startActivity(Intent.createChooser(intent, "Share CSV File"));
        } else {
            Toast.makeText(requireContext(), "CSV file not available.", Toast.LENGTH_SHORT).show();

        }
    }

    private void updateUIWithData(String jsonString) {
        try {

            label1.setText(jsonString);
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONObject data = jsonObject.getJSONObject("data");

            tempValue.setText(String.format("%.2f", data.getDouble("temperature")));
            humValue.setText(String.format("%.2f", data.getDouble("humidity")));
            pressValue.setText(String.format("%.2f", data.getDouble("pressure")));
            pm1Value.setText(String.valueOf(data.getInt("pm1")));
            pm2Value.setText(String.valueOf(data.getInt("pm2_5")));
            pm10Value.setText(String.valueOf(data.getInt("pm10")));
            coValue.setText(String.valueOf(data.getInt("co")));
            vocValue.setText(String.valueOf(data.getInt("voc")));
            co2Value.setText(String.valueOf(data.getInt("co2")));



            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());



            saveDataToCSV(jsonObject, timestamp);
        } catch (JSONException e) {
            statusTextView.setText("Invalid data received.");
            Log.e("BLE", "JSON Parsing error: " + e.toString());
        }
    }

    private void setupMqttClient() {
        String clientId = UUID.randomUUID().toString();
        mqttClient = new MqttAndroidClient(requireContext().getApplicationContext(), "tcp://nitdgp2.a.pinggy.link:19919", clientId, Ack.AUTO_ACK);


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
                    if (cause != null) {
                        Log.e("MQTT", "Connection lost: " + cause.toString());
                        statusTextView.setText("MQTT connection lost: " + cause.toString());
                    } else {
                        Log.e("MQTT", "Connection lost with unknown cause");
                        statusTextView.setText("MQTT connection lost: Unknown cause");
                    }

                    // Optionally, attempt to reconnect after a delay
                    handler.postDelayed(() -> setupMqttClient(), 5000); // Reattempt connection in 5 seconds
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
}