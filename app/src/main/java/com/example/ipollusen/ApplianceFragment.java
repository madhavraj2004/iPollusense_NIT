package com.example.ipollusen;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
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

                    TextView promptLabel = new TextView(getContext());
                    promptLabel.setPadding(0, 20, 0, 10);

                    if (promptName.equalsIgnoreCase("State")) {
                        promptLabel.setText("State - " + description);

                        Spinner stateSpinner = new Spinner(getContext());
                        ArrayAdapter<String> stateAdapter = new ArrayAdapter<>(getContext(),
                                android.R.layout.simple_spinner_item, new String[]{"ON", "OFF"});
                        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        stateSpinner.setAdapter(stateAdapter);

                        applianceLayout.addView(promptLabel);
                        applianceLayout.addView(stateSpinner);

                    } else if (type.equals("number")) {
                        // For number type with range
                        JSONArray range = prompt.optJSONArray("valid_range");
                        String min = range != null ? String.valueOf(range.optInt(0)) : "N/A";
                        String max = range != null ? String.valueOf(range.optInt(1)) : "N/A";

                        promptLabel.setText(promptName + " - " + description + " (" + min + " - " + max + ")");

                        EditText inputField = new EditText(getContext());
                        inputField.setInputType(InputType.TYPE_CLASS_NUMBER);
                        inputField.setHint("Enter " + promptName);
                        inputField.setTag(promptName);

                        applianceLayout.addView(promptLabel);
                        applianceLayout.addView(inputField);

                    } else if (type.equals("list")) {
                        JSONArray validOptions = prompt.optJSONArray("valid_range");
                        List<String> options = new ArrayList<>();
                        if (validOptions != null) {
                            for (int k = 0; k < validOptions.length(); k++) {
                                options.add(validOptions.getString(k));
                            }
                        }

                        promptLabel.setText(promptName + " - " + description);

                        Spinner optionsSpinner = new Spinner(getContext());
                        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(getContext(),
                                android.R.layout.simple_spinner_item, options);
                        listAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        optionsSpinner.setAdapter(listAdapter);

                        applianceLayout.addView(promptLabel);
                        applianceLayout.addView(optionsSpinner);

                    } else if (type.equals("boolean")) {
                        promptLabel.setText(promptName + " - " + description);

                        Switch switchView = new Switch(getContext());
                        switchView.setTag(promptName);

                        applianceLayout.addView(promptLabel);
                        applianceLayout.addView(switchView);
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
        // Exclude the last child (Save Button)
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

                    JSONArray appliancePromptsArray = new JSONArray();
                    JSONArray prompts = originalAppliance.getJSONArray("appliancePrompts");

                    for (int p = 0; p < prompts.length(); p++) {
                        JSONObject prompt = prompts.getJSONObject(p);
                        String promptName = prompt.getString("name");
                        String type = prompt.optString("type", "String");

                        JSONObject state = new JSONObject();
                        state.put("name", promptName);
                        state.put("description", prompt.optString("description", ""));
                        state.put("type", type);

                        // Copy valid_range if exists
                        if (prompt.has("valid_range")) {
                            state.put("valid_range", prompt.getJSONArray("valid_range"));
                        }

                        // Get value from UI based on type/prompt
                        switch (promptName.toLowerCase()) {
                            case "state":
                                Spinner stateSpinner = innerLayout.findViewWithTag("state_spinner");
                                if (stateSpinner != null)
                                    state.put("value", stateSpinner.getSelectedItem().toString());
                                break;

                            case "range":
                                EditText lowerLimit = innerLayout.findViewWithTag("lowerLimit_" + promptName);
                                EditText upperLimit = innerLayout.findViewWithTag("upperLimit_" + promptName);
                                if (lowerLimit != null && upperLimit != null) {
                                    String lowerText = lowerLimit.getText().toString().trim();
                                    String upperText = upperLimit.getText().toString().trim();
                                    state.put("value", lowerText + "-" + upperText);
                                }
                                break;

                            default:
                                if (type.equals("number")) {
                                    SeekBar numberSeek = innerLayout.findViewWithTag("seek_" + promptName);
                                    if (numberSeek != null)
                                        state.put("value", String.valueOf(numberSeek.getProgress()));
                                } else if (type.equals("boolean")) {
                                    CheckBox checkBox = innerLayout.findViewWithTag("check_" + promptName);
                                    if (checkBox != null)
                                        state.put("value", checkBox.isChecked() ? "true" : "");
                                } else if (type.equals("list")) {
                                    Spinner spinner = innerLayout.findViewWithTag("spinner_" + promptName);
                                    if (spinner != null)
                                        state.put("value", spinner.getSelectedItem().toString());
                                } else {
                                    EditText editText = innerLayout.findViewWithTag("edit_" + promptName);
                                    if (editText != null)
                                        state.put("value", editText.getText().toString().trim());
                                }
                                break;
                        }

                        appliancePromptsArray.put(state);
                    }

                    applianceObject.put("appliancePrompts", appliancePromptsArray);
                    appliancesArray.put(applianceObject);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        Log.d("FinalJSON", appliancesArray.toString());
    }

}
