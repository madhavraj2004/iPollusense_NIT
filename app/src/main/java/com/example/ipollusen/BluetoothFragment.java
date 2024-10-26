package com.example.ipollusen;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.polidea.rxandroidble3.RxBleClient;
import com.polidea.rxandroidble3.RxBleConnection;
import com.polidea.rxandroidble3.RxBleDevice;
import com.polidea.rxandroidble3.RxBleScanResult;
import com.polidea.rxandroidble3.exceptions.BleScanException;

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
    public static final String CHARACTERISTIC_UUID = "0000fef4-0000-1000-8000-00805f9b34fb";
    private static final String TARGET_MAC_ADDRESS = "7C:DF:A1:EE:D4:96";
    private static final String TAG = "BluetoothFragment";

    private TextView statusTextView;
    private TextView dataTextView;
    private RecyclerView devicesRecyclerView;
    private DeviceAdapter deviceAdapter;
    private final List<RxBleDevice> deviceList = new ArrayList<>();

    private RxBleClient rxBleClient;
    private RxBleDevice selectedDevice;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Disposable scanDisposable;

    private boolean isScanning = false;
    private final Handler scanHandler = new Handler(Looper.getMainLooper());

    private SharedViewModel sharedViewModel;

    private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] dataBytes = intent.getByteArrayExtra("data");
            if (dataBytes != null) {
                String dataString = new String(dataBytes);
                // Update SharedViewModel with the received data
                sharedViewModel.setData(dataString);
                Log.d(TAG, "Data received: " + dataString);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        // Initialize SharedViewModel
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

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

        rxBleClient = RxBleClient.create(requireContext());

        checkPermissions();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Register the data receiver
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(dataReceiver, new IntentFilter("BLE_DATA_RECEIVED"));
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(dataReceiver);
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            startScan();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
        stopScan(); // Ensure the scan is stopped when the fragment is destroyed
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

    private void startScan() {
        if (isScanning) return; // Prevent starting a new scan if already scanning

        isScanning = true;
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
        Log.e(TAG, "Scan failed: " + throwable.toString());
        if (throwable instanceof BleScanException) {
            handleScanError((BleScanException) throwable);
        } else {
            statusTextView.setText("Scan failed.");
            isScanning = false; // Reset scanning flag in case of other errors
        }
    }

    private void handleScanError(BleScanException scanException) {
        Log.e(TAG, "Scan failed: " + scanException.getMessage());

        // Implement a simple delay for retrying scan after an error
        long retryDelay = 2000; // Set a fixed delay (e.g., 2 seconds)
        Log.i(TAG, "Retry scan after: " + retryDelay + " ms");

        scanHandler.postDelayed(() -> {
            isScanning = false; // Reset scanning flag
            startScan(); // Attempt to start the scan again
        }, retryDelay);
    }

    private void stopScan() {
        if (scanDisposable != null && !scanDisposable.isDisposed()) {
            scanDisposable.dispose();
            isScanning = false; // Reset scanning flag
            statusTextView.setText("Scan stopped.");
        }
    }

    private void connectToDevice() {
        if (selectedDevice == null) {
            statusTextView.setText("No device selected.");
            return;
        }

        statusTextView.setText("Connecting to " + selectedDevice.getName() + "...");
        Intent serviceIntent = new Intent(getContext(), BluetoothService.class);
        serviceIntent.putExtra("device_address", selectedDevice.getMacAddress());
        requireContext().startService(serviceIntent);
    }

    private void onDeviceSelected(RxBleDevice device) {
        selectedDevice = device;
        connectToDevice();
    }
}
