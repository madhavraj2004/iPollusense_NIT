package com.example.ipollusen.ui.home;

import android.content.Context;
import android.util.Log;
import com.github.mikephil.charting.data.Entry;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SensorDataUtil {
    public static List<Float> liveDustData = new ArrayList<>();
    public static List<Float> liveCOData = new ArrayList<>();
    // Fetch sensor data, optionally limit the data or calculate averages
    public static ArrayList<Entry> getSensorData(Context context, String fileName, int columnIndex, int dataLimit) {
        ArrayList<Entry> entries = new ArrayList<>();
        File csvFile = new File(context.getExternalFilesDir(null), fileName);

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int count = 0;
            float runningSum = 0;

            // Skip the header line
            reader.readLine();

            while ((line = reader.readLine()) != null && (dataLimit == -1 || count < dataLimit)) {
                String[] columns = line.split(",");
                if (columns.length > columnIndex) {
                    try {
                        // Parse the sensor value from the selected column
                        float value = Float.parseFloat(columns[columnIndex]);
                        runningSum += value;

                        // Calculate running average
                        float average = runningSum / (count + 1);

                        // Add smoothed average entry
                        entries.add(new Entry(count, average));
                        count++;
                    } catch (NumberFormatException e) {
                        Log.e("SensorDataUtil", "Error parsing value: " + e.toString());
                    }
                }
            }

        } catch (IOException e) {
            Log.e("SensorDataUtil", "Error reading CSV file: " + e.toString());
        }

        return entries;
    }
    public static float getAverage(Context context, String fileName, String columnName, boolean isLiveData) {
        float sum = 0;
        int count = 0;

        if (!isLiveData) {
            // Read data from CSV file
            String filePath = context.getExternalFilesDir(null) + "/" + fileName;
            Log.d("SensorDataUtil", "Reading data from CSV file: " + filePath);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)))) {
                String line;
                String[] headers = br.readLine().split(","); // Assuming the first line contains headers
                int columnIndex = Arrays.asList(headers).indexOf(columnName);

                if (columnIndex == -1) {
                    Log.e("SensorDataUtil", "Column not found: " + columnName);
                    return 0;
                }

                Log.d("SensorDataUtil", "Column index for " + columnName + ": " + columnIndex);

                while ((line = br.readLine()) != null) {
                    String[] values = line.split(",");
                    if (!values[columnIndex].isEmpty()) {
                        try {
                            float value = Float.parseFloat(values[columnIndex]);
                            sum += value;
                            count++;
                        } catch (NumberFormatException e) {
                            Log.e("SensorDataUtil", "Invalid number format in column " + columnName + ": " + values[columnIndex]);
                        }
                    }
                }
            } catch (IOException e) {
                Log.e("SensorDataUtil", "Error reading file " + filePath, e);
            }
        } else {
            // Handle live data
            List<Float> liveDataList = columnName.equals("aqi_dust") ? liveDustData : liveCOData;
            Log.d("SensorDataUtil", "Live Data List (" + columnName + "): " + liveDataList);

            if (liveDataList.isEmpty()) {
                Log.w("SensorDataUtil", "No live data found for " + columnName);
                return 0;
            }

            // Iterate through live data and calculate sum
            for (Float value : liveDataList) {
                sum += value;
                count++;
                Log.d("SensorDataUtil", "Adding live value: " + value); // Log each value added to the sum
            }

            Log.d("SensorDataUtil", "Total sum for live " + columnName + ": " + sum + ", Count: " + count);
        }

        float average = count > 0 ? sum / count : 0;
        Log.d("SensorDataUtil", "Average for " + columnName + ": " + average);
        return average;
    }



}