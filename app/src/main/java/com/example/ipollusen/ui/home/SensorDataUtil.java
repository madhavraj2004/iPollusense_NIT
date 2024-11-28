package com.example.ipollusen.ui.home;

import android.content.Context;
import android.util.Log;
import com.github.mikephil.charting.data.Entry;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class SensorDataUtil {

    // Fetch sensor data, optionally limit the data or calculate averages
    public static ArrayList<Entry> getSensorData(Context context, String filename, int columnIndex, int limit) {
        ArrayList<Entry> entries = new ArrayList<>();
        float sum = 0;
        int count = 0;

        try {
            // Open the CSV file from assets
            InputStream inputStream = context.getAssets().open(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            int index = 0;

            // Skip the header line
            reader.readLine();

            while ((line = reader.readLine()) != null && (limit == -1 || count < limit)) {
                String[] values = line.split(",");

                if (values.length > columnIndex) {
                    try {
                        // Parse the sensor value from the selected column
                        float value = Float.parseFloat(values[columnIndex]);
                        sum += value;
                        count++;

                        // Create an Entry for each data point
                        entries.add(new Entry(index, value)); // x = index, y = value
                    } catch (NumberFormatException e) {
                        e.printStackTrace(); // Handle invalid data
                    }
                }
                index++;
            }
            reader.close();

            // If we read some values, compute the average and add a final entry for the average
            if (count > 0) {
                float average = sum / count;
                entries.add(new Entry(index, average)); // Add average entry
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return entries;
    }
}