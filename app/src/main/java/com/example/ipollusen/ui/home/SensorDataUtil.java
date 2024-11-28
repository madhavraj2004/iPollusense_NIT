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
    public static List<Float> CalculatedDustData = new ArrayList<>(); // Added declaration
    public static List<Float> CalculatedCOData = new ArrayList<>();   // Added declaration

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
                        float value = Float.parseFloat(columns[columnIndex]);
                        runningSum += value;
                        float average = runningSum / (count + 1);
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

    public static float getAverage(Context context, String fileName, String columnName, boolean isCalculatedData) {
        float sum = 0;
        int count = 0;

        if (!isCalculatedData) {
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
                    if (values.length > columnIndex) { // Check if the current line has enough columns
                        if (!values[columnIndex].isEmpty()) {
                            try {
                                float value = Float.parseFloat(values[columnIndex]);
                                sum += value;
                                count++;
                            } catch (NumberFormatException e) {
                                Log.e("SensorDataUtil", "Invalid number format in column " + columnName + ": " + values[columnIndex]);
                            }
                        }
                    } else {
                        Log.w("SensorDataUtil", "Skipping line due to insufficient columns: " + Arrays.toString(values));
                    }
                }
            } catch (IOException e) {
                Log.e("SensorDataUtil", "Error reading file " + filePath, e);
            }
        } else {
            // Handle Calculated data
            List<Float> calculatedDataList = columnName.equals("aqi_dust_calculated") ? CalculatedDustData : CalculatedCOData;
            Log.d("SensorDataUtil", "Calculated Data List (" + columnName + "): " + calculatedDataList);

            if (calculatedDataList.isEmpty()) {
                Log.w("SensorDataUtil", "No live data found for " + columnName);
                return 0;
            }

            // Iterate through Calculated data and calculate sum
            for (Float value : calculatedDataList) {
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
