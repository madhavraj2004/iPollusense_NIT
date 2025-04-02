package com.example.ipollusen.ui.home;


import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.ipollusen.BluetoothDeviceAdapter;
import com.example.ipollusen.R;
import com.example.ipollusen.UserViewModel;
import com.example.ipollusen.databinding.FragmentHomeBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.polidea.rxandroidble3.RxBleClient;
import com.polidea.rxandroidble3.RxBleDevice;
import com.polidea.rxandroidble3.RxBleConnection;

import android.widget.CheckBox;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import android.content.Context;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import info.mqtt.android.service.MqttAndroidClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.concurrent.ArrayBlockingQueue;


import com.android.volley.RequestQueue;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
public class HomeFragment extends Fragment {

    private Button mPickDateButton;
    private TextView mShowSelectedDateText;
    private double latitude;
    private double longitude;
    private JSONObject jsonObject;
    private String timestamp;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int MAX_RETRIES = 5; // Set a maximum number of retries to prevent infinite attempts
    private static final long RETRY_INTERVAL_MS = 5000; // Initial retry interval (5 seconds)
    private long retryInterval = RETRY_INTERVAL_MS; // Dynamic retry interval for backoff
    private static final String MQTT_BROKER_URL = "tcp://nitdgp3.a.pinggy.link:17224";
    private static final String HTTP_ENDPOINT = "https://nitdgp2.a.pinggy.link/mqtt-post";
    private static final String CHARACTERISTIC_UUID = "0000fef4-0000-1000-8000-00805f9b34fb";
    //private static final String TARGET_DEVICE_MAC = "7C:DF:A1:EE:D4:96";
    private static String MQTT_TOPIC = ""; // Default topic
    private static final int MAX_RECORDS = 10; // Size of the data queue for each parameter
    private static final String SHARED_PREFS_NAME = "connected_devices_prefs";
    private static final String KEY_CONNECTED_DEVICES = "connected_devices";
    private String lastSentData = null; // Store the last sent JSON data
    private static String MQTT_DEVICE = "";
    private MqttAndroidClient mqttClient;
    private LineChart lineChart;

    private Map<String, List<Entry>> currentData = new HashMap<>();
    private boolean isHistoricalMode = false;

    private UserViewModel userViewModel;
    private RequestQueue requestQueue;
    private static final long BUFFER_TIME_LIMIT = 20 * 60 * 1000; // 20 minutes in milliseconds

    // Store sensor data lists for LineChart
    private final HashMap<String, ArrayList<Entry>> sensorData = new HashMap<>();

    // Initialize sensorData HashMap
    private void initializeSensorData() {
        String[] keys = {"temperature", "humidity", "co2", "pressure", "voc", "co", "pm1", "pm2.5", "pm10"};
        for (String key : keys) {
            sensorData.put(key, new ArrayList<>()); // Each sensor type gets its own list
        }
    }

    private Spinner deviceSpinner;
    private List<String> connectedDevicesList = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    private String jsonString;
  //  private String filePath = "/storage/emulated/0/Android/data/com.example.ipollusen/files/sensor_data.csv";
    private Map<String, ArrayBlockingQueue<Entry>> dataQueues; // Data queues for each sensor parameter
    private LineChart liveLineChart;

    private BluetoothDeviceAdapter recyclerViewAdapter;
    private List<RxBleDevice> discoveredDevices = new ArrayList<>();

    private ArrayAdapter<String> deviceAdapter;
    private TextView statusTextView;
    private RxBleClient rxBleClient;
    private String targetDeviceMac;

    private BluetoothDeviceAdapter bluetoothDeviceAdapter;
    private RecyclerView deviceRecyclerView;
    private List<RxBleDevice> scannedDevices = new ArrayList<>();


    private CheckBox liveCheckboxTemperature, liveCheckboxHumidity , liveCheckboxPressure , liveCheckboxPM1 , liveCheckboxPM2_5 , liveCheckboxPM10 , liveCheckboxCO , liveCheckboxVOC , liveCheckboxCO2 ;

    private FragmentHomeBinding binding;

    private CheckBox tempCheckbox, humCheckbox, co2Checkbox, pressCheckbox, vocCheckbox, coCheckbox, pm1Checkbox, pm2Checkbox, pm10Checkbox;


    private LineData lineData;
    private List<LineDataSet> dataSetList;




    private RxBleDevice selectedDevice;
    private RxBleConnection connection;
    private Disposable connectionDisposable;
    private Disposable scanDisposable;





    private Map<String, String> devices = new HashMap<>(); // Map of MAC Address to Nickname

    private Handler handler = new Handler();
    private Runnable updateTask = new Runnable() {
        @Override
        public void run() {
            if (connection != null) {

                handler.postDelayed(this, 10000);
            }
        }
    };
    private CompositeDisposable disposables = new CompositeDisposable();
  //  private File csvFile;

    private List<Float> liveDustData = new ArrayList<>();
    private List<Float> liveCOData = new ArrayList<>();




    private static final int BUFFER_SIZE = 20;

    // Create a queue for storing the latest data
    private Queue<Float> temperatureBuffer = new LinkedList<>();
    private Queue<Float> humidityBuffer = new LinkedList<>();
    private Queue<Float> pressureBuffer = new LinkedList<>();
    private Queue<Float> pm1Buffer = new LinkedList<>();
    private Queue<Float> pm2_5Buffer = new LinkedList<>();
    private Queue<Float> pm10Buffer = new LinkedList<>();
    private Queue<Float> coBuffer = new LinkedList<>();
    private Queue<Float> vocBuffer = new LinkedList<>();
    private Queue<Float> co2Buffer = new LinkedList<>();
    private static final int MAX_DATA_POINTS = 100;
    private static final String API_URL = "http://52.250.54.24:3500/api/node/filter";
    private ArrayList<Entry> tempData, humData, pressData;
    private ArrayList<Entry> pm1Data, pm2Data, pm10Data, coData, vocData, co2Data;


    private ArrayList<Entry> dataEntries = new ArrayList<>();
    private final int maxDataPoints = 100;
    private String message;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private Spinner deviceDropdown;
    private View addDeviceLayout, mqttDeviceLayout, bluetoothDeviceLayout;
    private Button  bluetoothOption, mqttOption, cancelOption;
    private Button okButton, backButtonMqtt, scanButton, backButtonBluetooth;
    private EditText deviceIdInput;
    private RecyclerView bluetoothRecyclerView;

    private ArrayList<String> deviceList = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;


    private LocationManager locationManager;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);




        deviceSpinner = view.findViewById(R.id.deviceDropdown);
        deviceDropdown = view.findViewById(R.id.deviceDropdown);
        CheckBox liveCheckboxTemperature = view.findViewById(R.id.LivecheckboxTemperature);
        CheckBox liveCheckboxHumidity = view.findViewById(R.id.LivecheckboxHumidity);
        CheckBox liveCheckboxCO2 = view.findViewById(R.id.LivecheckboxCO2);
        CheckBox liveCheckboxPressure = view.findViewById(R.id.LivecheckboxPressure);
        CheckBox liveCheckboxVOC = view.findViewById(R.id.LivecheckboxVOC);
        CheckBox liveCheckboxCO = view.findViewById(R.id.LivecheckboxCO);
        CheckBox liveCheckboxPM1 = view.findViewById(R.id.LivecheckboxPM1);
        CheckBox liveCheckboxPM2_5 = view.findViewById(R.id.LivecheckboxPM2_5);
        CheckBox liveCheckboxPM10 = view.findViewById(R.id.LivecheckboxPM10);

        liveCheckboxTemperature.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIWithData(jsonString));
        liveCheckboxHumidity.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIWithData(jsonString));
        liveCheckboxPressure.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIWithData(jsonString));
        liveCheckboxPM1.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIWithData(jsonString));
        liveCheckboxPM2_5.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIWithData(jsonString));
        liveCheckboxPM10.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIWithData(jsonString));
        liveCheckboxCO.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIWithData(jsonString));
        liveCheckboxVOC.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIWithData(jsonString));
        liveCheckboxCO2.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIWithData(jsonString));


        // Initialize UI Elements

        addDeviceLayout = view.findViewById(R.id.addDeviceLayout);
        mqttDeviceLayout = view.findViewById(R.id.mqttDeviceLayout);
        bluetoothDeviceLayout = view.findViewById(R.id.bluetoothDeviceLayout);
        statusTextView = view.findViewById(R.id.statusTextView);


        bluetoothOption = view.findViewById(R.id.bluetoothOption);
        mqttOption = view.findViewById(R.id.mqttOption);
        cancelOption = view.findViewById(R.id.cancelOption);

        okButton = view.findViewById(R.id.okButton);
        backButtonMqtt = view.findViewById(R.id.backButtonMqtt);
        scanButton = view.findViewById(R.id.scanButton);
        backButtonBluetooth = view.findViewById(R.id.backButtonBluetooth);

        deviceIdInput = view.findViewById(R.id.deviceIdInput);
        bluetoothRecyclerView = view.findViewById(R.id.bluetoothRecyclerView);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        requestBluetoothPermissions(); // Ensure permissions are granted at launch

        setupButtonListeners();
        Configuration.getInstance().setUserAgentValue("com.example.ipollusen");
        // Load connected devices from SharedPreferences
        loadConnectedDevices();
        // Initialize RecyclerView
        bluetoothRecyclerView = view.findViewById(R.id.bluetoothRecyclerView);
        bluetoothRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

// Initialize the BluetoothDeviceAdapter and set it to the RecyclerView
        recyclerViewAdapter = new BluetoothDeviceAdapter(discoveredDevices, device -> {
            // When an item is clicked, update the target device MAC
            targetDeviceMac = device.getMacAddress();
            statusTextView.setText("Selected device: " + device.getName());

            // Hide RecyclerView once a device is selected
            bluetoothRecyclerView.setVisibility(View.GONE);
            connectToDevice();
            bluetoothDeviceLayout.setVisibility(View.GONE);
            // Add the selected device to the Spinner if not already present
            String deviceInfo = device.getName() + " (" + device.getMacAddress() + ")";
            if (!connectedDevicesList.contains(deviceInfo)) {
                connectedDevicesList.add(deviceInfo);
                spinnerAdapter.notifyDataSetChanged(); // Notify spinnerAdapter of the update

                // Save the updated connected devices list
                saveConnectedDevices();
            }
        });
        bluetoothRecyclerView.setAdapter(recyclerViewAdapter);


// Create a spinnerAdapter for the Spinner
        spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, connectedDevicesList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deviceSpinner.setAdapter(spinnerAdapter);

// Set the OnItemSelectedListener for the Spinner
        deviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected device info from the Spinner (e.g., "DeviceName (MAC Address)")
                String selectedDeviceInfo = connectedDevicesList.get(position);

                // Extract MAC address from the selected item
                String selectedDeviceMac = extractMacAddress(selectedDeviceInfo);

                targetDeviceMac = selectedDeviceMac;

                // Update the status text to show the selected device
                statusTextView.setText("Selected device: " + selectedDeviceInfo);

                // Call connectToDevice() with the selected MAC address
                connectToDevice();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case where no item is selected (optional)
                statusTextView.setText("No device selected.");
            }
        });

// Hide RecyclerView by default
        bluetoothRecyclerView.setVisibility(View.GONE);








        GpsMyLocationProvider locationProvider = new GpsMyLocationProvider(requireContext());



        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);


        // Request permissions if not already granted
        requestPermissionsIfNecessary(new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        });

        // Set up the location listener to update the TextView with current coordinates
//        LocationListener locationListener = new LocationListener() {
//            @Override
//            public void onLocationChanged(@NonNull Location location) {
//                latitude = location.getLatitude();
//                longitude = location.getLongitude();
//
//                if (jsonObject != null && timestamp != null) {
//                    saveDataToCSV(jsonObject, timestamp, latitude, longitude);
//                } else {
//                    Log.e("LocationError", "JSON object or timestamp is not available.");
//                }
//
//            }
//
//            @Override
//            public void onStatusChanged(String provider, int status, Bundle extras) {
//            }
//
//            @Override
//            public void onProviderEnabled(@NonNull String provider) {
//            }
//
//            @Override
//            public void onProviderDisabled(@NonNull String provider) {
//            }
//        };
//
//        // Request location updates (e.g., every 5 seconds or minimum distance change)
//        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
//                == PackageManager.PERMISSION_GRANTED) {
//            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
//        }
        statusTextView = view.findViewById(R.id.statusTextView);



        tempData = new ArrayList<>();
        humData = new ArrayList<>();
        pressData = new ArrayList<>();
        pm1Data = new ArrayList<>();
        pm2Data = new ArrayList<>();
        pm10Data = new ArrayList<>();
        coData = new ArrayList<>();
        vocData = new ArrayList<>();
        co2Data = new ArrayList<>();



        dataQueues = new HashMap<>();
        String[] keys = {
                "temperature", "humidity", "pressure", "pm1", "pm2_5", "pm10", "co", "voc", "co2"
        };

        // Initialize queues for each parameter
        for (String key : keys) {
            dataQueues.put(key, new ArrayBlockingQueue<>(MAX_RECORDS));
        }
        liveLineChart = view.findViewById(R.id.LiveLineChart);






       // Button exportButton = view.findViewById(R.id.exportButton);
        Button scanButton = view.findViewById(R.id.scanButton);

        statusTextView = view.findViewById(R.id.statusTextView);



        okButton = view.findViewById(R.id.okButton);
        okButton.setOnClickListener(v -> {
            // Ensure the topic has the 'data/' prefix and takes only the device ID from deviceIdInput
            String deviceId = deviceIdInput.getText().toString().trim();
            if (!deviceId.isEmpty()) {
                MQTT_DEVICE = "data/" + deviceId;
            } else {
                Log.e("MQTT", "Device ID is empty. Please enter a valid device ID.");
                Toast.makeText(requireContext(), "Please enter a valid device ID", Toast.LENGTH_SHORT).show();
                return; // Exit if device ID is not provided
            }

            // Setup MQTT for the specified device
           // setupMqttDevice();
            mqttDeviceLayout.setVisibility(View.GONE);
        });

        //exportButton.setOnClickListener(v -> exportDataToCSV());
        scanButton.setOnClickListener(v -> startScan());



        rxBleClient = RxBleClient.create(requireContext());
        // Automatically start scanning when the fragment is opened


        // Initialize handler for periodic updates
        handler = new Handler();

        //setupMqttDevice();
        updateTask = new Runnable() {
            @Override
            public void run() {
                if (connection != null) {
                    readCharacteristic();
                    handler.postDelayed(this, 10000);
                }
            }
        };

        lineChart = view.findViewById(R.id.exposureLineChart);


        mPickDateButton = view.findViewById(R.id.pick_date_button);
        mShowSelectedDateText = view.findViewById(R.id.show_selected_date);
        // Load CSV data




      //  float liveDustAverage = getAverage(getContext(), "sensor_data.csv", "aqi_dust", true);

       // Log.d(TAG, "Live Dust Average: " + liveDustAverage);

      //  float predictedDustAverage = getAverage(getContext(), "sensor_data.csv", "aqi_dust_predicted", false);
      //  Log.d(TAG, "Predicted Dust Average: " + predictedDustAverage);
        liveLineChart = view.findViewById(R.id.LiveLineChart);
        setupliveChart();
        if (liveLineChart == null) {
            Log.e("MqttProcessing", "Live chart initialization failed.");
        }




        checkPermissions();
        rxBleClient = RxBleClient.create(requireContext());
        handler = new Handler();




        // Initialize the button and text view
        // Register the button and text view
        mPickDateButton = view.findViewById(R.id.pick_date_button);
        mShowSelectedDateText = view.findViewById(R.id.show_selected_date);

        // Create an instance of the Material Date Range Picker
        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> materialDateBuilder = MaterialDatePicker.Builder.dateRangePicker();
        materialDateBuilder.setTitleText("SELECT A DATE");

        final MaterialDatePicker<Pair<Long, Long>> materialDatePicker = materialDateBuilder.build();

        // Set up the button to open the Material Date Range Picker
        mPickDateButton.setOnClickListener(v -> materialDatePicker.show(getParentFragmentManager(), "MATERIAL_DATE_PICKER"));

        // Handle the positive button click from the Material Date Range Picker
        materialDatePicker.addOnPositiveButtonClickListener(selection -> {
            // Check if both dates are selected
            if (selection.first != null && selection.second != null) {
                // Format the start and end date into a readable format
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
                String startDate = sdf.format(new Date(selection.first)).split(" ")[0] + " 00:00:00";
                String endDate = sdf.format(new Date(selection.second)).split(" ")[0] + " 23:59:59";

                // Log the selected date range
                Log.d("HomeFragment", "Selected date range: " + startDate + " to " + endDate);

                // Set the mode to historical mode
                isHistoricalMode = true;

                // Fetch data for the selected date range
                fetchData(startDate, endDate);

                // Optionally, show the selected date range on the UI
                @SuppressLint("SetTextI18n")
                String selectedDate = "Selected Date: " + startDate + " to " + endDate;
                mShowSelectedDateText.setText(selectedDate);
            } else {
                Log.e("HomeFragment", "Date selection was null");
            }
        });

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        // Observe user email
        userViewModel.getUserEmail().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String userEmail) {
                if (userEmail == null || userEmail.isEmpty()) {
                    Log.e(TAG, "Email not found in ViewModel");
                } else {
                    Log.d(TAG, "User Email: " + userEmail);
                }
            }
        });

        // Fetch user ID when HomeFragment opens

        setupDatePicker();
        initCheckBoxes(view);
        setupChart();
        setupDeviceDropdown();
        return view;


    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Safe to interact with views here


        CheckBox checkboxLive = view.findViewById(R.id.checkboxLive);
        CheckBox checkboxPredicted = view.findViewById(R.id.checkboxPredicted);
        CheckBox checkboxDust = view.findViewById(R.id.checkboxpredictionDust);
        CheckBox checkboxCO = view.findViewById(R.id.checkboxpredictionCO);
        checkboxDust.setChecked(true);
        checkboxCO.setChecked(true);
        // Set listeners for checkboxes
        checkboxLive.setOnCheckedChangeListener((buttonView, isChecked) -> updatePredictionChart(jsonString));
        checkboxPredicted.setOnCheckedChangeListener((buttonView, isChecked) -> updatePredictionChart(jsonString));
        checkboxDust.setOnCheckedChangeListener((buttonView, isChecked) -> updatePredictionChart(jsonString));
        checkboxCO.setOnCheckedChangeListener((buttonView, isChecked) -> updatePredictionChart(jsonString));

        // Optionally call the update method initially if needed
        updatePredictionChart(jsonString);
    }





    //for ui setup for device connection
    private void setupDeviceDropdown() {
        deviceList.add("Select Device");
        deviceList.add("Add New Device");

        if (deviceDropdown == null) {
            Log.e("HomeFragment", "deviceDropdown is NULL! Check fragment_home.xml ID.");
            return;
        }

        deviceAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, deviceList);
        deviceDropdown.setAdapter(deviceAdapter);

        deviceDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == deviceList.size() - 1) { // "Add New Device" selected
                    addDeviceLayout.setVisibility(View.VISIBLE);
                } else {
                    addDeviceLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupButtonListeners() {
        bluetoothOption.setOnClickListener(v -> {
            addDeviceLayout.setVisibility(View.GONE);
            bluetoothDeviceLayout.setVisibility(View.VISIBLE);
            startScan();
        });

        mqttOption.setOnClickListener(v -> {
            addDeviceLayout.setVisibility(View.GONE);
            mqttDeviceLayout.setVisibility(View.VISIBLE);
        });

        cancelOption.setOnClickListener(v -> addDeviceLayout.setVisibility(View.GONE));

        okButton.setOnClickListener(v -> {
            String deviceId = deviceIdInput.getText().toString().trim();
            if (!deviceId.isEmpty()) {
                deviceList.add(deviceId + " (MQTT)");
                deviceAdapter.notifyDataSetChanged();
                mqttDeviceLayout.setVisibility(View.GONE);
            }
        });

        backButtonMqtt.setOnClickListener(v -> mqttDeviceLayout.setVisibility(View.GONE));

        scanButton.setOnClickListener(v -> startScan());

        backButtonBluetooth.setOnClickListener(v -> bluetoothDeviceLayout.setVisibility(View.GONE));
    }




    // Request Bluetooth permissions at runtime (for Android 12+)
    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN},
                        1);
            }
        }
    }



//    private void setupMqttDevice() {
//        String clientId = UUID.randomUUID().toString();
//
//        mqttClient = new MqttAndroidClient(requireContext().getApplicationContext(), MQTT_BROKER_URL, clientId, Ack.AUTO_ACK);
//
//        mqttClient.setCallback(new MqttCallbackExtended() {
//            @Override
//            public void connectComplete(boolean reconnect, String serverURI) {
//                Log.d("MQTT", "Connected. Reconnect: " + reconnect);
//                if (reconnect) {
//                    subscribeToDevice(MQTT_DEVICE);
//                }
//            }
//
//            @Override
//            public void connectionLost(Throwable cause) {
//                Log.e("MQTT", "Connection lost: " + (cause != null ? cause.getMessage() : "Unknown error"));
//                statusTextView.setText("Connection lost. Reconnecting...");
//                handler.postDelayed(() -> setupMqttDevice(), 2000);
//            }
//
//            @Override
//            public void messageArrived(String topic, MqttMessage message) {
//                String receivedMessage = new String(message.getPayload());
//                Log.d("MQTT", "Message received: " + receivedMessage);
//                statusTextView.setText("Message received: " + receivedMessage);
//            }
//
//            @Override
//            public void deliveryComplete(IMqttDeliveryToken token) {
//                Log.d("MQTT", "Message delivery completed");
//            }
//        });
//
//        MqttConnectOptions options = new MqttConnectOptions();
//        options.setCleanSession(true);
//        options.setAutomaticReconnect(true);
//        options.setKeepAliveInterval(60); // Ensure keep-alive interval is set
//
//        try {
//            mqttClient.connect(options, null, new IMqttActionListener() {
//                @Override
//                public void onSuccess(IMqttToken asyncActionToken) {
//                    Log.d("MQTT", "Connected to MQTT broker");
//                    statusTextView.setText("Connected to MQTT broker");
//                    subscribeToDevice(MQTT_DEVICE);
//                }
//
//                @Override
//                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                    Log.e("MQTT", "Failed to connect: " + exception.getMessage());
//                    statusTextView.setText("Failed to connect: " + exception.getMessage());
//                }
//            });
//
//        } catch (Exception e) {
//            Log.e("MQTT", "Error setting up MQTT client: " + e.getMessage());
//            statusTextView.setText("Error setting up MQTT client.");
//        }
//    }
//
//    // Method to subscribe to a topic
//    private void subscribeToDevice(String MQTT_DEVICE) {
//        try {
//            mqttClient.subscribe(MQTT_DEVICE, 1, null, new IMqttActionListener() {
//                @Override
//                public void onSuccess(IMqttToken asyncActionToken) {
//                    Log.d("MQTT", "Successfully subscribed to topic: " + MQTT_DEVICE);
//                    statusTextView.setText("Subscribed to topic: " + MQTT_DEVICE);
//                }
//
//                @Override
//                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                    Log.e("MQTT", "Failed to subscribe to topic: " + MQTT_DEVICE + ", " + exception.getMessage());
//                    statusTextView.setText("Failed to subscribe to topic.");
//                }
//            });
//        } catch (Exception e) {
//            Log.e("MQTT", "Error subscribing to topic: " + e.getMessage());
//        }
//    }

    // Setup the chart with smooth lines
    private void setupliveChart() {
        if (liveLineChart == null) {
            Log.e("HomeFragment", "setupChart: LineChart is NULL");
            return;
        }
        Log.d("HomeFragment", "Initializing chart setup");

        // Enable interactions
        liveLineChart.setPinchZoom(true);
        liveLineChart.setScaleEnabled(true);
        liveLineChart.setDragEnabled(true);

        // Customize chart appearance
        liveLineChart.getDescription().setEnabled(false); // Remove chart description
        liveLineChart.setBackgroundColor(Color.WHITE); // Set background color
        liveLineChart.setBorderColor(Color.BLACK); // Set border color

        // Configure X-axis
        XAxis xAxis = liveLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        // Configure Y-axis
        YAxis leftAxis = liveLineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);

        YAxis rightAxis = liveLineChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Set viewport offsets
        liveLineChart.setViewPortOffsets(50f, 50f, 50f, 50f);

        Log.d("HomeFragment", "Chart setup completed");
    }

    // Function to create smooth line datasets
    private LineDataSet createSmoothLineDataSet(List<Entry> data, String label, int color, float lineWidth) {
        LineDataSet dataSet = new LineDataSet(data, label);
        dataSet.setColor(color);
        dataSet.setLineWidth(lineWidth);
        dataSet.setDrawCircles(false); // Disable data point circles for a cleaner look
        dataSet.setDrawValues(false); // Disable values over points
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Enable smooth lines
        return dataSet;
    }


    private void setupDatePicker() {
        Log.d("HomeFragment", "Initializing date picker");

        MaterialDatePicker.Builder<Pair<Long, Long>> materialDateBuilder = MaterialDatePicker.Builder.dateRangePicker();
        materialDateBuilder.setTitleText("SELECT A DATE");

        final MaterialDatePicker<Pair<Long, Long>> materialDatePicker = materialDateBuilder.build();

        mPickDateButton.setOnClickListener(v -> materialDatePicker.show(getParentFragmentManager(), "MATERIAL_DATE_PICKER"));

        materialDatePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection.first != null && selection.second != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

                // Append default time (Start of day and end of day)
                String startDate = sdf.format(new Date(selection.first)).split(" ")[0] + " 00:00:00";
                String endDate = sdf.format(new Date(selection.second)).split(" ")[0] + " 23:59:59";

                Log.d("HomeFragment", "Selected date range: " + startDate + " to " + endDate);

                String selectedDate = "Selected Date: " + materialDatePicker.getHeaderText();
                mShowSelectedDateText.setText(selectedDate);

                // Send formatted request
                fetchData(startDate, endDate);
            } else {
                Log.e("HomeFragment", "Date selection was null");
            }
        });
    }


    private void fetchData(String startDate, String endDate) {
        isHistoricalMode = true;
        OkHttpClient client = new OkHttpClient();
        JSONObject jsonBody = new JSONObject();

        try {
            jsonBody.put("start", startDate);
            jsonBody.put("end", endDate);
            jsonBody.put("nodeValue", "1192"); // Static node value as required
            Log.d("API_REQUEST", "Sent JSON: " + jsonBody.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("API_ERROR", "Request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        Log.d("API_RESPONSE", "Received JSON: " + responseString);
                        JSONObject jsonResponse = new JSONObject(responseString);
                        parseAndStoreData(jsonResponse);
                    } catch (JSONException e) {
                        Log.e("API_ERROR", "JSON Parsing error: " + e.getMessage());
                    }
                } else {
                    Log.e("API_ERROR", "Response unsuccessful: " + response.code());
                }
            }
        });
    }

    private void parseAndStoreData(JSONObject response) {
        sensorData.clear();
        JSONArray dataArray = response.optJSONArray("data");

        if (dataArray == null || dataArray.length() == 0) {
            Log.e("API_ERROR", "No data received.");
            return;
        }

        int dataSize = dataArray.length();
        int targetSize = 100;  // Ensure at least 100 meaningful points
        int step = Math.max(1, dataSize / targetSize);
        Log.d("DATA_PROCESS", "Total received data points: " + dataSize + ", Step: " + step);

        // Averaging nearby values
        for (int i = 0; i < dataSize; i += step) {
            double sumTemperature = 0, sumHumidity = 0, sumPressure = 0, sumPM1 = 0;
            double sumPM2_5 = 0, sumPM10 = 0, sumCO = 0, sumVOC = 0, sumCO2 = 0;
            int count = 0;

            for (int j = i; j < i + step && j < dataSize; j++) {
                JSONObject dataObject = dataArray.optJSONObject(j);
                if (dataObject == null) continue;

                JSONObject activityData = dataObject.optJSONObject("activityData");
                if (activityData == null) continue;

                JSONObject dataPoint = activityData.optJSONObject("data");
                if (dataPoint == null) continue;

                sumTemperature += dataPoint.optDouble("temperature", 0);
                sumHumidity += dataPoint.optDouble("humidity", 0);
                sumPressure += dataPoint.optDouble("pressure", 0);
                sumPM1 += dataPoint.optDouble("pm1", 0);
                sumPM2_5 += dataPoint.optDouble("pm2_5", 0);
                sumPM10 += dataPoint.optDouble("pm10", 0);
                sumCO += dataPoint.optDouble("co", 0);
                sumVOC += dataPoint.optDouble("voc", 0);
                sumCO2 += dataPoint.optDouble("co2", 0);
                count++;
            }

            if (count > 0) {
                float xValue = i / (float) step;  // Ensuring proper x-value spacing

                addDataEntry("temperature", xValue, sumTemperature / count);
                addDataEntry("humidity", xValue, sumHumidity / count);
                addDataEntry("pressure", xValue, sumPressure / count);
                addDataEntry("pm1", xValue, sumPM1 / count);
                addDataEntry("pm2.5", xValue, sumPM2_5 / count);
                addDataEntry("pm10", xValue, sumPM10 / count);
                addDataEntry("co", xValue, sumCO / count);
                addDataEntry("voc", xValue, sumVOC / count);
                addDataEntry("co2", xValue, sumCO2 / count);
            }
        }

        Log.d("HomeFragment", "Data successfully parsed and stored. Displaying " + sensorData.get("temperature").size() + " points.");
    }
    private void addDataEntry(String key, float x, double y) {
        if (!sensorData.containsKey(key)) {
            sensorData.put(key, new ArrayList<>());
        }
        sensorData.get(key).add(new Entry(x, (float) y));
    }
    // Adds datasets for historical data
    private void addHistoricalDataSets(LineData data) {
        if (tempCheckbox.isChecked() && sensorData.containsKey("temperature") && !sensorData.get("temperature").isEmpty()) {
            data.addDataSet(createDataSet(sensorData.get("temperature"), "Temperature", Color.RED));
        }
        if (humCheckbox.isChecked() && sensorData.containsKey("humidity") && !sensorData.get("humidity").isEmpty()) {
            data.addDataSet(createDataSet(sensorData.get("humidity"), "Humidity", Color.BLUE));
        }
        if (co2Checkbox.isChecked() && sensorData.containsKey("co2") && !sensorData.get("co2").isEmpty()) {
            data.addDataSet(createDataSet(sensorData.get("co2"), "CO2", Color.GREEN));
        }
        if (pressCheckbox.isChecked() && sensorData.containsKey("pressure") && !sensorData.get("pressure").isEmpty()) {
            data.addDataSet(createDataSet(sensorData.get("pressure"), "Pressure", Color.YELLOW));
        }
        if (vocCheckbox.isChecked() && sensorData.containsKey("voc") && !sensorData.get("voc").isEmpty()) {
            data.addDataSet(createDataSet(sensorData.get("voc"), "VOC", Color.MAGENTA));
        }
        if (coCheckbox.isChecked() && sensorData.containsKey("co") && !sensorData.get("co").isEmpty()) {
            data.addDataSet(createDataSet(sensorData.get("co"), "CO", Color.CYAN));
        }
        if (pm1Checkbox.isChecked() && sensorData.containsKey("pm1") && !sensorData.get("pm1").isEmpty()) {
            data.addDataSet(createDataSet(sensorData.get("pm1"), "PM1", Color.DKGRAY));
        }
        if (pm2Checkbox.isChecked() && sensorData.containsKey("pm2.5") && !sensorData.get("pm2.5").isEmpty()) {
            data.addDataSet(createDataSet(sensorData.get("pm2.5"), "PM2.5", Color.LTGRAY));
        }
        if (pm10Checkbox.isChecked() && sensorData.containsKey("pm10") && !sensorData.get("pm10").isEmpty()) {
            data.addDataSet(createDataSet(sensorData.get("pm10"), "PM10", Color.BLACK));
        }
    }
    private void setupChart() {
        if (lineChart == null) {
            Log.e("HomeFragment", "setupChart: LineChart is NULL");
            return;
        }
        Log.d("HomeFragment", "Initializing chart setup");

        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        lineChart.setViewPortOffsets(50f, 50f, 50f, 50f);
        lineChart.getXAxis().setAxisMinimum(0f);
        lineChart.getXAxis().setAxisMaximum(10f);
        lineChart.getAxisLeft().setAxisMinimum(0f);
        lineChart.getAxisLeft().setAxisMaximum(100f);

        Log.d("HomeFragment", "Chart setup completed");
    }

    private void initCheckBoxes(View view) {
        tempCheckbox = view.findViewById(R.id.checkboxTemperature);
        humCheckbox = view.findViewById(R.id.checkboxHumidity);
        co2Checkbox = view.findViewById(R.id.checkboxCO2);
        pressCheckbox = view.findViewById(R.id.checkboxPressure);
        vocCheckbox = view.findViewById(R.id.checkboxVOC);
        coCheckbox = view.findViewById(R.id.checkboxCO);
        pm1Checkbox = view.findViewById(R.id.checkboxPM1);
        pm2Checkbox = view.findViewById(R.id.checkboxPM2_5);
        pm10Checkbox = view.findViewById(R.id.checkboxPM10);

        tempCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateChart());
        humCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateChart());
        co2Checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateChart());
        pressCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateChart());
        vocCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateChart());
        coCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateChart());
        pm1Checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateChart());
        pm2Checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateChart());
        pm10Checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateChart());


    }
    // Method to add new data to the buffer and update the chart
    public void addDataToBuffer(String jsonString) {
        try {
            JSONObject jsonData = new JSONObject(jsonString);
            JSONObject data = jsonData.getJSONObject("data");

            // Extract sensor values from the JSON object
            addToQueue(temperatureBuffer, (float) data.getDouble("temperature"));
            addToQueue(humidityBuffer, (float) data.getDouble("humidity"));
            addToQueue(pressureBuffer, (float) data.getDouble("pressure"));
            addToQueue(pm1Buffer, (float) data.getDouble("pm1"));
            addToQueue(pm2_5Buffer, (float) data.getDouble("pm2_5"));
            addToQueue(pm10Buffer, (float) data.getDouble("pm10"));
            addToQueue(coBuffer, (float) data.getDouble("co"));
            addToQueue(vocBuffer, (float) data.getDouble("voc"));
            addToQueue(co2Buffer, (float) data.getDouble("co2"));
            logBufferSizes();
            // Call the method to update the chart after adding new data
            updateChart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void logBufferSizes() {
        Log.d("BufferSizes", "Temperature Buffer Size: " + temperatureBuffer.size());
        Log.d("BufferSizes", "Humidity Buffer Size: " + humidityBuffer.size());
        Log.d("BufferSizes", "Pressure Buffer Size: " + pressureBuffer.size());
        Log.d("BufferSizes", "PM1 Buffer Size: " + pm1Buffer.size());
        Log.d("BufferSizes", "PM2.5 Buffer Size: " + pm2_5Buffer.size());
        Log.d("BufferSizes", "PM10 Buffer Size: " + pm10Buffer.size());
        Log.d("BufferSizes", "CO Buffer Size: " + coBuffer.size());
        Log.d("BufferSizes", "VOC Buffer Size: " + vocBuffer.size());
        Log.d("BufferSizes", "CO2 Buffer Size: " + co2Buffer.size());
    }
    // Helper method to add data to a queue and maintain the buffer size
    private void addToQueue(Queue<Float> buffer, float value) {
        if (buffer.size() >= BUFFER_SIZE) {
            buffer.poll();  // Remove the oldest entry if the buffer exceeds size
        }
        buffer.offer(value);  // Add the new value to the queue
    }

    // Method to update the chart with the latest data (called every time new data is added)
    public void updateChart() {
        LineData data = new LineData();

        // Check if it's historical mode or buffer mode
        if (isHistoricalMode) {
            // For historical data
            if (sensorData.isEmpty()) {
                Log.e("ChartUpdate", "No historical data to display.");
                return;
            }
            // Add logic for historical data - ensuring only valid data is shown
            addHistoricalDataSets(data);
        } else {
            // For buffer data (dynamic, real-time)
            addBufferDataSets(data);
        }

        // Set the data to the chart and invalidate it to refresh
        lineChart.setData(data);
        lineChart.invalidate(); // Refresh chart
        Log.d("ChartUpdate", "Updated chart with " + data.getDataSetCount() + " datasets.");
    }

    // Adds datasets for real-time data (default buffer mode)
    private void addBufferDataSets(LineData data) {
        if (tempCheckbox.isChecked() && !temperatureBuffer.isEmpty()) {
            data.addDataSet(createDataSetFromBuffer(temperatureBuffer, "Temperature", Color.RED));
        }
        if (humCheckbox.isChecked() && !humidityBuffer.isEmpty()) {
            data.addDataSet(createDataSetFromBuffer(humidityBuffer, "Humidity", Color.BLUE));
        }
        if (co2Checkbox.isChecked() && !co2Buffer.isEmpty()) {
            data.addDataSet(createDataSetFromBuffer(co2Buffer, "CO2", Color.GREEN));
        }
        if (pressCheckbox.isChecked() && !pressureBuffer.isEmpty()) {
            data.addDataSet(createDataSetFromBuffer(pressureBuffer, "Pressure", Color.YELLOW));
        }
        if (vocCheckbox.isChecked() && !vocBuffer.isEmpty()) {
            data.addDataSet(createDataSetFromBuffer(vocBuffer, "VOC", Color.MAGENTA));
        }
        if (coCheckbox.isChecked() && !coBuffer.isEmpty()) {
            data.addDataSet(createDataSetFromBuffer(coBuffer, "CO", Color.CYAN));
        }
        if (pm1Checkbox.isChecked() && !pm1Buffer.isEmpty()) {
            data.addDataSet(createDataSetFromBuffer(pm1Buffer, "PM1", Color.DKGRAY));
        }
        if (pm2Checkbox.isChecked() && !pm2_5Buffer.isEmpty()) {
            data.addDataSet(createDataSetFromBuffer(pm2_5Buffer, "PM2.5", Color.LTGRAY));
        }
        if (pm10Checkbox.isChecked() && !pm10Buffer.isEmpty()) {
            data.addDataSet(createDataSetFromBuffer(pm10Buffer, "PM10", Color.BLACK));
        }
    }

    // Convert the buffer into a list of Entries for the chart
    private List<Entry> createEntriesFromBuffer(Queue<Float> buffer) {
        List<Entry> entries = new ArrayList<>();
        int index = 0;
        for (Float value : buffer) {
            entries.add(new Entry(index++, value));
        }
        return entries;
    }

    // Create a LineDataSet with entries and style it
    private LineDataSet createDataSetFromBuffer(Queue<Float> buffer, String label, int color) {
        List<Entry> entries = createEntriesFromBuffer(buffer);
        return createDataSet(entries, label, color);
    }

    // Create a LineDataSet with entries and style it
    private LineDataSet createDataSet(List<Entry> entries, String label, int color) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setDrawFilled(false);
        return dataSet;
    }



//    private void setupChart() {
//        if (lineChart != null) {
//            // Enable dragging (panning)
//            lineChart.setDragEnabled(true);
//
//            // Enable scaling (zooming)
//            lineChart.setScaleEnabled(true);
//
//            // Optionally, enable pinch-to-zoom in both directions (x and y axes)
//            lineChart.setPinchZoom(true);
//
//            // Set the chart's X and Y axes to be movable (optional)
//            XAxis xAxis = lineChart.getXAxis();
//            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);  // Optional: Position X-axis at the bottom
//            YAxis leftAxis = lineChart.getAxisLeft();
//            leftAxis.setDrawGridLines(true);  // Optional: Enable grid lines for better visual representation
//            YAxis rightAxis = lineChart.getAxisRight();
//            rightAxis.setEnabled(false);  // Optional: Disable the right axis (if not needed)
//
//            // Setting up viewPort offset to ensure the chart is visible with proper padding
//            lineChart.setViewPortOffsets(50f, 50f, 50f, 50f);
//
//            // Optionally, set the minimum/maximum values for X and Y axes (this does not restrict zoom, but sets axis limits)
//            lineChart.getXAxis().setAxisMinimum(0f); // X-axis minimum value
//            lineChart.getXAxis().setAxisMaximum(10f); // X-axis maximum value
//            lineChart.getAxisLeft().setAxisMinimum(0f); // Y-axis minimum value
//            lineChart.getAxisLeft().setAxisMaximum(100f); // Y-axis maximum value
//        }
//
//    }
//
//    public void updateChart() {
//        if (lineChart != null && getView() != null) {
//            List<ILineDataSet> dataSets = new ArrayList<>();
//
//            // Get the checkboxes for each sensor type
//            CheckBox checkboxTemperature = getView().findViewById(R.id.checkboxTemperature);
//            CheckBox checkboxHumidity = getView().findViewById(R.id.checkboxHumidity);
//            CheckBox checkboxCO2 = getView().findViewById(R.id.checkboxCO2);
//            CheckBox checkboxPressure = getView().findViewById(R.id.checkboxPressure);
//            CheckBox checkboxVOC = getView().findViewById(R.id.checkboxVOC);
//            CheckBox checkboxCO = getView().findViewById(R.id.checkboxCO);
//            CheckBox checkboxPM1 = getView().findViewById(R.id.checkboxPM1);
//            CheckBox checkboxPM2_5 = getView().findViewById(R.id.checkboxPM2_5);
//            CheckBox checkboxPM10 = getView().findViewById(R.id.checkboxPM10);
//
//            // Data limit for smoother graphs (e.g., limit to 100 points)
//            int dataLimit = 100;
//
//            // Helper method to create and customize datasets
//            if (checkboxTemperature.isChecked()) {
//                createAndAddDataSet(dataSets, "Temperature", 1, dataLimit, R.color.colorPrimary, R.color.colorPrimaryLight);
//            }
//
//            if (checkboxHumidity.isChecked()) {
//                createAndAddDataSet(dataSets, "Humidity", 2, dataLimit, R.color.colorAccent, R.color.colorAccentLight);
//            }
//
//            if (checkboxCO2.isChecked()) {
//                createAndAddDataSet(dataSets, "CO2", 3, dataLimit, R.color.colorSecondary, R.color.colorSecondaryLight);
//            }
//
//            if (checkboxPressure.isChecked()) {
//                createAndAddDataSet(dataSets, "Pressure", 4, dataLimit, R.color.colorTertiary, R.color.colorTertiaryLight);
//            }
//
//            if (checkboxVOC.isChecked()) {
//                createAndAddDataSet(dataSets, "VOC", 5, dataLimit, R.color.colorQuaternary, R.color.colorQuaternaryLight);
//            }
//
//            if (checkboxCO.isChecked()) {
//                createAndAddDataSet(dataSets, "CO", 6, dataLimit, R.color.colorFifth, R.color.colorFifthLight);
//            }
//
//            if (checkboxPM1.isChecked()) {
//                createAndAddDataSet(dataSets, "PM1", 7, dataLimit, R.color.colorSixth, R.color.colorSixthLight);
//            }
//
//            if (checkboxPM2_5.isChecked()) {
//                createAndAddDataSet(dataSets, "PM2.5", 8, dataLimit, R.color.colorSeventh, R.color.colorSeventhLight);
//            }
//
//            if (checkboxPM10.isChecked()) {
//                createAndAddDataSet(dataSets, "PM10", 9, dataLimit, R.color.colorEighth, R.color.colorEighthLight);
//            }
//
//            // Update chart with the new data
//            LineData lineData = new LineData(dataSets);
//            lineChart.setData(lineData);
//            lineChart.invalidate(); // Refresh the chart
//        }
//    }

    private void updatePredictionChart(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            Log.e("PredictionChart", "JSON string is null or empty, cannot update chart.");
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONObject calculated = jsonObject.getJSONObject("calculated");
            JSONObject predicted = jsonObject.getJSONObject("predicted");
            updateStatusIcon(calculated);
            BarChart predictionBarChart = getView().findViewById(R.id.predictionBarChart);
            CheckBox checkboxLive = getView().findViewById(R.id.checkboxLive);
            CheckBox checkboxPredicted = getView().findViewById(R.id.checkboxPredicted);
            CheckBox checkboxDust = getView().findViewById(R.id.checkboxpredictionDust);
            CheckBox checkboxCO = getView().findViewById(R.id.checkboxpredictionCO);

            if (predictionBarChart != null) {
                Log.d("PredictionChart", "Updating prediction chart...");
                List<BarEntry> entries = new ArrayList<>();
                List<Integer> colors = new ArrayList<>();

                boolean showLive = checkboxLive.isChecked();
                boolean showPredicted = checkboxPredicted.isChecked();
                boolean showDust = checkboxDust.isChecked();
                boolean showCO = checkboxCO.isChecked();

                Log.d("PredictionChart", "CheckBox States - Live: " + showLive + ", Predicted: " + showPredicted +
                        ", Dust: " + showDust + ", CO: " + showCO);

                if (!showDust && !showCO) {
                    Log.w("PredictionChart", "No data selected to display (Dust and CO unchecked).");
                    predictionBarChart.clear();
                    return;
                }

                // Handle Live Data
                if (showLive) {
                    if (showDust) {
                        float liveDust = (float) calculated.optInt("aqi_dust", 0);
                        Log.d("PredictionChart", "Live Dust AQI: " + liveDust);
                        entries.add(new BarEntry(entries.size(), liveDust));
                        colors.add(getResources().getColor(R.color.colorLiveDust));
                    }
                    if (showCO) {
                        float liveCO = (float) calculated.optInt("aqi_co", 0);
                        Log.d("PredictionChart", "Live CO AQI: " + liveCO);
                        entries.add(new BarEntry(entries.size(), liveCO));
                        colors.add(getResources().getColor(R.color.colorLiveCO));
                    }
                }

                // Handle Predicted Data
                if (showPredicted) {
                    if (showDust) {
                        float predictedDust = (float) predicted.optInt("aqi_dust", 0);
                        Log.d("PredictionChart", "Predicted Dust AQI: " + predictedDust);
                        entries.add(new BarEntry(entries.size(), predictedDust));
                        colors.add(getResources().getColor(R.color.colorPredictedDust));
                    }
                    if (showCO) {
                        float predictedCO = (float) predicted.optInt("aqi_co", 0);
                        Log.d("PredictionChart", "Predicted CO AQI: " + predictedCO);
                        entries.add(new BarEntry(entries.size(), predictedCO));
                        colors.add(getResources().getColor(R.color.colorPredictedCO));
                    }
                }

                // Log Entries for debugging
                for (int i = 0; i < entries.size(); i++) {
                    Log.d("PredictionChart", "BarEntry " + i + ": x = " + entries.get(i).getX() + ", y = " + entries.get(i).getY());
                }

                // Configure BarDataSet
                BarDataSet dataSet = new BarDataSet(entries, "AQI Data");
                dataSet.setColors(colors);
                dataSet.setValueTextSize(12f);

                // Update BarChart
                BarData data = new BarData(dataSet);
                predictionBarChart.setData(data);
                predictionBarChart.invalidate();
                Log.d("PredictionChart", "Chart updated successfully.");
            }
        } catch (JSONException e) {
            Log.e("PredictionChart", "JSON Parsing error: " + e.toString());
        }
    }

    private void updateStatusIcon(JSONObject calculated) {
        try {
            // Find the highest value in the 'calculated' JSONObject
            int highestValue = 0;
            Iterator<String> keys = calculated.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                int value = calculated.optInt(key, 0);
                if (value > highestValue) {
                    highestValue = value;
                }
            }

            // Determine the image resource and AQI description based on the highest value
            int resourceId;
            String statusDescription;

            if (highestValue == 1) {
                resourceId = R.drawable.image1;
                statusDescription = "Good";
            } else if (highestValue == 2) {
                resourceId = R.drawable.image2;
                statusDescription = "Moderate";
            } else if (highestValue == 3) {
                resourceId = R.drawable.image3;
                statusDescription = "Satisfactory";
            } else if (highestValue == 4) {
                resourceId = R.drawable.image4;
                statusDescription = "Poor";
            } else if (highestValue == 5) {
                resourceId = R.drawable.image5;
                statusDescription = "Very Poor";
            } else {
                resourceId = R.drawable.image6;
                statusDescription = "Severe";
            }

            // Update the ImageView and TextView
            ImageView statusIcon = getView().findViewById(R.id.statusIcon);
            TextView statusText = getView().findViewById(R.id.statusText);

            if (statusIcon != null) {
                statusIcon.setImageResource(resourceId);
            }
            if (statusText != null) {
                statusText.setText("AQI Level: " + highestValue + " " + statusDescription);
            }
        } catch (Exception e) {
            Log.e("StatusIcon", "Error updating status icon: " + e.getMessage());
        }
    }


    // Helper method to create and add a LineDataSet with smooth lines
//    private void createAndAddDataSet(List<ILineDataSet> dataSets, String label, int columnIndex, int dataLimit, int color, int fillColor) {
//        ArrayList<Entry> entries = SensorDataUtil.getSensorData(getContext(), "sensor_data.csv", columnIndex, dataLimit);
//        LineDataSet dataSet = new LineDataSet(entries, label);
//
//        // Set chart properties for smoothness and style
//        dataSet.setColor(getResources().getColor(color));
//        dataSet.setLineWidth(2f);
//        dataSet.setDrawCircles(false); // Remove point markers for cleaner look
//
//        dataSet.setCubicIntensity(0.2f); // Smoothness intensity (adjust as needed)
//        dataSet.setDrawFilled(true); // Enable gradient fill
//        dataSet.setFillColor(getResources().getColor(fillColor));// Gradient fill color
//        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
//        dataSet.setDrawValues(false); // Remove value labels from data points
//
//        dataSets.add(dataSet);
//    }




    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Code for scanning or updating the device list
    private void updateDeviceList(RxBleDevice device) {
        if (!discoveredDevices.contains(device)) {
            discoveredDevices.add(device);
            recyclerViewAdapter.notifyDataSetChanged();  // Update RecyclerView
        }

        // Add device to Spinner list (if it's not already added)
        String deviceName = device.getName();
        String deviceMac = device.getMacAddress();
        if (!connectedDevicesList.contains(deviceMac)) {
            connectedDevicesList.add(deviceMac);  // Use MAC address for identification
            deviceAdapter.notifyDataSetChanged();  // Update Spinner
        }
    }
    private String extractMacAddress(String deviceInfo) {
        // Assuming deviceInfo is in the format "DeviceName (MAC Address)"
        int startIndex = deviceInfo.indexOf("(");
        int endIndex = deviceInfo.indexOf(")");
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return deviceInfo.substring(startIndex + 1, endIndex);
        }
        return ""; // Return empty string if format is invalid
    }



    private void saveConnectedDevices() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save the connected devices list as a Set
        Set<String> deviceSet = new HashSet<>(connectedDevicesList);
        editor.putStringSet(KEY_CONNECTED_DEVICES, deviceSet);
        editor.apply();
    }

    private void loadConnectedDevices() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        // Retrieve the connected devices as a Set
        Set<String> deviceSet = sharedPreferences.getStringSet(KEY_CONNECTED_DEVICES, new HashSet<>());
        connectedDevicesList.clear();
        connectedDevicesList.addAll(deviceSet);

        // Notify the spinner adapter about the updated data
        if (spinnerAdapter != null) {
            spinnerAdapter.notifyDataSetChanged();
        }
    }
    private void startScan() {
        // Check if statusTextView is null before setting text
        if (statusTextView != null) {
            statusTextView.setText("Scanning...");
        } else {
            Log.e("BLE", "statusTextView is null.");
            return;
        }

        // Check if rxBleClient is initialized
        if (rxBleClient == null) {
            Log.e("BLE", "rxBleClient is not initialized.");
            return;
        }
        bluetoothRecyclerView.setVisibility(View.VISIBLE);
        Disposable scanDisposable = rxBleClient.scanBleDevices()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(scanResult -> {
                    RxBleDevice device = scanResult.getBleDevice();
                    updateDeviceList(device);

                    // Check if the scanned device matches the target device
                    if (device.getMacAddress().equals(targetDeviceMac)) {
                        selectedDevice = device;

                        // Ensure statusTextView is not null before updating text
                        if (statusTextView != null) {
                            statusTextView.setText("Target device found: " + selectedDevice.getName());
                        }
                    }
                }, throwable -> {
                    // Handle scan failure
                    if (statusTextView != null) {
                        statusTextView.setText("Scan failed.");
                    }
                    Log.e("BLE", "Scan failed: " + throwable.toString());
                });

        // Add the disposable to the disposables collection
        disposables.add(scanDisposable);
    }


    private void connectToDevice() {
        // Check if a device MAC address is selected
        if (targetDeviceMac == null || targetDeviceMac.isEmpty()) {
            Log.d("BLE", "No device selected.");
            Toast.makeText(getContext(), "No device selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find the selected device in your BLE client or other data source
        RxBleDevice selectedDevice = rxBleClient.getBleDevice(targetDeviceMac);

        if (selectedDevice == null) {
            Log.d("BLE", "Device not found.");
            Toast.makeText(getContext(), "Device not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("BLE", "Attempting to connect to device: " + selectedDevice.getName() + " - " + selectedDevice.getMacAddress());

        connectionDisposable = selectedDevice.establishConnection(false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        rxBleConnection -> {
                            // Successfully connected
                            connection = rxBleConnection;
                            Log.d("BLE", "Connected to device: " + selectedDevice.getName() + " - " + selectedDevice.getMacAddress());
                            bluetoothRecyclerView.setVisibility(View.GONE);
                            handler.postDelayed(updateTask, 5000); // Start a periodic update task if needed


                            // Add the device to the list and save it
                            String deviceInfo = selectedDevice.getName() + " (" + selectedDevice.getMacAddress() + ")";
                            if (!connectedDevicesList.contains(deviceInfo)) {
                                connectedDevicesList.add(deviceInfo);
                                spinnerAdapter.notifyDataSetChanged(); // Notify adapter for Spinner update
                                saveConnectedDevices();                // Save connected devices
                            }

                            // Reset retry interval after successful connection
                            retryInterval = RETRY_INTERVAL_MS;

                            Toast.makeText(getContext(), "Connected to: " + deviceInfo, Toast.LENGTH_SHORT).show();
                        },
                        throwable -> {
                            // Connection failed
                            Log.e("BLE", "Connection failed for device: " + selectedDevice.getName() + " - " + selectedDevice.getMacAddress(), throwable);
                            statusTextView.setText("Connection failed.");

                            // Retry logic with exponential backoff
                            if (retryInterval <= (RETRY_INTERVAL_MS * MAX_RETRIES)) {
                                Log.d("BLE", "Retrying connection in " + retryInterval + "ms");
                                handler.postDelayed(() -> {
                                    Log.d("BLE", "Retrying connection to device...");
                                    connectToDevice(); // Retry the connection
                                }, retryInterval);
                                retryInterval *= 2; // Double the interval for exponential backoff
                            } else {
                                Log.e("BLE", "Max retries reached. Unable to connect to device.");
                                statusTextView.setText("Max retry attempts reached. Unable to connect.");
                            }
                        }
                );
    }





    private void readCharacteristic() {
        if (connection == null) {
            Log.e("BLE", "Connection is null, cannot read characteristic.");
            return;
        }

        Log.d("BLE", "Attempting to read characteristic with UUID: " + CHARACTERISTIC_UUID);

        Disposable readDisposable = connection.readCharacteristic(UUID.fromString(CHARACTERISTIC_UUID))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(characteristicValue -> {
                    Log.d("BLE", "Characteristic read successfully. Value length: " + (characteristicValue != null ? characteristicValue.length : 0));
                    bluetoothRecyclerView.setVisibility(View.GONE);
                    if (characteristicValue == null || characteristicValue.length == 0) {
                        Log.w("BLE", "Received characteristic value is null or empty.");
                        statusTextView.setText("Error: Empty data received.");
                        return;
                    }

                    // Convert the characteristic value to a JSON string
                    jsonString = new String(characteristicValue);
                    Log.d("BLE", "Raw JSON string: " + jsonString);

                    updateUIWithData(jsonString);
                    updatePredictionChart(jsonString);
                    addDataToBuffer(jsonString);

                    try {
                        jsonObject = new JSONObject(jsonString);
                        Log.d("BLE", "JSON object parsed successfully.");

                        int deviceId = jsonObject.getInt("device_id");
                        Log.d("BLE", "Extracted device_id: " + deviceId);

                         MQTT_TOPIC = "data/" + deviceId;
                        Log.d("BLE", "MQTT_TOPIC: " + MQTT_TOPIC);

                        setupHttpPostClient();

                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Error parsing JSON string or extracting deviceId: " + e.getMessage());
                        statusTextView.setText("Error parsing data.");
                    }
                }, throwable -> {
                    Log.e("BLE", "Read failed: " + throwable.toString());
                    statusTextView.setText("Read failed.");
                });

        disposables.add(readDisposable);
    }


//    private void saveDataToCSV(JSONObject jsonObject, String timestamp, double latitude, double longitude) {
//        try {
//            if (csvFile == null) {
//                csvFile = new File(requireContext().getExternalFilesDir(null), "sensor_data.csv");
//            }
//
//            Log.d("CSV", "CSV file path: " + csvFile.getAbsolutePath());
//
//            boolean isNewFile = !csvFile.exists() || csvFile.length() == 0;
//
//            try (FileWriter writer = new FileWriter(csvFile, true)) {
//                if (isNewFile) {
//                    Log.d("CSV", "Writing headers to CSV...");
//                    writer.append("Timestamp,Temperature,Humidity,Pressure,PM1,PM2.5,PM10,CO,VOC,CO2,aqi_dust,aqi_co,aqi_voc,aqi_co2,aqi_dust_predicted,aqi_co_predicted,dust_status,co_status,Latitude,Longitude\n");
//                }
//
//                Log.d("CSV", "Parsing JSON object...");
//                JSONObject data = jsonObject.getJSONObject("data");
//                JSONObject calculated = jsonObject.getJSONObject("calculated");
//                JSONObject predicted = jsonObject.getJSONObject("predicted");
//                JSONObject status = jsonObject.getJSONObject("status");
//
//                Log.d("CSV", "Building row...");
//                String row = timestamp + "," +
//                        data.optDouble("temperature", 0) + "," +
//                        data.optDouble("humidity", 0) + "," +
//                        data.optDouble("pressure", 0) + "," +
//                        data.optInt("pm1", 0) + "," +
//                        data.optInt("pm2_5", 0) + "," +
//                        data.optInt("pm10", 0) + "," +
//                        data.optInt("co", 0) + "," +
//                        data.optInt("voc", 0) + "," +
//                        data.optInt("co2", 0) + "," +
//                        calculated.optInt("aqi_dust", 0) + "," +
//                        calculated.optInt("aqi_co", 0) + "," +
//                        calculated.optInt("aqi_voc", 0) + "," +
//                        calculated.optInt("aqi_co2", 0) + "," +
//                        predicted.optInt("aqi_dust", 0) + "," +
//                        predicted.optInt("aqi_co", 0) + "," +
//                        status.optInt("dust", 0) + "," +
//                        status.optInt("co", 0) + "," +
//                        latitude + "," +
//                        longitude + "\n";
//
//                Log.d("CSV", "Appending data row: " + row);
//                writer.append(row);
//                writer.flush();
//                Log.d("CSV", "Data successfully written to: " + csvFile.getAbsolutePath());
//            }
//
//        } catch (IOException | JSONException e) {
//            Log.e("CSV", "Error writing to CSV: ", e);
//        }
//    }
//
//
//
//    private void exportDataToCSV() {
//        if (csvFile != null && csvFile.exists()) {
//            Uri fileUri = FileProvider.getUriForFile(
//                    requireContext(), getContext().getPackageName() + ".provider", csvFile);
//            Intent intent = new Intent(Intent.ACTION_SEND);
//            intent.setType("text/csv");
//            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
//            startActivity(Intent.createChooser(intent, "Share CSV File"));
//        } else {
//            Toast.makeText(requireContext(), "CSV file not available.", Toast.LENGTH_SHORT).show();
//        }
//    }

    private void updateUIWithData(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            Log.e("HomeFragment", "Received null or empty JSON string");
            return;
        }

        try {

            jsonObject = new JSONObject(jsonString);
            JSONObject data = jsonObject.getJSONObject("data");

            CheckBox liveCheckboxTemperature = getView().findViewById(R.id.LivecheckboxTemperature);
            CheckBox liveCheckboxHumidity = getView().findViewById(R.id.LivecheckboxHumidity);
            CheckBox liveCheckboxCO2 = getView().findViewById(R.id.LivecheckboxCO2);
            CheckBox liveCheckboxPressure = getView().findViewById(R.id.LivecheckboxPressure);
            CheckBox liveCheckboxVOC = getView().findViewById(R.id.LivecheckboxVOC);
            CheckBox liveCheckboxCO = getView().findViewById(R.id.LivecheckboxCO);
            CheckBox liveCheckboxPM1 = getView().findViewById(R.id.LivecheckboxPM1);
            CheckBox liveCheckboxPM2_5 = getView().findViewById(R.id.LivecheckboxPM2_5);
            CheckBox liveCheckboxPM10 = getView().findViewById(R.id.LivecheckboxPM10);

            liveCheckboxTemperature.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIWithData(jsonString));
            liveCheckboxHumidity.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIWithData(jsonString));
            liveCheckboxPressure.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIWithData(jsonString));
            liveCheckboxPM1.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIWithData(jsonString));
            liveCheckboxPM2_5.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIWithData(jsonString));
            liveCheckboxPM10.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIWithData(jsonString));
            liveCheckboxCO.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIWithData(jsonString));
            liveCheckboxVOC.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIWithData(jsonString));
            liveCheckboxCO2.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIWithData(jsonString));



//            tempValue.setText(String.format("%.2f", data.getDouble("temperature")));
//            humValue.setText(String.format("%.2f", data.getDouble("humidity")));
//            pressValue.setText(String.format("%.2f", data.getDouble("pressure")));
//            pm1Value.setText(String.valueOf(data.getInt("pm1")));
//            pm2Value.setText(String.valueOf(data.getInt("pm2_5")));
//            pm10Value.setText(String.valueOf(data.getInt("pm10")));
//            coValue.setText(String.valueOf(data.getInt("co")));
//            vocValue.setText(String.valueOf(data.getInt("voc")));
//            co2Value.setText(String.valueOf(data.getInt("co2")));



           // timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            //saveDataToCSV(jsonObject, timestamp, latitude, longitude);

            // Scaling factors for each parameter (modify as needed)
            double scaledTemp = data.optDouble("temperature", 0) / 50.0;
            double scaledHumidity = data.optDouble("humidity", 0) / 100.0;
            double scaledPressure = data.optDouble("pressure", 0) / 1000.0;
            double scaledPm1 = data.optInt("pm1", 0) / 5.0;
            double scaledPm2_5 = data.optInt("pm2_5", 0) / 5.0;
            double scaledPm10 = data.optInt("pm10", 0) / 5.0;
            double scaledCo = data.optDouble("co", 0) / 100.0;
            double scaledVoc = data.optDouble("voc", 0) / 100.0;
            double scaledCo2 = data.optDouble("co2", 0) / 100.0;

            // Add scaled data to respective data lists for chart plotting
            tempData.add(new Entry(tempData.size(), (float) scaledTemp));
            humData.add(new Entry(humData.size(), (float) scaledHumidity));
            pressData.add(new Entry(pressData.size(), (float) scaledPressure));
            pm1Data.add(new Entry(pm1Data.size(), (float) scaledPm1));
            pm2Data.add(new Entry(pm2Data.size(), (float) scaledPm2_5));
            pm10Data.add(new Entry(pm10Data.size(), (float) scaledPm10));
            coData.add(new Entry(coData.size(), (float) scaledCo));
            vocData.add(new Entry(vocData.size(), (float) scaledVoc));
            co2Data.add(new Entry(co2Data.size(), (float) scaledCo2));

            // Create and add datasets based on checkbox selections
            List<ILineDataSet> dataSets = new ArrayList<>();
            if (liveCheckboxTemperature.isChecked()) {
                dataSets.add(createSmoothLineDataSet(tempData, "Temperature", Color.BLUE, 3.0f));
            }
            if (liveCheckboxHumidity.isChecked()) {
                dataSets.add(createSmoothLineDataSet(humData, "Humidity", Color.GREEN, 3.0f));
            }
            if (liveCheckboxPressure.isChecked()) {
                dataSets.add(createSmoothLineDataSet(pressData, "Pressure", Color.YELLOW, 3.0f));
            }
            if (liveCheckboxPM1.isChecked()) {
                dataSets.add(createSmoothLineDataSet(pm1Data, "PM1", Color.CYAN, 3.0f));
            }
            if (liveCheckboxPM2_5.isChecked()) {
                dataSets.add(createSmoothLineDataSet(pm2Data, "PM2.5", Color.MAGENTA, 3.0f));
            }
            if (liveCheckboxPM10.isChecked()) {
                dataSets.add(createSmoothLineDataSet(pm10Data, "PM10", Color.GRAY, 3.0f));
            }
            if (liveCheckboxCO.isChecked()) {
                dataSets.add(createSmoothLineDataSet(coData, "CO", Color.RED, 3.0f));
            }
            if (liveCheckboxVOC.isChecked()) {
                dataSets.add(createSmoothLineDataSet(vocData, "VOC", Color.LTGRAY, 3.0f));
            }
            if (liveCheckboxCO2.isChecked()) {
                dataSets.add(createSmoothLineDataSet(co2Data, "CO2", Color.DKGRAY, 3.0f));
            }

            // Set new data and refresh the chart
            LineData lineData = new LineData(dataSets);
            liveLineChart.setData(lineData);
            liveLineChart.invalidate(); // Refresh the chart
        } catch (JSONException e) {
            statusTextView.setText("Invalid data received.");
            Log.e("BLE", "JSON Parsing error: " + e.toString());
        }
    }
    private LineDataSet createLineDataSet(List<Entry> data, String label, int color, float lineWidth) {
        LineDataSet dataSet = new LineDataSet(data, label);
        dataSet.setColor(color);
        dataSet.setLineWidth(lineWidth);
        dataSet.setDrawCircles(false); // Optionally, hide circles at data points
        return dataSet;
    }


    private void setupHttpPostClient() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("HTTP", "Running setupHttpPostClient: Simulating data reception...");
                simulateDataReception();
                handler.postDelayed(this, 10000);
            }
        }, 10000);
    }

    private void simulateDataReception() {
        Log.d("HTTP", "simulateDataReception: Checking jsonString...");

        if (jsonString == null || jsonString.isEmpty()) {
            Log.e("HTTP", "jsonString is null or empty. Skipping HTTP request.");
            return;
        }

        try {
            Log.d("HTTP", "simulateDataReception: Parsing JSON...");
            JSONObject jsonObject = new JSONObject(jsonString);

            // Extract device_id
            int deviceId = jsonObject.optInt("device_id", -1);

            // Generate timestamp
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            // Create new data object with device_id and timestamp
            JSONObject newDataObject = new JSONObject();
            newDataObject.put("device_id", deviceId);
            newDataObject.put("timestamp", timestamp);

            // Extract existing data structure dynamically
            if (jsonObject.has("data")) {
                newDataObject.put("data", jsonObject.getJSONObject("data"));
            }

            if (jsonObject.has("calculated")) {
                newDataObject.put("calculated", jsonObject.getJSONObject("calculated"));
            }

            if (jsonObject.has("predicted")) {
                newDataObject.put("predicted", jsonObject.getJSONObject("predicted"));
            }

            if (jsonObject.has("status")) {
                newDataObject.put("status", jsonObject.getJSONObject("status"));
            }

            // Convert newDataObject to a JSON string
            String dataString = newDataObject.toString();

            // Generate client and topic dynamically
            String client = "client" + deviceId;
            String topic = "data/" + deviceId;

            // Construct the final JSON object
            JSONObject newJson = new JSONObject();
            newJson.put("data", dataString);  // Stringified JSON
            newJson.put("client", client);
            newJson.put("topic", topic);

            Log.d("HTTP", "simulateDataReception: Final JSON: " + newJson.toString());

            if (dataString.equals(lastSentData)) {
                Log.d("HTTP", "Duplicate data detected, skipping HTTP POST.");
                return; // Skip sending duplicate data
            }

            lastSentData = dataString; // Update last sent data reference

            sendHttpPost(newJson.toString());
        } catch (JSONException e) {
            Log.e("HTTP", "JSON parsing error: " + e.getMessage());
        }
    }

    private void sendHttpPost(String jsonPayload) {
        new Thread(() -> {
            try {
                Log.d("HTTP", "sendHttpPost: Preparing HTTP POST request...");
                URL url = new URL(HTTP_ENDPOINT);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                Log.d("HTTP", "sendHttpPost: Sending JSON: " + jsonPayload);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                Log.d("HTTP", "sendHttpPost: Server response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("HTTP", "Data successfully sent to server.");
                    handler.post(() -> statusTextView.setText("Data successfully sent."));
                } else {
                    Log.e("HTTP", "Failed to send data. Response Code: " + responseCode);
                    handler.post(() -> statusTextView.setText("Failed to send data."));
                }
                connection.disconnect();
            } catch (Exception e) {
                Log.e("HTTP", "Error in HTTP request: " + e.getMessage());
                handler.post(() -> statusTextView.setText("Error in HTTP request."));
            }
        }).start();
    }



    @Override
    public void onResume() {
        super.onResume();


    }
    @Override
    public void onPause() {
        super.onPause();


    }
    private void requestPermissionsIfNecessary(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), permissions, REQUEST_PERMISSIONS_REQUEST_CODE);
                break;
            }
        }
    }




    public static class CSVParser {
        public static List<GeoPoint> parseSensorData(String filePath) {
            List<GeoPoint> coordinates = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)))) {
                String line;
                // Skip header if present
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue; // Skip header
                    }

                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        try {
                            double latitude = Double.parseDouble(parts[0].trim());
                            double longitude = Double.parseDouble(parts[1].trim());
                            coordinates.add(new GeoPoint(latitude, longitude));
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid latitude/longitude format: " + line);
                        }
                    } else {
                        System.err.println("Invalid line format: " + line);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading CSV file: " + e.getMessage());
            }

            return coordinates;
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