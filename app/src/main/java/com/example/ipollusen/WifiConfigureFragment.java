package com.example.ipollusen;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import com.polidea.rxandroidble3.RxBleClient;
import com.polidea.rxandroidble3.RxBleConnection;
import com.polidea.rxandroidble3.RxBleDevice;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

public class WifiConfigureFragment extends Fragment {

    private static final String TAG = "WifiConfigureFragment";

    // BLE Constants
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final UUID WRITE_CHARACTERISTIC_UUID = UUID.fromString("0000dead-0000-1000-8000-00805f9b34fb");
    private static final String CHARACTERISTIC_UUID = "0000fef4-0000-1000-8000-00805f9b34fb";

    private RecyclerView recyclerView;
    private BluetoothDeviceAdapter adapter;
    private List<RxBleDevice> bleDeviceList = new ArrayList<>();
    private EditText etSsid, etPassword;
    private RxBleClient rxBleClient;
    private RxBleDevice selectedDevice;
    private RxBleConnection connection;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MaterialButton scanButton, startButton;
    private TextInputEditText ssidInput, passwordInput;
    private TextView statusTextView;
    private TextView receiveddata;
    private static final int CHUNK_SIZE = 20;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wifi_configure, container, false);

        // Initialize BLE Client
        rxBleClient = RxBleClient.create(requireContext());

        // Initialize UI components
        recyclerView = view.findViewById(R.id.bluetoothRecyclerView);
        scanButton = view.findViewById(R.id.scanButton);
        startButton = view.findViewById(R.id.btnStart);
        ssidInput = view.findViewById(R.id.etSsid);
        passwordInput = view.findViewById(R.id.etPassword);
        statusTextView = view.findViewById(R.id.statusTextView);
        receiveddata = view.findViewById(R.id.receiveddata);

        // Setup RecyclerView
        adapter = new BluetoothDeviceAdapter(bleDeviceList, this::onDeviceSelected);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Set Button Click Listeners
        scanButton.setOnClickListener(v -> startScan());
        startButton.setOnClickListener(v -> sendStartCommand());

        // Check permissions
        checkPermissions();

        return view;
    }

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

    private void startScan() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            statusTextView.setText("Error: Bluetooth is not enabled.");
            Toast.makeText(requireContext(), "Please enable Bluetooth to scan for devices.", Toast.LENGTH_SHORT).show();
            return;
        }

        statusTextView.setText("Scanning for BLE devices...");
        bleDeviceList.clear();
        adapter.notifyDataSetChanged();
        recyclerView.setVisibility(View.VISIBLE); // Show RecyclerView when scanning starts
        Disposable scanDisposable = rxBleClient.scanBleDevices()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(scanResult -> {
                    RxBleDevice device = scanResult.getBleDevice();
                    if (!bleDeviceList.contains(device)) {
                        bleDeviceList.add(device);
                        adapter.notifyItemInserted(bleDeviceList.size() - 1);
                    }
                }, throwable -> {
                    statusTextView.setText("Error: Scan failed.");
                    Log.e(TAG, "Scan failed: " + throwable.toString());
                });

        disposables.add(scanDisposable);
    }

    private void onDeviceSelected(RxBleDevice device) {
        selectedDevice = device;
        statusTextView.setText("Selected device: " + device.getName());
        recyclerView.setVisibility(View.GONE); // Hide RecyclerView on selection
        connectToDevice(); // Automatically connect to the selected device
    }

    private void connectToDevice() {
        if (selectedDevice == null) {
            statusTextView.setText("Error: No device selected.");
            Toast.makeText(requireContext(), "No device selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        statusTextView.setText("Connecting to device: " + selectedDevice.getName());
        Disposable connectionDisposable = selectedDevice.establishConnection(false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(rxBleConnection -> {
                    connection = rxBleConnection;
                    statusTextView.setText("Connected to device: " + selectedDevice.getName());
                    handler.postDelayed(updateTask, 5000);
                }, throwable -> {
                    statusTextView.setText("Error: Connection failed.");
                    Log.e(TAG, "Connection failed: " + throwable.toString());
                });

        disposables.add(connectionDisposable);
    }

    // Method to send the START command
    private void sendStartCommand() {
        String ssid = ssidInput.getText().toString().trim(); // Use ssidInput instead of etSsid
        String password = passwordInput.getText().toString().trim(); // Use passwordInput instead of etPassword

        if (ssid.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "SSID and Password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Send the START keyword first
        sendBleCommand("START");

        // Construct the BLE command for WiFi credentials (SSID and Password)
        String command = "{\"ble_cmd\":\"WIFI_CRED\",\"ssid\":\"" + ssid + "\",\"pass\":\"" + password + "\"}";
        sendBleCommand(command);

        // Send the STOP keyword last
        sendBleCommand("STOP");
        statusTextView.setText("config Data sent");
    }

    // Method to send a BLE command in chunks
    private void sendBleCommand(String command) {
        if (connection == null) {
            Toast.makeText(requireContext(), "Not connected to a device.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert the command string to bytes
        byte[] commandBytes = command.getBytes(StandardCharsets.UTF_8);
        int length = commandBytes.length;

        // Send the command in chunks of CHUNK_SIZE bytes
        for (int i = 0; i < length; i += CHUNK_SIZE) {
            int end = Math.min(length, i + CHUNK_SIZE);
            byte[] chunk = new byte[end - i];
            System.arraycopy(commandBytes, i, chunk, 0, end - i);
            sendBleChunk(chunk);
        }
    }

    // Method to write a single chunk to the BLE device
    private void sendBleChunk(byte[] chunk) {
        if (connection == null) {
            Toast.makeText(requireContext(), "Not connected to a device.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Write the BLE chunk to the WRITE_CHARACTERISTIC_UUID
        Disposable writeDisposable = connection.writeCharacteristic(WRITE_CHARACTERISTIC_UUID, chunk)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new io.reactivex.rxjava3.functions.Consumer<byte[]>() { // Handle success
                            @Override
                            public void accept(byte[] bytes) throws Throwable {
                                Log.d(TAG, "Chunk sent successfully: " + new String(chunk));
                            }
                        },
                        new io.reactivex.rxjava3.functions.Consumer<Throwable>() { // Handle error
                            @Override
                            public void accept(Throwable throwable) throws Throwable {
                                Log.e(TAG, "Failed to send chunk: " + throwable.getMessage());
                            }
                        }
                );

        disposables.add(writeDisposable);
    }


    private void readCharacteristic() {
        if (connection == null) {
            statusTextView.setText("Error: Not connected to a device.");
            Log.e(TAG, "Connection is null, cannot read characteristic.");
            return;
        }

        Disposable readDisposable = connection.readCharacteristic(UUID.fromString(CHARACTERISTIC_UUID))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(characteristicValue -> {
                    String jsonString = new String(characteristicValue);
                    Log.d(TAG, "Characteristic Value: " + jsonString);
                    receiveddata.setText("Received Data: " + jsonString);
                }, throwable -> {
                    receiveddata.setText("Error: Read failed.");
                    Log.e(TAG, "Read failed: " + throwable.toString());
                });

        disposables.add(readDisposable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateTask);
        disposables.clear();
    }
}