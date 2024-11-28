package com.example.ipollusen;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.polidea.rxandroidble3.RxBleDevice;

import java.util.List;
import java.util.Map;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
    private List<Map.Entry<String, String>> deviceList; // List of devices as (MAC, nickname) pairs

    public DeviceAdapter(List<Map.Entry<String, String>> deviceList) {
        this.deviceList = deviceList;
    }

    public void updateDevices(List<Map.Entry<String, String>> newDeviceList) {
        this.deviceList = newDeviceList;
        notifyDataSetChanged(); // Notify adapter to refresh the RecyclerView
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Map.Entry<String, String> deviceEntry = deviceList.get(position);
        holder.deviceNameTextView.setText(deviceEntry.getValue()); // Set nickname
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceNameTextView;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceNameTextView = itemView.findViewById(R.id.deviceNameTextView);
        }
    }
}
