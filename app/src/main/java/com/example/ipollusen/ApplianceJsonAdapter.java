package com.example.ipollusen;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ipollusen.R;

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
            holder.applianceName.setText(appliance.optString("applianceName", "N/A"));

            JSONObject stateObj = appliance.optJSONObject("applianceState");
            if (stateObj != null) {
                // 1. Appliance ON/OFF state
                String state = stateObj.optString("state", "N/A");
                holder.applianceState.setText(state);

                // 2. Get second prompt name
                JSONArray prompts = appliance.optJSONArray("appliancePrompts");
                if (prompts != null && prompts.length() > 1) {
                    String secondPromptName = prompts.getJSONObject(1).optString("name", null);
                    if (secondPromptName != null) {
                        JSONObject promptObj = stateObj.optJSONObject(secondPromptName);
                        if (promptObj != null) {
                            // Get lower and upper limits
                            int lower = promptObj.optInt("lowerLimit", -1);
                            int upper = promptObj.optInt("upperLimit", -1);
                            holder.applianceRange.setText(lower + " - " + upper);

                            // Get selected mode from mode array
                            JSONArray modes = promptObj.optJSONArray("mode");
                            int selected = promptObj.optInt("selected", -1);
                            if (modes != null && selected >= 0 && selected < modes.length()) {
                                String selectedMode = modes.optString(selected, "N/A");
                                holder.applianceMode.setText(selectedMode);
                            } else {
                                holder.applianceMode.setText("N/A");
                            }
                        } else {
                            holder.applianceRange.setText("N/A");
                            holder.applianceMode.setText("N/A");
                        }
                    }
                } else {
                    holder.applianceRange.setText("N/A");
                    holder.applianceMode.setText("N/A");
                }
            } else {
                holder.applianceState.setText("N/A");
                holder.applianceRange.setText("N/A");
                holder.applianceMode.setText("N/A");
            }
        } catch (JSONException e) {
            e.printStackTrace();
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
