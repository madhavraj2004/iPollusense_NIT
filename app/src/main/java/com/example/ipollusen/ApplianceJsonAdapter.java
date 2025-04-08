package com.example.ipollusen;

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
            holder.applianceName.setText(appliance.optString("applianceName", ""));

            JSONObject stateObj = appliance.optJSONObject("applianceState");
            JSONArray prompts = appliance.optJSONArray("appliancePrompts");

            // --- APPLIANCE STATE ---
            String state = "";
            if (stateObj != null) {
                state = stateObj.optString("state", "");
            }
            holder.applianceState.setText(state);

            // --- RANGE & MODE ---
            if (prompts != null && prompts.length() > 0) {
                // Prefer 2nd prompt if available, else use 1st
                JSONObject selectedPrompt = prompts.getJSONObject(Math.min(1, prompts.length() - 1));
                String promptName = selectedPrompt.optString("name");

                JSONObject promptStateObj = stateObj != null ? stateObj.optJSONObject(promptName) : null;

                // 1. RANGE
                String rangeText = "";
                if (promptStateObj != null && promptStateObj.has("lowerLimit") && promptStateObj.has("upperLimit")) {
                    int lower = promptStateObj.optInt("lowerLimit", -1);
                    int upper = promptStateObj.optInt("upperLimit", -1);
                    if (lower != -1 && upper != -1) {
                        rangeText = lower + " - " + upper;
                    }
                }

                if (rangeText.isEmpty()) {
                    JSONArray validRange = selectedPrompt.optJSONArray("valid_range");
                    if (validRange != null && validRange.length() == 2) {
                        int lower = validRange.optInt(0, -1);
                        int upper = validRange.optInt(1, -1);
                        if (lower != -1 && upper != -1) {
                            rangeText = lower + " - " + upper;
                        }
                    }
                }
                holder.applianceRange.setText(rangeText);

                // 2. MODE
                String modeText = "";
                if (promptStateObj != null) {
                    JSONArray modes = promptStateObj.optJSONArray("mode");
                    int selected = promptStateObj.optInt("selected", -1);
                    if (modes != null && selected >= 0 && selected < modes.length()) {
                        modeText = modes.optString(selected, "");
                    }
                }

                if (modeText.isEmpty()) {
                    JSONArray fallbackModes = selectedPrompt.optJSONArray("valid_range");
                    if (fallbackModes != null && fallbackModes.length() > 0) {
                        modeText = fallbackModes.optString(0, "");
                    }
                }

                holder.applianceMode.setText(modeText);
            } else {
                holder.applianceRange.setText("");
                holder.applianceMode.setText("");
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
