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

        // Fetch appliances from the API and build the UI
        fetchAppliances();

        // Static Save button listener: generate JSON, pass result and navigate back.
        btnSaveAppliance.setOnClickListener(v -> {
            JSONArray finalAppliancesArray = generateFinalApplianceJSON();
            if (finalAppliancesArray != null && finalAppliancesArray.length() > 0) {
                try {
                    JSONObject applianceWrapper = new JSONObject();
                    applianceWrapper.put("appliances", finalAppliancesArray);

                    Bundle result = new Bundle();
                    result.putString("appliance_data", applianceWrapper.toString());

                    // Share the result with the receiving fragment and navigate back
                    getParentFragmentManager().setFragmentResult("appliance_data_result", result);
                    Navigation.findNavController(v).navigate(R.id.action_navigation_appliance_to_roomDetailsFragment);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(requireContext(), "Error formatting appliance data", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "Please enable at least one appliance to proceed.", Toast.LENGTH_SHORT).show();
            }
        });

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

        // Loop through each appliance and create UI card.
        for (int i = 0; i < appliances.length(); i++) {
            try {
                JSONObject appliance = appliances.getJSONObject(i);
                String applianceName = appliance.getString("applianceName");
                JSONArray prompts = appliance.getJSONArray("appliancePrompts");

                // Create CardView container.
                CardView cardView = new CardView(getContext());
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(20, 20, 20, 20);
                cardView.setLayoutParams(cardParams);
                cardView.setCardElevation(8);
                cardView.setRadius(20);
                cardView.setUseCompatPadding(true);
                // Save the original appliance JSON in the card's tag.
                cardView.setTag(appliance);

                // Inner layout for appliance details.
                LinearLayout applianceLayout = new LinearLayout(getContext());
                applianceLayout.setOrientation(LinearLayout.VERTICAL);
                applianceLayout.setPadding(30, 30, 30, 30);

                // Top row with a main switch and title.
                LinearLayout topRow = new LinearLayout(getContext());
                topRow.setOrientation(LinearLayout.HORIZONTAL);

                CheckBox mainSwitch = new CheckBox(getContext());
                mainSwitch.setTag("mainSwitch"); // determines if the appliance is enabled.

                TextView title = new TextView(getContext());
                title.setText(applianceName);
                title.setTextSize(18);
                title.setPadding(20, 0, 0, 0);

                topRow.addView(mainSwitch);
                topRow.addView(title);
                applianceLayout.addView(topRow);

                // Divider.
                View divider = new View(getContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
                divider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                applianceLayout.addView(divider);

                // Loop through each prompt and build appropriate input UI.
                for (int j = 0; j < prompts.length(); j++) {
                    JSONObject prompt = prompts.getJSONObject(j);
                    String promptName = prompt.getString("name");
                    String description = prompt.optString("description", "");
                    String type = prompt.optString("type", "").toLowerCase();

                    TextView promptLabel = new TextView(getContext());
                    promptLabel.setPadding(0, 20, 0, 10);

                    // Handle "State" specially for boolean type using a Spinner.
                    if (promptName.equalsIgnoreCase("State")) {
                        promptLabel.setText("State - " + description);

                        Spinner stateSpinner = new Spinner(getContext());
                        ArrayAdapter<String> stateAdapter = new ArrayAdapter<>(getContext(),
                                android.R.layout.simple_spinner_item, new String[]{"ON", "OFF"});
                        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        stateSpinner.setAdapter(stateAdapter);
                        // Tag set to the prompt name for later retrieval.
                        stateSpinner.setTag(promptName);

                        applianceLayout.addView(promptLabel);
                        applianceLayout.addView(stateSpinner);
                    }
                    // For number-type fields: if a valid_range array exists, create two input fields for min and max.
                    else if (type.equals("number")) {
                        JSONArray range = prompt.optJSONArray("valid_range");
                        if (range != null && range.length() == 2) {
                            String minValue = String.valueOf(range.optInt(0));
                            String maxValue = String.valueOf(range.optInt(1));
                            promptLabel.setText(promptName + " - " + description + " (" + minValue + " - " + maxValue + ")");

                            LinearLayout numberLayout = new LinearLayout(getContext());
                            numberLayout.setOrientation(LinearLayout.HORIZONTAL);

                            // EditText for lower value.
                            EditText minEditText = new EditText(getContext());
                            minEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                            minEditText.setHint("Min " + promptName);
                            minEditText.setLayoutParams(new LinearLayout.LayoutParams(
                                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                            minEditText.setTag(promptName + "_min");

                            // EditText for upper value.
                            EditText maxEditText = new EditText(getContext());
                            maxEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                            maxEditText.setHint("Max " + promptName);
                            maxEditText.setLayoutParams(new LinearLayout.LayoutParams(
                                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                            maxEditText.setTag(promptName + "_max");

                            numberLayout.addView(minEditText);
                            numberLayout.addView(maxEditText);

                            applianceLayout.addView(promptLabel);
                            applianceLayout.addView(numberLayout);
                        } else {
                            // Fallback: single input if no range is provided.
                            promptLabel.setText(promptName + " - " + description);
                            EditText inputField = new EditText(getContext());
                            inputField.setInputType(InputType.TYPE_CLASS_NUMBER);
                            inputField.setHint("Enter " + promptName);
                            inputField.setTag(promptName);
                            applianceLayout.addView(promptLabel);
                            applianceLayout.addView(inputField);
                        }
                    }
                    // For list type inputs.
                    else if (type.equals("list")) {
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
                        optionsSpinner.setTag(promptName);

                        applianceLayout.addView(promptLabel);
                        applianceLayout.addView(optionsSpinner);
                    }
                    // For boolean types (other than "State" which we handled above).
                    else if (type.equals("boolean")) {
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

        // (Optional) Dynamically add a Save button at the bottom. (You can remove this if you have a static one.)
        Button saveButton = new Button(getContext());
        saveButton.setText("Save Appliances");
        saveButton.setTextSize(16);
        saveButton.setBackgroundColor(getResources().getColor(R.color.teal_700));
        saveButton.setTextColor(getResources().getColor(android.R.color.white));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        saveParams.setMargins(40, 60, 40, 100);
        saveButton.setLayoutParams(saveParams);
        saveButton.setOnClickListener(v -> {
            saveApplianceData();
            // After saving, share the result and navigate back.
            JSONArray finalArray = generateFinalApplianceJSON();
            if (finalArray != null && finalArray.length() > 0) {
                try {
                    JSONObject applianceWrapper = new JSONObject();
                    applianceWrapper.put("appliances", finalArray);

                    Bundle result = new Bundle();
                    result.putString("appliance_data", applianceWrapper.toString());
                    getParentFragmentManager().setFragmentResult("appliance_data_result", result);

                    Navigation.findNavController(v).navigate(R.id.action_navigation_appliance_to_roomDetailsFragment);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        applianceContainer.addView(saveButton);
    }

    /**
     * This method loops through each enabled appliance (where mainSwitch is checked)
     * and updates each prompt with its UI value.
     */
    private JSONArray generateFinalApplianceJSON() {
        JSONArray finalArray = new JSONArray();

        // Loop through all direct children (CardViews) of applianceContainer.
        for (int i = 0; i < applianceContainer.getChildCount(); i++) {
            View child = applianceContainer.getChildAt(i);
            if (!(child instanceof CardView)) continue;  // Skip non-card views.
            CardView card = (CardView) child;
            JSONObject appliance = (JSONObject) card.getTag();

            // Check if this appliance is enabled.
            CheckBox mainSwitch = card.findViewWithTag("mainSwitch");
            if (mainSwitch != null && mainSwitch.isChecked()) {
                try {
                    // Create a copy of the original appliance JSON to update its prompts.
                    JSONObject applianceCopy = new JSONObject(appliance.toString());
                    JSONArray prompts = applianceCopy.getJSONArray("appliancePrompts");

                    for (int j = 0; j < prompts.length(); j++) {
                        JSONObject prompt = prompts.getJSONObject(j);
                        String promptName = prompt.getString("name");
                        String promptType = prompt.getString("type").toLowerCase();

                        String value = "";
                        // For boolean: check if UI input is a Switch or Spinner.
                        if (promptType.equals("boolean")) {
                            View inputView = card.findViewWithTag(promptName);
                            if (inputView instanceof Switch) {
                                value = ((Switch) inputView).isChecked() ? "true" : "false";
                            } else if (inputView instanceof Spinner) {
                                // Convert spinner selection "ON"/"OFF" to "true"/"false"
                                String selected = ((Spinner) inputView).getSelectedItem().toString();
                                value = selected.equalsIgnoreCase("ON") ? "true" : "false";
                            }
                        }
                        // For number type, support range input.
                        else if (promptType.equals("number")) {
                            View minView = card.findViewWithTag(promptName + "_min");
                            View maxView = card.findViewWithTag(promptName + "_max");
                            if (minView instanceof EditText && maxView instanceof EditText) {
                                String minVal = ((EditText) minView).getText().toString().trim();
                                String maxVal = ((EditText) maxView).getText().toString().trim();
                                if (!minVal.isEmpty() && !maxVal.isEmpty()) {
                                    value = minVal + "-" + maxVal;
                                }
                            } else {
                                // Fallback for single input scenario.
                                View inputView = card.findViewWithTag(promptName);
                                if (inputView instanceof EditText) {
                                    value = ((EditText) inputView).getText().toString();
                                }
                            }
                        }
                        // For list type.
                        else if (promptType.equals("list")) {
                            View inputView = card.findViewWithTag(promptName);
                            if (inputView instanceof Spinner) {
                                // For "State" in some cases, convert "ON"/"OFF" if needed.
                                String selected = ((Spinner) inputView).getSelectedItem().toString();
                                if (promptName.equalsIgnoreCase("State")) {
                                    value = selected.equalsIgnoreCase("ON") ? "true" : "false";
                                } else {
                                    value = selected;
                                }
                            }
                        }
                        // For other types (fallback).
                        else {
                            View inputView = card.findViewWithTag(promptName);
                            if (inputView instanceof EditText) {
                                value = ((EditText) inputView).getText().toString();
                            }
                        }
                        prompt.put("value", value);
                    }
                    finalArray.put(applianceCopy);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.d("FinalApplianceJSON", finalArray.toString());
        return finalArray;
    }

    /**
     * Alternative method to create JSON from UI inputs.
     * This is similar to generateFinalApplianceJSON().
     */
    private void saveApplianceData() {
        JSONArray appliancesArray = new JSONArray();

        int totalChildren = applianceContainer.getChildCount();
        for (int i = 0; i < totalChildren; i++) {
            View view = applianceContainer.getChildAt(i);
            if (!(view instanceof CardView)) continue;
            CardView cardView = (CardView) view;
            JSONObject originalAppliance = (JSONObject) cardView.getTag();
            LinearLayout innerLayout = (LinearLayout) cardView.getChildAt(0);

            CheckBox mainSwitch = innerLayout.findViewWithTag("mainSwitch");
            if (mainSwitch == null || !mainSwitch.isChecked()) continue;

            try {
                JSONObject applianceObject = new JSONObject();
                applianceObject.put("_id", originalAppliance.optString("_id", ""));
                applianceObject.put("applianceName", originalAppliance.getString("applianceName"));

                JSONArray appliancePromptsArray = new JSONArray();
                JSONArray prompts = originalAppliance.getJSONArray("appliancePrompts");

                for (int p = 0; p < prompts.length(); p++) {
                    JSONObject prompt = prompts.getJSONObject(p);
                    String promptName = prompt.getString("name");
                    String type = prompt.optString("type", "string").toLowerCase();

                    JSONObject state = new JSONObject();
                    state.put("name", promptName);
                    state.put("description", prompt.optString("description", ""));
                    state.put("type", type);

                    if (prompt.has("valid_range")) {
                        state.put("valid_range", prompt.getJSONArray("valid_range"));
                    }

                    String value = "";
                    if (type.equals("boolean")) {
                        View inputView = innerLayout.findViewWithTag(promptName);
                        if (inputView instanceof Switch) {
                            value = ((Switch) inputView).isChecked() ? "true" : "false";
                        } else if (inputView instanceof Spinner) {
                            String selected = ((Spinner) inputView).getSelectedItem().toString();
                            value = selected.equalsIgnoreCase("ON") ? "true" : "false";
                        }
                    } else if (type.equals("number")) {
                        View minView = innerLayout.findViewWithTag(promptName + "_min");
                        View maxView = innerLayout.findViewWithTag(promptName + "_max");
                        if (minView instanceof EditText && maxView instanceof EditText) {
                            String minVal = ((EditText) minView).getText().toString().trim();
                            String maxVal = ((EditText) maxView).getText().toString().trim();
                            if (!minVal.isEmpty() && !maxVal.isEmpty()) {
                                value = minVal + "-" + maxVal;
                            }
                        } else {
                            View inputView = innerLayout.findViewWithTag(promptName);
                            if (inputView instanceof EditText) {
                                value = ((EditText) inputView).getText().toString();
                            }
                        }
                    } else if (type.equals("list")) {
                        View inputView = innerLayout.findViewWithTag(promptName);
                        if (inputView instanceof Spinner) {
                            String selected = ((Spinner) inputView).getSelectedItem().toString();
                            if (promptName.equalsIgnoreCase("State")) {
                                value = selected.equalsIgnoreCase("ON") ? "true" : "false";
                            } else {
                                value = selected;
                            }
                        }
                    } else {
                        View inputView = innerLayout.findViewWithTag(promptName);
                        if (inputView instanceof EditText) {
                            value = ((EditText) inputView).getText().toString().trim();
                        }
                    }
                    state.put("value", value);
                    appliancePromptsArray.put(state);
                }
                applianceObject.put("appliancePrompts", appliancePromptsArray);
                appliancesArray.put(applianceObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.d("FinalJSON", appliancesArray.toString());
    }
}
