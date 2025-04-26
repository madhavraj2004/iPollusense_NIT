package com.example.ipollusen;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;

public class ApplianceJsonAdapter extends RecyclerView.Adapter<ApplianceJsonAdapter.ApplianceJsonViewHolder> {
    private final List<JSONObject> applianceJsonList;

    public ApplianceJsonAdapter(List<JSONObject> applianceJsonList) {
        this.applianceJsonList = applianceJsonList;
    }

    @NonNull
    @Override
    public ApplianceJsonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appliance_json, parent, false);
        return new ApplianceJsonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplianceJsonViewHolder holder, int position) {
        JSONObject appliance = applianceJsonList.get(position);
        try {
            // Set appliance name
            holder.applianceName.setText(appliance.getString("applianceName"));

            JSONArray prompts = appliance.getJSONArray("appliancePrompts");
            StringBuilder stateText = new StringBuilder();
            StringBuilder rangeText = new StringBuilder();
            StringBuilder modeText = new StringBuilder();

            // Iterate through all prompts
            for (int i = 0; i < prompts.length(); i++) {
                JSONObject prompt = prompts.getJSONObject(i);
                String name = prompt.getString("name");
                String type = prompt.getString("type");
                String value = prompt.getString("value");

                switch (name.toLowerCase()) {
                    case "state":
                        if (stateText.length() > 0) stateText.append("\n");
                        stateText.append(name).append(": ").append(value);
                        break;

                    case "temperature":
                    case "speed":
                        if (rangeText.length() > 0) rangeText.append("\n");
                        rangeText.append(name).append(": ").append(value);
                        if (prompt.has("valid_range")) {
                            JSONArray range = prompt.getJSONArray("valid_range");
                            rangeText.append("\n(").append(range.get(0))
                                    .append("-").append(range.get(1)).append(")");
                        }
                        break;

                    case "mode":
                        if (modeText.length() > 0) modeText.append("\n");
                        modeText.append(name).append(": ").append(value);
                        if (prompt.has("valid_range")) {
                            JSONArray modes = prompt.getJSONArray("valid_range");
                            modeText.append("\n(");
                            for (int j = 0; j < modes.length(); j++) {
                                if (j > 0) modeText.append(", ");
                                modeText.append(modes.getString(j));
                            }
                            modeText.append(")");
                        }
                        break;

                    case "swing":
                    case "has_filter":
                        if (stateText.length() > 0) stateText.append("\n");
                        stateText.append(name).append(": ").append(value);
                        break;

                    default:
                        // For any other boolean type prompts
                        if ("boolean".equals(type)) {
                            if (stateText.length() > 0) stateText.append("\n");
                            stateText.append(name).append(": ").append(value);
                        }
                        break;
                }
            }

            // Set the text views with collected information
            holder.applianceState.setText(stateText.length() > 0 ? stateText.toString() : "N/A");
            holder.applianceRange.setText(rangeText.length() > 0 ? rangeText.toString() : "N/A");
            holder.applianceMode.setText(modeText.length() > 0 ? modeText.toString() : "N/A");

            Log.d("ApplianceAdapter", String.format("Appliance[%d]: %s\nState: %s\nRange: %s\nMode: %s",
                    position,
                    appliance.getString("applianceName"),
                    stateText.toString(),
                    rangeText.toString(),
                    modeText.toString()
            ));

        } catch (JSONException e) {
            Log.e("ApplianceAdapter", "Error binding appliance at position " + position, e);
            holder.applianceName.setText("Error");
            holder.applianceState.setText("N/A");
            holder.applianceRange.setText("N/A");
            holder.applianceMode.setText("N/A");
        }
    }
    @Override
    public int getItemCount() {
        return applianceJsonList.size();
    }

    static class ApplianceJsonViewHolder extends RecyclerView.ViewHolder {
        TextView applianceName, applianceState, applianceRange, applianceMode;

        public ApplianceJsonViewHolder(@NonNull View itemView) {
            super(itemView);
            applianceName = itemView.findViewById(R.id.applianceName);
            applianceState = itemView.findViewById(R.id.applianceState);
            applianceRange = itemView.findViewById(R.id.applianceRange);
            applianceMode = itemView.findViewById(R.id.applianceMode);
        }
    }
}
