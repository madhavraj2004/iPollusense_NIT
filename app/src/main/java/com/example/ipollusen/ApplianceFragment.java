package com.example.ipollusen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApplianceFragment extends Fragment {

    private LinearLayout applianceContainer;
    private Button btnSaveAppliance;
    private SharedViewModel sharedViewModel;
    private OkHttpClient client = new OkHttpClient();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_appliances, container, false);
        applianceContainer = view.findViewById(R.id.applianceContainer);
        btnSaveAppliance = view.findViewById(R.id.btnSaveAppliance);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        btnSaveAppliance.setOnClickListener(v -> saveApplianceData());
        fetchAppliances();

        return view;
    }

    private void fetchAppliances() {
        String url = "http://52.250.54.24:3500/api/appliances/";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Error fetching appliances", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        requireActivity().runOnUiThread(() -> populateUI(jsonArray));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void populateUI(JSONArray appliances) {
        applianceContainer.removeAllViews();

        for (int i = 0; i < appliances.length(); i++) {
            try {
                JSONObject appliance = appliances.getJSONObject(i);
                String applianceName = appliance.getString("applianceName");
                JSONArray prompts = appliance.getJSONArray("appliancePrompts");

                CardView cardView = new CardView(getContext());
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(20, 20, 20, 20);
                cardView.setLayoutParams(cardParams);
                cardView.setCardElevation(8);
                cardView.setRadius(20);
                cardView.setUseCompatPadding(true);
                cardView.setTag(appliance);

                LinearLayout applianceLayout = new LinearLayout(getContext());
                applianceLayout.setOrientation(LinearLayout.VERTICAL);
                applianceLayout.setPadding(30, 30, 30, 30);

                LinearLayout topRow = new LinearLayout(getContext());
                topRow.setOrientation(LinearLayout.HORIZONTAL);

                CheckBox mainSwitch = new CheckBox(getContext());
                mainSwitch.setTag("mainSwitch");

                TextView title = new TextView(getContext());
                title.setText(applianceName);
                title.setTextSize(18);
                title.setPadding(20, 0, 0, 0);

                topRow.addView(mainSwitch);
                topRow.addView(title);
                applianceLayout.addView(topRow);

                View divider = new View(getContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
                divider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                applianceLayout.addView(divider);

                for (int j = 0; j < prompts.length(); j++) {
                    JSONObject prompt = prompts.getJSONObject(j);
                    String promptName = prompt.getString("name");
                    String description = prompt.optString("description", "");
                    String type = prompt.optString("type", "").toLowerCase();

                    switch (promptName.toLowerCase()) {
                        case "state":
                            TextView stateLabel = new TextView(getContext());
                            stateLabel.setText("State - " + description);
                            stateLabel.setPadding(0, 20, 0, 10);
                            applianceLayout.addView(stateLabel);

                            Spinner stateSpinner = new Spinner(getContext());
                            ArrayAdapter<String> stateAdapter = new ArrayAdapter<>(getContext(),
                                    android.R.layout.simple_spinner_item, new String[]{"ON", "OFF"});
                            stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            stateSpinner.setAdapter(stateAdapter);
                            stateSpinner.setTag("state_spinner");
                            applianceLayout.addView(stateSpinner);
                            break;

                        case "range":
                            TextView rangeLabel = new TextView(getContext());
                            rangeLabel.setText("Range - " + description);
                            rangeLabel.setPadding(0, 20, 0, 10);
                            applianceLayout.addView(rangeLabel);

                            TextView lowerText = new TextView(getContext());
                            lowerText.setText("Lower Limit:");
                            applianceLayout.addView(lowerText);

                            SeekBar lowerSeek = new SeekBar(getContext());
                            lowerSeek.setMax(100);
                            lowerSeek.setProgress(20);
                            lowerSeek.setTag("lowerSeek_" + promptName);
                            applianceLayout.addView(lowerSeek);

                            TextView upperText = new TextView(getContext());
                            upperText.setText("Upper Limit:");
                            applianceLayout.addView(upperText);

                            SeekBar upperSeek = new SeekBar(getContext());
                            upperSeek.setMax(100);
                            upperSeek.setProgress(80);
                            upperSeek.setTag("upperSeek_" + promptName);
                            applianceLayout.addView(upperSeek);
                            break;

                        default:
                            if (type.equals("number")) {
                                TextView numberLabel = new TextView(getContext());
                                numberLabel.setText(promptName + " - " + description);
                                numberLabel.setPadding(0, 20, 0, 10);
                                applianceLayout.addView(numberLabel);

                                SeekBar numberSeek = new SeekBar(getContext());
                                numberSeek.setMax(100);
                                numberSeek.setProgress(50);
                                numberSeek.setTag("numberSeek_" + promptName);

                                TextView numberValue = new TextView(getContext());
                                numberValue.setText("Value: 50");
                                numberValue.setPadding(0, 10, 0, 20);
                                numberSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                    @Override
                                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                        numberValue.setText("Value: " + progress);
                                    }

                                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                                });

                                applianceLayout.addView(numberSeek);
                                applianceLayout.addView(numberValue);
                            } else if (prompt.has("valid_range")) {
                                TextView modeLabel = new TextView(getContext());
                                modeLabel.setText(promptName + " - " + description);
                                modeLabel.setPadding(0, 20, 0, 10);
                                applianceLayout.addView(modeLabel);

                                Spinner modeSpinner = new Spinner(getContext());
                                JSONArray options = prompt.getJSONArray("valid_range");
                                List<String> optionList = new ArrayList<>();
                                for (int k = 0; k < options.length(); k++) {
                                    optionList.add(options.getString(k));
                                }

                                ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(getContext(),
                                        android.R.layout.simple_spinner_item, optionList);
                                modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                modeSpinner.setAdapter(modeAdapter);
                                modeSpinner.setTag("modeSpinner_" + promptName);
                                applianceLayout.addView(modeSpinner);
                            }
                            break;
                    }
                }

                cardView.addView(applianceLayout);
                applianceContainer.addView(cardView);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Button saveButton = new Button(getContext());
        saveButton.setText("Save Appliances");
        saveButton.setTextSize(16);
        saveButton.setBackgroundColor(getResources().getColor(R.color.teal_700));
        saveButton.setTextColor(getResources().getColor(android.R.color.white));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        saveParams.setMargins(40, 60, 40, 100);
        saveButton.setLayoutParams(saveParams);
        saveButton.setOnClickListener(v -> saveApplianceData());

        applianceContainer.addView(saveButton);
    }

    private void saveApplianceData() {
        JSONArray appliancesArray = new JSONArray();

        int totalChildren = applianceContainer.getChildCount();
        for (int i = 0; i < totalChildren - 1; i++) {
            View cardView = applianceContainer.getChildAt(i);
            if (cardView instanceof CardView) {
                try {
                    JSONObject originalAppliance = (JSONObject) cardView.getTag();
                    LinearLayout innerLayout = (LinearLayout) ((CardView) cardView).getChildAt(0);

                    CheckBox mainSwitch = innerLayout.findViewWithTag("mainSwitch");
                    if (mainSwitch != null && !mainSwitch.isChecked()) continue;

                    JSONObject applianceObject = new JSONObject();
                    applianceObject.put("_id", originalAppliance.optString("_id", ""));
                    applianceObject.put("applianceName", originalAppliance.getString("applianceName"));

                    JSONArray applianceStatesArray = new JSONArray();
                    JSONArray prompts = originalAppliance.getJSONArray("appliancePrompts");

                    for (int p = 0; p < prompts.length(); p++) {
                        JSONObject prompt = prompts.getJSONObject(p);
                        String promptName = prompt.getString("name");
                        String type = prompt.optString("type", "String");

                        JSONObject state = new JSONObject();
                        state.put("name", promptName);
                        state.put("type", type);
                        state.put("description", prompt.optString("description", ""));

                        switch (promptName.toLowerCase()) {
                            case "state":
                                Spinner stateSpinner = innerLayout.findViewWithTag("state_spinner");
                                if (stateSpinner != null)
                                    state.put("value", stateSpinner.getSelectedItem().toString());
                                break;

                            case "range":
                                SeekBar lowerSeek = innerLayout.findViewWithTag("lowerSeek_" + promptName);
                                SeekBar upperSeek = innerLayout.findViewWithTag("upperSeek_" + promptName);
                                if (lowerSeek != null && upperSeek != null) {
                                    JSONArray rangeArray = new JSONArray();
                                    rangeArray.put(lowerSeek.getProgress());
                                    rangeArray.put(upperSeek.getProgress());
                                    state.put("valid_range", rangeArray);
                                    state.put("value", upperSeek.getProgress());
                                }
                                break;

                            default:
                                if (type.equals("number")) {
                                    SeekBar numberSeek = innerLayout.findViewWithTag("numberSeek_" + promptName);
                                    if (numberSeek != null) {
                                        state.put("value", numberSeek.getProgress());
                                    }
                                } else {
                                    Spinner modeSpinner = innerLayout.findViewWithTag("modeSpinner_" + promptName);
                                    if (modeSpinner != null) {
                                        state.put("value", modeSpinner.getSelectedItem().toString());
                                        if (prompt.has("valid_range")) {
                                            state.put("valid_range", prompt.getJSONArray("valid_range"));
                                        }
                                    }
                                }
                                break;
                        }

                        applianceStatesArray.put(state);
                    }

                    applianceObject.put("appliancePrompts", applianceStatesArray);
                    appliancesArray.put(applianceObject);

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Error gathering appliance data", Toast.LENGTH_SHORT).show();
                }
            }
        }

        JSONObject finalJson = new JSONObject();
        try {
            finalJson.put("appliances", appliancesArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Bundle result = new Bundle();
        result.putString("appliance_data", finalJson.toString());
        getParentFragmentManager().setFragmentResult("appliance_data_result", result);

        Toast.makeText(getContext(), "Appliance data saved", Toast.LENGTH_SHORT).show();
        Navigation.findNavController(requireView()).navigateUp();
    }
}
