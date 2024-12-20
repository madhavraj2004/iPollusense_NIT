package com.example.ipollusen;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.polidea.rxandroidble3.RxBleDevice;

import java.util.List;

public class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder> {

    private List<RxBleDevice> deviceList;
    private OnDeviceClickListener onDeviceClickListener; // Listener to handle click events

    // Constructor to pass the device list and listener
    public BluetoothDeviceAdapter(List<RxBleDevice> deviceList, OnDeviceClickListener onDeviceClickListener) {
        this.deviceList = deviceList;
        this.onDeviceClickListener = onDeviceClickListener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        RxBleDevice device = deviceList.get(position);
        holder.deviceNameTextView.setText(device.getName());
        holder.deviceMacTextView.setText(device.getMacAddress());

        // Handle click event to update the selected device
        holder.itemView.setOnClickListener(v -> {
            if (onDeviceClickListener != null) {
                onDeviceClickListener.onDeviceClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceNameTextView;
        TextView deviceMacTextView;

        public DeviceViewHolder(View itemView) {
            super(itemView);
            deviceNameTextView = itemView.findViewById(R.id.device_name);
            deviceMacTextView = itemView.findViewById(R.id.device_mac);
        }
    }

    // Interface to handle device click events
    public interface OnDeviceClickListener {
        void onDeviceClick(RxBleDevice device);
    }
}
