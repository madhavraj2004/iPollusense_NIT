package com.example.ipollusen;

import android.graphics.Color;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;

public class BarChartHelper {

    // Method to initialize a BarChart with a single value and label
    public void setupBarChart(BarChart barChart, String label, float initialValue) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, initialValue));

        BarDataSet dataSet = new BarDataSet(entries, label);
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        barChart.setData(data);

        // Customize chart appearance
        Description description = new Description();
        description.setText(label + " Level");
        description.setTextSize(12f);
        barChart.setDescription(description);

        // Set up chart axes
        barChart.getAxisLeft().setGranularity(1f);
        barChart.getAxisRight().setEnabled(false);
        barChart.getXAxis().setEnabled(false);
        barChart.invalidate(); // Refresh the chart
    }

    // Method to update an existing BarChart with a new value
    public void updateChart(BarChart barChart, float newValue) {
        BarData data = barChart.getData();
        if (data != null && data.getDataSetCount() > 0) {
            BarDataSet dataSet = (BarDataSet) data.getDataSetByIndex(0);
            dataSet.clear(); // Clear old values
            dataSet.addEntry(new BarEntry(0, newValue)); // Add new value
            data.notifyDataChanged();
            barChart.notifyDataSetChanged();
            barChart.invalidate(); // Refresh the chart
        }
    }
}
