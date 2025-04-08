package com.example.ipollusen;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.android.volley.*;
import com.android.volley.toolbox.*;

import org.json.*;

public class RecommendationPromptFragment extends Fragment {

    private static final String TAG = "RecPromptFragment";

    private static final String ARG_ROOM_ID = "room_id";
    private static final String ARG_ROOM_NAME = "room_name";
    private static final String ARG_ROOM_DESC = "room_desc";
    private static final String ARG_LENGTH = "length";
    private static final String ARG_BREADTH = "breadth";
    private static final String ARG_NODE_IDS = "node_ids";

    private UserViewModel userViewModel;
    private double userLat = 0.0;
    private double userLong = 0.0;
    private String selectedModifier = "health";
    private String mapRoomUserId = "";

    private TextView htmlResponseTextView;
    private EditText inputCustomPrompt;

    public static RecommendationPromptFragment newInstance(String roomId, String roomName, String roomDesc,
                                                           int length, int breadth, String nodeIds) {
        RecommendationPromptFragment fragment = new RecommendationPromptFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        args.putString(ARG_ROOM_NAME, roomName);
        args.putString(ARG_ROOM_DESC, roomDesc);
        args.putInt(ARG_LENGTH, length);
        args.putInt(ARG_BREADTH, breadth);
        args.putString(ARG_NODE_IDS, nodeIds);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recommendation_prompt, container, false);

        inputCustomPrompt = view.findViewById(R.id.inputCustomPrompt);
        Spinner spinnerModifier = view.findViewById(R.id.spinnerModifier);
        htmlResponseTextView = view.findViewById(R.id.htmlResponseTextView);
        Button btnSendPrompt = view.findViewById(R.id.btnSendPrompt);
        // Assume your toolbar contains the back arrow. If using a Toolbar:
        // Toolbar toolbar = view.findViewById(R.id.toolbar);
        // toolbar.setNavigationOnClickListener(v -> navigateBack());
        // Or if you have a dedicated button for the back arrow:

        // Also if you have a close button:
        Button btnClose = view.findViewById(R.id.closeButton);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                NavController navController = NavHostFragment.findNavController(this);
                navController.navigate(R.id.action_navigation_reccomend_prompt_to_navigation_recommend);
            });
        }


        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"health", "comfort", "balanced"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModifier.setAdapter(adapter);

        spinnerModifier.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedModifier = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Modifier selected: " + selectedModifier);
            }
            public void onNothingSelected(AdapterView<?> parent) {
                selectedModifier = "health";
            }
        });

        String roomId = "";
        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID, "");
        }

        fetchLocation();

        String finalRoomId = roomId;
        btnSendPrompt.setOnClickListener(v -> {
            String customPrompt = inputCustomPrompt.getText().toString().trim();
            Log.d(TAG, "Send button clicked with customPrompt: " + customPrompt);
            fetchMapRoomUserIdAndSendPrompt(finalRoomId, customPrompt);
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Handle system back button press using the helper method
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        navigateBack();
                    }
                });
    }

    // Helper method: Try navigateUp; if that fails, pop back stack.
    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        if (!navController.navigateUp()) {
            navController.popBackStack();
        }
    }

    private void fetchLocation() {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            }, 1);
            return;
        }

        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                userLat = location.getLatitude();
                userLong = location.getLongitude();
                Log.d(TAG, "User location fetched: Lat = " + userLat + ", Long = " + userLong);
            }
        }, null);
    }

    private void fetchMapRoomUserIdAndSendPrompt(String roomId, String customPrompt) {
        try {
            String userId = userViewModel.getUserId().getValue();
            if (userId == null || userId.isEmpty()) {
                Toast.makeText(requireContext(), "User ID not available", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject requestJson = new JSONObject();
            requestJson.put("userId", userId);

            String url = "http://52.250.54.24:3500/api/mapRoomUser/search";
            Log.d(TAG, "Requesting mapRoomUserId for userId: " + userId);

            RequestQueue queue = Volley.newRequestQueue(requireContext());

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestJson,
                    response -> {
                        try {
                            JSONArray items = response.optJSONArray("data");
                            if (items != null) {
                                for (int i = 0; i < items.length(); i++) {
                                    JSONObject item = items.getJSONObject(i);
                                    if (roomId.equals(item.optString("roomId"))) {
                                        mapRoomUserId = item.optString("_id");
                                        Log.d(TAG, "Matched mapRoomUserId: " + mapRoomUserId);
                                        sendInitialPrompt(customPrompt);
                                        return;
                                    }
                                }
                                Toast.makeText(requireContext(), "Room ID not found for user", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Error parsing mapRoomUser response", e);
                        }
                    },
                    error -> {
                        Log.e(TAG, "Volley error while fetching mapRoomUserId", error);
                        Toast.makeText(requireContext(), "Failed to retrieve mapRoomUserId", Toast.LENGTH_SHORT).show();
                    });

            queue.add(request);
        } catch (Exception e) {
            Log.e(TAG, "Error forming mapRoomUser request", e);
        }
    }

    private void sendInitialPrompt(String customPrompt) {
        try {
            JSONObject json = new JSONObject();
            json.put("lat", userLat);
            json.put("long", userLong);
            json.put("mapRoomUserId", mapRoomUserId);
            json.put("modifier", selectedModifier);
            json.put("customprompt", customPrompt);

            String urlPrompt = "http://nitdgp2.a.pinggy.link/acgpt";
            Log.d(TAG, "Sending prompt: " + json.toString());

            RequestQueue queue = Volley.newRequestQueue(requireContext());
            JsonObjectRequest promptRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    urlPrompt,
                    json,
                    response -> {
                        Log.d(TAG, "Prompt sent successfully. Response: " + response.toString());
                        Toast.makeText(requireContext(), "Prompt Sent!", Toast.LENGTH_SHORT).show();
                        fetchPromptResponse();
                    },
                    error -> {
                        Log.e(TAG, "Failed to send prompt", error);
                        Toast.makeText(requireContext(), "Failed to send prompt.", Toast.LENGTH_SHORT).show();
                    }
            );
            promptRequest.setRetryPolicy(new DefaultRetryPolicy(
                    20000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(promptRequest);
        } catch (Exception e) {
            Log.e(TAG, "Error forming JSON for prompt", e);
            Toast.makeText(requireContext(), "Error forming JSON!", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchPromptResponse() {
        try {
            JSONObject json = new JSONObject();
            json.put("mapRoomUserId", mapRoomUserId);
            String urlResponse = "http://52.250.54.24:3500/api/responseBuffer/find";
            Log.d(TAG, "Fetching prompt response with mapRoomUserId: " + mapRoomUserId);

            RequestQueue queue = Volley.newRequestQueue(requireContext());
            JsonObjectRequest responseRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    urlResponse,
                    json,
                    response -> {
                        Log.d(TAG, "Received prompt response: " + response.toString());

                        StringBuilder builder = new StringBuilder();

                        // Add Rationale (if exists)
                        String rationale = response.optString("rationale", "");
                        if (!rationale.isEmpty()) {
                            builder.append("Rationale:\n").append(rationale).append("\n\n");
                        }

                        // Add Suggestions (if exists)
                        JSONArray promptsArray = response.optJSONArray("prompts");
                        if (promptsArray != null && promptsArray.length() > 0) {
                            builder.append("Suggestions:\n");
                            for (int i = 0; i < promptsArray.length(); i++) {
                                builder.append("• ").append(promptsArray.optString(i, "")).append("\n\n");
                            }
                        }

                        // Fallback to raw response if nothing found
                        if (builder.length() == 0) {
                            builder.append(response.toString());
                        }

                        htmlResponseTextView.setText(builder.toString().trim());
                    },
                    error -> {
                        htmlResponseTextView.setText("Failed to retrieve recommendations.");
                        Log.e(TAG, "Error fetching prompt response", error);
                    }
            );
            responseRequest.setRetryPolicy(new DefaultRetryPolicy(
                    15000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(responseRequest);
        } catch (Exception e) {
            Log.e(TAG, "Error forming prompt response request", e);
        }
    }
}
