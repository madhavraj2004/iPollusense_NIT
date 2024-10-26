package com.example.ipollusen;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.polidea.rxandroidble3.RxBleClient;
import com.polidea.rxandroidble3.RxBleConnection;
import com.polidea.rxandroidble3.RxBleDevice;
import com.polidea.rxandroidble3.RxBleScanResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class BluetoothFragment extends Fragment {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String CHARACTERISTIC_UUID = "0000fef4-0000-1000-8000-00805f9b34fb";
    private static final String TARGET_MAC_ADDRESS = "7C:DF:A1:EE:D4:96";
    private static final long READ_INTERVAL_MS = 5000; // 5 seconds
    private static final long RETRY_INTERVAL_MS = 3000; // 3 seconds

    private TextView statusTextView;
    private TextView dataTextView;
    private RecyclerView devicesRecyclerView;
    private DeviceAdapter deviceAdapter;
    private final List<RxBleDevice> deviceList = new ArrayList<>();

    private RxBleClient rxBleClient;
    private RxBleDevice selectedDevice;
    private RxBleConnection connection;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private SharedViewModel sharedViewModel;
    private Disposable scanDisposable;
    private Disposable connectDisposable;
    private Disposable readDisposable;
    private Disposable retryConnectDisposable;

    private final BluetoothReceiver bluetoothReceiver = new BluetoothReceiver();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        statusTextView = view.findViewById(R.id.statusTextView);
        dataTextView = view.findViewById(R.id.label1);
        devicesRecyclerView = view.findViewById(R.id.recyclerView);
        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        deviceAdapter = new DeviceAdapter(deviceList, this::onDeviceSelected);
        devicesRecyclerView.setAdapter(deviceAdapter);

        Button scanButton = view.findViewById(R.id.scanButton);
        Button connectButton = view.findViewById(R.id.connectButton);

        scanButton.setOnClickListener(v -> startScan());
        connectButton.setOnClickListener(v -> connectToDevice());

        // Initialize RxBleClient and ViewModel
        rxBleClient = RxBleClient.create(requireContext());
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Check and request permissions
        checkPermissions();

        // Register BluetoothReceiver
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        requireContext().registerReceiver(bluetoothReceiver, filter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            startScan();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopScan();
        disposeIfNotNull(readDisposable);
        disposeIfNotNull(retryConnectDisposable);
        requireContext().unregisterReceiver(bluetoothReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear(); // Clear all disposables
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
        } else {
            startScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan();
            } else {
                statusTextView.setText("Permissions denied.");
            }
        }
    }

    private void startScan() {
        stopScan(); // Stop any ongoing scan

        statusTextView.setText("Scanning...");
        scanDisposable = rxBleClient.scanBleDevices()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onScanResult, this::onScanError);

        compositeDisposable.add(scanDisposable);
    }

    private void onScanResult(RxBleScanResult scanResult) {
        RxBleDevice device = scanResult.getBleDevice();

        if (TARGET_MAC_ADDRESS.equals(device.getMacAddress())) {
            selectedDevice = device;
            connectToDevice();
        } else if (!deviceList.contains(device)) {
            deviceList.add(device);
            deviceAdapter.notifyDataSetChanged();
        }
    }

    private void onScanError(Throwable throwable) {
        statusTextView.setText("Scan failed.");
        Log.e("BLE", "Scan failed: " + throwable.toString());
    }

    private void stopScan() {
        if (scanDisposable != null && !scanDisposable.isDisposed()) {
            scanDisposable.dispose();
        }
    }

    private void connectToDevice() {
        if (selectedDevice == null) {
            statusTextView.setText("No device selected.");
            return;
        }

        statusTextView.setText("Connecting to " + selectedDevice.getName() + "...");
        connectDisposable = selectedDevice.establishConnection(false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onConnectionEstablished, this::onConnectionFailed);

        compositeDisposable.add(connectDisposable);
    }

    private void onConnectionEstablished(RxBleConnection rxBleConnection) {
        connection = rxBleConnection;
        statusTextView.setText("Connected to " + selectedDevice.getName());

        // Start periodic reading of characteristic
        readDisposable = Schedulers.io().createWorker().schedulePeriodically(() -> {
            if (connection != null) {
                connection.readCharacteristic(UUID.fromString(CHARACTERISTIC_UUID))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onCharacteristicRead, this::onCharacteristicReadFailed);
            }
        }, 0, READ_INTERVAL_MS, TimeUnit.MILLISECONDS);

        compositeDisposable.add(readDisposable);

        // Stop retry connection attempt when connected successfully
        disposeIfNotNull(retryConnectDisposable);
    }

    private void onConnectionFailed(Throwable throwable) {
        statusTextView.setText("Connection failed. Retrying in 3 seconds...");
        Log.e("BLE", "Connection failed: " + throwable.toString());

        // Retry connection after a delay
        retryConnectDisposable = Schedulers.io().createWorker().schedulePeriodically(() -> {
            if (selectedDevice != null) {
                connectToDevice();
            }
        }, RETRY_INTERVAL_MS, RETRY_INTERVAL_MS, TimeUnit.MILLISECONDS);

        compositeDisposable.add(retryConnectDisposable);
    }

    private void onCharacteristicRead(byte[] bytes) {
        String jsonString = new String(bytes);
        Log.d("BLE", "Data received: " + jsonString);
        updateUIWithData(jsonString);
    }

    private void onCharacteristicReadFailed(Throwable throwable) {
        statusTextView.setText("Failed to read characteristic.");
        Log.e("BLE", "Failed to read characteristic: " + throwable.toString());
    }

    private void updateUIWithData(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            double temperature = jsonObject.getDouble("temperature");
            double humidity = jsonObject.getDouble("humidity");
            int no2 = jsonObject.getInt("no2");
            int c2h5oh = jsonObject.getInt("c2h5oh");
            int voc = jsonObject.getInt("voc");
            int co = jsonObject.getInt("co");
            int pm1 = jsonObject.getInt("pm1");
            int pm2_5 = jsonObject.getInt("pm2_5");
            int pm10 = jsonObject.getInt("pm10");

            SensorData sensorData = new SensorData(temperature, humidity, no2, c2h5oh, voc, co, pm1, pm2_5, pm10);

            String parsedData = String.format("Temperature: %.2f°C\nHumidity: %.2f%%\nNO2: %d ppb\nC2H5OH: %d ppb\nVOC: %d ppb\nCO: %d ppb\nPM1: %d µg/m³\nPM2.5: %d µg/m³\nPM10: %d µg/m³",
                    temperature, humidity, no2, c2h5oh, voc, co, pm1, pm2_5, pm10);

            dataTextView.setText(parsedData);
            sharedViewModel.setSensorData(sensorData); // Update ViewModel with new SensorData
        } catch (JSONException e) {
            statusTextView.setText("Failed to parse data.");
            Log.e("BLE", "JSON Parsing error: " + e.toString());
        }
    }


    private void disposeIfNotNull(Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    private void onDeviceSelected(RxBleDevice device) {
        selectedDevice = device;
        connectToDevice();
    }

    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    startScan();
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    stopScan();
                }
            }
        }
    }
}