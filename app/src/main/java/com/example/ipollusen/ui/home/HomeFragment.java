package com.example.ipollusen.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.ipollusen.BarChartHelper;
import com.example.ipollusen.R;
import com.example.ipollusen.SharedViewModel;
import com.github.mikephil.charting.charts.BarChart;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    private BarChart barChartPM1, barChartPM2_5, barChartPM10, barChartCO, barChartNO2, barChartVOC, barChartC2H5OH, barChartTemperature, barChartHumidity;
    private BarChartHelper barChartHelper;
    private SharedViewModel sharedViewModel; // Reference to SharedViewModel

    // Store the last received data
    private Map<String, Float> lastReceivedData = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize BarCharts
        barChartPM1 = root.findViewById(R.id.barChartPM1);
        barChartPM2_5 = root.findViewById(R.id.barChartPM2_5);
        barChartPM10 = root.findViewById(R.id.barChartPM10);
        barChartCO = root.findViewById(R.id.barChartCO);
        barChartNO2 = root.findViewById(R.id.barChartNO2);
        barChartVOC = root.findViewById(R.id.barChartVOC);
        barChartC2H5OH = root.findViewById(R.id.barChartC2H5OH);
        barChartTemperature = root.findViewById(R.id.barChartTemperature);
        barChartHumidity = root.findViewById(R.id.barChartHumidity);

        barChartHelper = new BarChartHelper();

        // Initialize SharedViewModel
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Observe JSON data from SharedViewModel
        sharedViewModel.getData().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String jsonString) {
                updateDataFromBluetooth(jsonString);
            }
        });

        // Initialize with dummy data for testing
        Map<String, Float> dummyData = new HashMap<>();
        dummyData.put("pm1", 35f);
        dummyData.put("pm2_5", 20f);
        dummyData.put("pm10", 18f);
        dummyData.put("co", 5f);
        dummyData.put("no2", 15f);
        dummyData.put("voc", 10f);
        dummyData.put("c2h5oh", 12f);
        dummyData.put("temperature", 28f);
        dummyData.put("humidity", 40f);
        updatePollutantData(dummyData);

        return root;
    }

    private void updatePollutantData(Map<String, Float> data) {
        // Update each BarChart with corresponding data
        barChartHelper.setupBarChart(barChartPM1, "PM1", data.get("pm1"));
        barChartHelper.setupBarChart(barChartPM2_5, "PM2.5", data.get("pm2_5"));
        barChartHelper.setupBarChart(barChartPM10, "PM10", data.get("pm10"));
        barChartHelper.setupBarChart(barChartCO, "CO", data.get("co"));
        barChartHelper.setupBarChart(barChartNO2, "NO2", data.get("no2"));
        barChartHelper.setupBarChart(barChartVOC, "VOC", data.get("voc"));
        barChartHelper.setupBarChart(barChartC2H5OH, "C2H5OH", data.get("c2h5oh"));
        barChartHelper.setupBarChart(barChartTemperature, "Temperature", data.get("temperature"));
        barChartHelper.setupBarChart(barChartHumidity, "Humidity", data.get("humidity"));
    }

    // Method to receive JSON data from BluetoothFragment
    public void updateDataFromBluetooth(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            // Parse data and update last received data
            lastReceivedData.put("temperature", (float) jsonObject.getDouble("temperature"));
            lastReceivedData.put("humidity", (float) jsonObject.getDouble("humidity"));
            lastReceivedData.put("no2", (float) jsonObject.getDouble("no2"));
            lastReceivedData.put("c2h5oh", (float) jsonObject.getDouble("c2h5oh"));
            lastReceivedData.put("voc", (float) jsonObject.getDouble("voc"));
            lastReceivedData.put("co", (float) jsonObject.getDouble("co"));
            lastReceivedData.put("pm1", (float) jsonObject.getDouble("pm1"));
            lastReceivedData.put("pm2_5", (float) jsonObject.getDouble("pm2_5"));
            lastReceivedData.put("pm10", (float) jsonObject.getDouble("pm10"));

            // Update the bar charts with the last received data
            updatePollutantData(lastReceivedData);
        } catch (JSONException e) {
            e.printStackTrace(); // Handle the exception
        }
    }
}
