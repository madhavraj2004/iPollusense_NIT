package com.example.ipollusen;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Button;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FeedbackFragment extends Fragment {

    private static final String TAG = "FeedbackFragment";
    private RoomRecommendAdapter roomrecommendAdapter;
    private LinearLayout applianceStateContainer;
    private TextView rationaleText;
    private TextView otherSuggestionsText;
    private Button sendRecommendationButton;
    private Handler handler;
    private String mapRoomUserId;
    private SharedResponseViewModel sharedViewModel;
    private JSONObject originalJson; // To hold the original JSON for feedback processing
    private ProgressBar progressBar;
    private TextView noDataTextView;

    public static FeedbackFragment newInstance(String mapRoomUserId) {
        FeedbackFragment fragment = new FeedbackFragment();
        Bundle args = new Bundle();
        args.putString("mapRoomUserId", mapRoomUserId);
        fragment.setArguments(args);
        return fragment;
    }
    public FeedbackFragment() {
        // Required empty public constructor.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper());
        // Initialize SharedResponseViewModel first
        try {
            sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedResponseViewModel.class);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing SharedResponseViewModel: " + e.getMessage());
            navigateBack();
            return;
        }

        // Get mapRoomUserId from arguments
        if (getArguments() != null) {
            mapRoomUserId = getArguments().getString("mapRoomUserId");
            Log.d(TAG, "Received mapRoomUserId: " + mapRoomUserId);

            if (mapRoomUserId == null || mapRoomUserId.isEmpty()) {
                Log.e(TAG, "mapRoomUserId is null or empty");
                navigateBack();
                return;
            }
        } else {
            Log.e(TAG, "No arguments received in FeedbackFragment");
            navigateBack();
            return;
        }

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedResponseViewModel.class);

        // Only start fetching if we have a valid mapRoomUserId
        if (mapRoomUserId != null && !mapRoomUserId.isEmpty()) {
            fetchPromptResponseInterval(mapRoomUserId);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feedback, container, false);

        // Initialize UI elements
        progressBar = view.findViewById(R.id.progressBar);
        noDataTextView = view.findViewById(R.id.noDataTextView);
        applianceStateContainer = view.findViewById(R.id.applianceStateContainer);
        rationaleText = view.findViewById(R.id.rationaleText);
        otherSuggestionsText = view.findViewById(R.id.otherSuggestionsText);
        sendRecommendationButton = view.findViewById(R.id.sendRecommendationButton);

        observeViewModel();

        // Handle the "Send Recommendation" button click
        sendRecommendationButton.setOnClickListener(v -> {
            try {
                JSONObject feedbackJson = constructFeedbackJson();
                logLongString(TAG, "Constructed Feedback JSON: " + feedbackJson.toString());
                sendFeedbackToServer(feedbackJson);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to construct feedback JSON: " + e.getMessage());
            }
        });
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        NavController navController = Navigation.findNavController(requireView());
                        if (navController.getCurrentDestination() != null &&
                                navController.getCurrentDestination().getId() == R.id.feedbackFragment) {
                            // Navigate back
                            navController.popBackStack();
                        } else {
                            Log.e(TAG, "Current destination is not FeedbackFragment. Back action ignored.");
                        }
                    }
                });
        if (sharedViewModel != null && mapRoomUserId != null && !mapRoomUserId.isEmpty()) {
            observeViewModel();
            fetchPromptResponseInterval(mapRoomUserId);
        } else {
            showError("Error: Unable to load data");
            navigateBack();
        }

        return view;
    }
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);

        noDataTextView.setVisibility(View.GONE);
    }



    private void showNoData() {
        progressBar.setVisibility(View.GONE);

        noDataTextView.setVisibility(View.VISIBLE);
    }
    private void logLongString(String tag, String message) {
        int maxLogSize = 1000;
        for (int i = 0; i <= message.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = Math.min((i + 1) * maxLogSize, message.length());
            Log.d(tag, message.substring(start, end));  // Use Log.d(), not logLongString()
        }
    }

    private void observeViewModel() {
        if (sharedViewModel == null) {
            Log.e(TAG, "SharedResponseViewModel is null");
            showError("Error: Unable to load data");
            navigateBack();
            return;
        }

        showLoading();
        try {
            sharedViewModel.getResponseMapLiveData().observe(getViewLifecycleOwner(), responseMap -> {
                if (responseMap != null && responseMap.containsKey(mapRoomUserId)) {
                    try {
                        String jsonString = responseMap.get(mapRoomUserId);
                        logLongString(TAG, "JSON Response: " + jsonString);
                        originalJson = new JSONObject(jsonString);

                        if (originalJson.has("histItem") && originalJson.getJSONObject("histItem").has("room")) {
                            JSONObject roomObj = originalJson.getJSONObject("histItem").getJSONObject("room");
                            logLongString(TAG, "Room data found: " + roomObj.toString());
                            handleRoomData(originalJson, generateApplianceIdToNameMap(roomObj));
                        } else {
                            Log.e(TAG, "No 'room' key found in the 'histItem'.");
                            showError("Room data is missing. Unable to display appliance details.");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        showError("Failed to parse response. Please try again.");
                    }
                } else {
                    Log.e(TAG, "Response map is null or does not contain the required key: " + mapRoomUserId);
                    showNoData();
                    showError("No data available for this room.");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up ViewModel observer: " + e.getMessage());
            showError("Error: Unable to load data");
            navigateBack();
        }
    }
    private void navigateBack() {
        if (isAdded() && getView() != null) {
            try {
                NavController navController = Navigation.findNavController(requireView());
                navController.popBackStack();
            } catch (Exception e) {
                Log.e(TAG, "Error navigating back: " + e.getMessage());
            }
        }
    }
    private void handleRoomData(JSONObject jsonResponse, Map<String, String> applianceIdToNameMap) {
        try {
            JSONObject responseObj = jsonResponse.getJSONObject("response");
            JSONObject histItemObj = jsonResponse.getJSONObject("histItem");

            populateUI(responseObj, histItemObj, applianceIdToNameMap);

            String rationale = responseObj.optString("rationale", "No rationale provided");
            String otherSuggestions = responseObj.optString("other_suggestions", "No suggestions provided");

            rationaleText.setText("Rationale: " + rationale);
            otherSuggestionsText.setText("Other Suggestions: " + otherSuggestions);
        } catch (JSONException e) {
            Log.e(TAG, "Error while handling room data: " + e.getMessage());
        }
    }

    private Map<String, String> generateApplianceIdToNameMap(JSONObject roomObj) {
        // Generate a map of appliance IDs to their names
        Map<String, String> applianceIdToNameMap = new HashMap<>();
        try {
            JSONArray appliancesArray = roomObj.getJSONArray("appliances");
            for (int i = 0; i < appliancesArray.length(); i++) {
                JSONObject applianceObj = appliancesArray.getJSONObject(i);
                String applianceId = applianceObj.getString("_id");
                String applianceName = applianceObj.getString("applianceName");
                applianceIdToNameMap.put(applianceId, applianceName);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error generating appliance ID to name map: " + e.getMessage());
        }
        return applianceIdToNameMap;
    }

    private void populateUI(JSONObject responseObj, JSONObject histItemObj, Map<String, String> applianceIdToNameMap) {
        applianceStateContainer.removeAllViews();

        try {
            JSONArray applianceStates = responseObj.getJSONArray("appliance_states");
            JSONObject applianceIdMap = histItemObj.getJSONObject("appliance_id_map");

            for (int i = 0; i < applianceStates.length(); i++) {
                JSONObject applianceStateObj = applianceStates.getJSONObject(i);
                Iterator<String> it = applianceStateObj.keys();
                if (!it.hasNext()) continue;

                String applianceKey = it.next();
                JSONObject applianceDetails = applianceStateObj.getJSONObject(applianceKey);

                String numericKey = applianceKey.replaceAll("[^0-9]", "");
                String applianceId = applianceIdMap.optString(numericKey, "");
                String friendlyName = applianceIdToNameMap.getOrDefault(applianceId, "Unknown Appliance");

                CardView cardView = createApplianceCard(getContext(), applianceDetails, friendlyName, applianceKey);
                applianceStateContainer.addView(cardView);
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error in populateUI: " + e.getMessage());
        }
    }

    private CardView createApplianceCard(Context context, JSONObject applianceDetails, String friendlyName, String applianceKey) {
        CardView cardView = new CardView(context);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(20, 20, 20, 20);
        cardView.setLayoutParams(cardParams);
        cardView.setCardElevation(8);
        cardView.setRadius(20);

        LinearLayout applianceLayout = new LinearLayout(context);
        applianceLayout.setOrientation(LinearLayout.VERTICAL);
        applianceLayout.setPadding(30, 30, 30, 30);

        TextView title = new TextView(context);
        title.setText(friendlyName);
        title.setTextSize(18);
        applianceLayout.addView(title);

        try {
            Iterator<String> keys = applianceDetails.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = applianceDetails.getString(key);
                TextView detailText = new TextView(context);
                detailText.setText(key + ": " + value);
                applianceLayout.addView(detailText);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing appliance details: " + e.getMessage());
        }

        ImageButton editButton = new ImageButton(context);
        editButton.setImageResource(android.R.drawable.ic_menu_edit);
        editButton.setBackgroundColor(Color.TRANSPARENT);
        editButton.setOnClickListener(v -> showEditDialog(context, applianceDetails, friendlyName));
        applianceLayout.addView(editButton);

        cardView.addView(applianceLayout);
        return cardView;
    }

    private void showEditDialog(Context context, JSONObject applianceDetails, String friendlyName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Appliance: " + friendlyName);

        LinearLayout dialogLayout = new LinearLayout(context);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(20, 20, 20, 20);

        try {
            Iterator<String> keys = applianceDetails.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = applianceDetails.getString(key);

                TextView label = new TextView(context);
                label.setText(key + ":");

                if (key.equalsIgnoreCase("State")) {
                    Spinner stateSpinner = new Spinner(context);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, new String[]{"ON", "OFF"});
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    stateSpinner.setAdapter(adapter);
                    stateSpinner.setSelection(value.equalsIgnoreCase("true") ? 0 : 1);
                    dialogLayout.addView(label);
                    dialogLayout.addView(stateSpinner);
                } else if (key.equalsIgnoreCase("Speed") || key.equalsIgnoreCase("Temperature")) {
                    // SeekBar for numeric values
                    SeekBar seekBar = new SeekBar(context);
                    seekBar.setMax(100);
                    int defaultValue = Integer.parseInt(value.split("-")[0]);
                    seekBar.setProgress(defaultValue);

                    TextView seekBarValue = new TextView(context);
                    seekBarValue.setText(key + ": " + defaultValue);

                    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            seekBarValue.setText(key + ": " + progress);
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) { }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) { }
                    });

                    dialogLayout.addView(label);
                    dialogLayout.addView(seekBarValue);
                    dialogLayout.addView(seekBar);
                } else if (key.equalsIgnoreCase("Mode")) {
                    // Dropdown for list values
                    Spinner modeSpinner = new Spinner(context);
                    JSONArray validRange = new JSONArray(applianceDetails.getString("valid_range"));
                    List<String> options = new ArrayList<>();
                    for (int i = 0; i < validRange.length(); i++) {
                        options.add(validRange.getString(i));
                    }
                    ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, options);
                    modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    modeSpinner.setAdapter(modeAdapter);
                    modeSpinner.setSelection(options.indexOf(value));
                    dialogLayout.addView(label);
                    dialogLayout.addView(modeSpinner);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing appliance details for editing: " + e.getMessage());
        }

        builder.setView(dialogLayout);
        builder.setPositiveButton("Save", (dialog, which) -> Log.d(TAG, "Appliance updated"));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private JSONObject constructFeedbackJson() throws JSONException {
        // Construct the feedback JSON object based on user edits
        JSONArray feedbackApplianceStates = new JSONArray();

        for (int i = 0; i < applianceStateContainer.getChildCount(); i++) {
            View cardView = applianceStateContainer.getChildAt(i);
            JSONObject applianceState = new JSONObject();

            // Extract appliance state from the card view and fill applianceState JSON
            // Example: applianceState.put("State", "ON");

            feedbackApplianceStates.put(applianceState);
        }

        JSONObject feedbackJson = new JSONObject();
        feedbackJson.put("appliance_states", feedbackApplianceStates);

        originalJson.put("feedback", feedbackJson); // Add feedback to the original JSON
        logLongString("from constructFeedbackJson ", String.valueOf(originalJson));
        return originalJson;
    }

    private void sendFeedbackToServer(JSONObject feedbackJson) {
        // Send the feedback JSON to the server
        new Thread(() -> {
            try {
                URL url = new URL("http://52.250.54.24:3500/api/responseBuffer/update");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                OutputStream os = connection.getOutputStream();
                os.write(feedbackJson.toString().getBytes());
                os.close();

                int responseCode = connection.getResponseCode();
                logLongString(TAG, "Response Code: " + responseCode);
            } catch (Exception e) {
                Log.e(TAG, "Failed to send feedback: " + e.getMessage());
            }
        }).start();
    }

    private void showError(String message) {
        // Display error messages in the UI
        Log.e(TAG, message);
        rationaleText.setText(message);
        otherSuggestionsText.setText("");
    }
    private void fetchPromptResponseInterval(String mapRoomUserId) {
        Handler handler = new Handler(Looper.getMainLooper());

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) {
                    Log.w(TAG, "FeedbackFragment is not attached. Stopping polling.");
                    return; // Exit if the Fragment is no longer attached
                }

                Log.d(TAG, "Polling started for mapRoomUserId: " + mapRoomUserId);

                JSONObject json = new JSONObject();
                try {
                    json.put("mapRoomUserId", mapRoomUserId);
                    Log.d(TAG, "Polling request body: " + json.toString());
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to create polling JSON: " + e.getMessage());
                }

                String url = "http://52.250.54.24:3500/api/responseBuffer/find";

                RequestQueue queue = Volley.newRequestQueue(requireContext());
                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        json,
                        response -> {
                            if (!isAdded()) {
                                Log.w(TAG, "FeedbackFragment is not attached. Ignoring response.");
                                return; // Exit if the Fragment is no longer attached
                            }

                            Log.d(TAG, "Polling response received: " + response.toString());
                            String responseString = response.toString();

                            if (response.has("_id")) {
                                Log.d(TAG, "Response buffer found for mapRoomUserId: " + mapRoomUserId);
                                handler.removeCallbacks(this); // Stop polling

                                try {
                                    sharedViewModel.addResponse(mapRoomUserId, responseString);

                                    JSONObject responseObject = new JSONObject(responseString);
                                    if (responseObject.has("histItem") && responseObject.getJSONObject("histItem").has("room")) {
                                        JSONObject roomObj = responseObject.getJSONObject("histItem").getJSONObject("room");
                                        Map<String, String> applianceIdToNameMap = generateApplianceIdToNameMap(roomObj);

                                        requireActivity().runOnUiThread(() -> {
                                            handleRoomData(responseObject, applianceIdToNameMap);
                                        });
                                    } else {
                                        Log.e(TAG, "No 'room' data found in response. Retrying in 5 seconds...");
                                        handler.postDelayed(this, 5000);
                                    }
                                } catch (JSONException e) {
                                    Log.e(TAG, "Error parsing response JSON: " + e.getMessage());
                                    handler.postDelayed(this, 5000);
                                }
                            } else {
                                Log.d(TAG, "No response buffer found. Retrying in 5 seconds...");
                                handler.postDelayed(this, 5000);
                            }
                        },
                        error -> {
                            Log.e(TAG, "Volley error: " + error.toString());
                            handler.postDelayed(this, 5000);
                        }
                );
                queue.add(request);
            }
        };

        handler.post(runnable);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            Log.d(TAG, "Handler callbacks removed in onDestroy.");
        }
    }
}