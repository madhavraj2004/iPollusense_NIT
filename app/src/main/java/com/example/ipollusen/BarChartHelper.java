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

    // Method to setup a BarChart with a single value
    public void setupBarChart(BarChart barChart, String label, float value) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, value));

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
        barChart.getAxisLeft().setGranularity(1f);
        barChart.getAxisRight().setEnabled(false);
        barChart.getXAxis().setEnabled(false);
        barChart.invalidate(); // Refresh the chart
    }
}
