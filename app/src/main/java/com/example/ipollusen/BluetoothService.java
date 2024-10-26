package com.example.ipollusen;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.polidea.rxandroidble3.RxBleClient;
import com.polidea.rxandroidble3.RxBleConnection;
import com.polidea.rxandroidble3.RxBleDevice;

import java.util.UUID;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

public class BluetoothService extends Service {

    public static final String CHARACTERISTIC_UUID = "0000fef4-0000-1000-8000-00805f9b34fb";
    private static final String TAG = "BluetoothService";

    private RxBleClient rxBleClient;
    private RxBleDevice selectedDevice;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    public void onCreate() {
        super.onCreate();
        rxBleClient = RxBleClient.create(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String deviceAddress = intent.getStringExtra("device_address");
        if (deviceAddress != null) {
            selectedDevice = rxBleClient.getBleDevice(deviceAddress);
            connectToDevice();
        }
        return START_STICKY;
    }

    private void connectToDevice() {
        if (selectedDevice == null) return;

        // Subscribe to the connection observable
        Disposable connectionDisposable = selectedDevice.establishConnection(false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onConnectionEstablished, this::onConnectionFailed);
        compositeDisposable.add(connectionDisposable);
    }

    private void onConnectionEstablished(RxBleConnection rxBleConnection) {
        Log.d(TAG, "Connected to device.");

        // Read characteristic and subscribe to the observable
        Disposable readDisposable = rxBleConnection.readCharacteristic(UUID.fromString(CHARACTERISTIC_UUID))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onCharacteristicRead, this::onReadFailed);
        compositeDisposable.add(readDisposable);
    }

    private void onCharacteristicRead(byte[] data) {
        // Broadcast data to the fragment
        Intent intent = new Intent("BLE_DATA_RECEIVED");
        intent.putExtra("data", data);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void onReadFailed(Throwable throwable) {
        Log.e(TAG, "Read failed: " + throwable.toString());
    }

    private void onConnectionFailed(Throwable throwable) {
        Log.e(TAG, "Connection failed: " + throwable.toString());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear(); // Clear all disposables to prevent memory leaks
    }
}
