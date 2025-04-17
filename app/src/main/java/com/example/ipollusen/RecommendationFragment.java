package com.example.ipollusen;


import static com.example.ipollusen.RoomsFragment.API_URL_ROOMS;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RecommendationFragment extends Fragment {

    private RecyclerView recyclerView;
    private RoomRecommendAdapter roomrecommendAdapter;
    private List<RoomRecommendModel> roomList;
    private UserViewModel userViewModel;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final OkHttpClient client = new OkHttpClient();
    private static final String TAG = "RecommendationFragment";
    private static final String API_URL_ROOM_LIST = "http://52.250.54.24:3500/api/mapRoomUser/search";
    private SharedResponseViewModel sharedViewModel;
    // Location and prompt sending variables
    private HashMap<String, String> mapRoomUserToRoomId = new HashMap<>(); // Maps mapRoomUserId to roomId
    private ProgressBar progressBar;
    private TextView noDataTextView;
    private double userLat = 0.0;
    private double userLong = 0.0;
    private String mapRoomUserId = "";
    private String selectedModifier = "health";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private List<String> mapRoomUserIds = new ArrayList<>(); // Holds the MapRoomUser IDs
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_recommendation, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewRecommendations);
        roomList = new ArrayList<>();
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedResponseViewModel.class);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        progressBar = view.findViewById(R.id.progressBar);
        noDataTextView = view.findViewById(R.id.noDataTextView);
        // Use anonymous inner classes to provide the callbacks (replace lambda if issues occur)
        fetchUserRooms(() -> {
            Log.d(TAG, "User rooms fetched successfully. Proceeding to fetch room details.");

            // Extract roomIds from the map and use them to fetch room details
            List<String> roomIds = new ArrayList<>(mapRoomUserToRoomId.values());
            fetchRoomDetails(roomIds);

            // Check response buffer for all MapRoomUser IDs
            checkResponseBufferForAllRooms(new ArrayList<>(mapRoomUserToRoomId.keySet()), () -> {
                Log.d(TAG, "Response buffer check completed. Initializing the adapter.");

                // Initialize and set the RoomRecommendAdapter
                roomrecommendAdapter = new RoomRecommendAdapter(
                        roomList,
                        (room, playButton, chatButton) -> {
                            // Handle play/pause logic when the button is clicked
                            togglePlayPause(room, playButton, chatButton);
                        },
                        room -> {
                            // Handle custom prompt dialog when the room is clicked
                            openCustomPromptDialog(room);
                        },
                        position -> {
                            // Handle feedback fragment navigation when a room is selected
                            RoomRecommendModel selectedRoom = roomList.get(position);
                            Log.d(TAG, "Feedback Fragment Opened for Room: " + selectedRoom.getRoomName());

                            // Reset the new recommendation flag if present
                            if (selectedRoom.isHasNewRecommendation()) {
                                selectedRoom.setHasNewRecommendation(false);
                                roomrecommendAdapter.notifyItemChanged(position);
                            }

                            // Navigate to the Feedback Fragment
                            NavController navController = Navigation.findNavController(requireView());
                            NavOptions navOptions = new NavOptions.Builder()
                                    .setPopUpTo(R.id.navigation_recommend, false) // Keeps `navigation_recommend` in the back stack
                                    .build();
                            navController.navigate(R.id.action_navigation_recommend_to_feedbackFragment, null, navOptions);
                        }
                );

                // Attach the adapter to the RecyclerView
                recyclerView.setAdapter(roomrecommendAdapter);
                Log.d(TAG, "Adapter initialized and set to RecyclerView.");
            });
        });
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        NavController navController = Navigation.findNavController(requireView());

                        // Log the current destination for debugging
                        if (navController.getCurrentDestination() != null) {
                            Log.d("NavController", "Current destination: " + navController.getCurrentDestination().getId());
                        }

                        if (navController.getCurrentDestination() != null &&
                                navController.getCurrentDestination().getId() == R.id.navigation_recommend) {
                            // Navigate to DashboardFragment
                            navController.navigate(R.id.action_navigation_recommend_to_navigation_dashboard);
                        } else {
                            Log.e("RecommendationFragment", "Current destination is not RecommendationFragment. Back action ignored.");
                        }
                    }
                });







        return view;
    }

    // Fetch location then run next step after fetching
    private void fetchLocation(Runnable onLocationFetched) {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Check if location providers are enabled
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGPSEnabled && !isNetworkEnabled) {
            showToast("Location services are disabled. Please enable them in settings.");
            return;
        }

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                userLat = location.getLatitude();
                userLong = location.getLongitude();

                Log.d(TAG, "User Location Fetched: Lat=" + userLat + ", Long=" + userLong);

                if (onLocationFetched != null) {
                    onLocationFetched.run();  // Execute your next task after location fetched
                }
                locationManager.removeUpdates(this); // Stop listening for further updates
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
                Log.d(TAG, "Location provider enabled: " + provider);
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                Log.d(TAG, "Location provider disabled: " + provider);
            }
        };

        if (isNetworkEnabled) {
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null);
        } else if (isGPSEnabled) {locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
        } else {
            showToast("No location provider available.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, try fetching location again
                // You might want to re-trigger the action that required location here, e.g.,
                // if (someConditionThatTriggeredLocationRequest) {
                //     fetchLocation(someCallback);
                // }
                Log.d(TAG, "Location permission granted.");
            } else {
                // Permission denied
                showToast("Location permission denied. Some features may not work.");
                Log.w(TAG, "Location permission denied by user.");
            }
        }
    }

    /**
     * Checks responseBuffer for all rooms and updates play/pause button state accordingly.
     */



    private void togglePlayPause(RoomRecommendModel room, ImageButton btnPlayPause, ImageButton btnChatBubble) {
        if (!btnPlayPause.isSelected()) {
            // Set the play button to the selected (pause) state and reveal the chat bubble
            btnPlayPause.setSelected(true);
            btnChatBubble.setVisibility(View.VISIBLE);
            Log.d(TAG, "Play action: Responses enabled for room " + room.getRoomId());
        } else {
            // Set the play button to unselected (play) state, hide the chat bubble, and handle pause action
            btnPlayPause.setSelected(false);
            btnChatBubble.setVisibility(View.GONE);
            Log.d(TAG, "Pause action: Remove responses for room " + room.getRoomId());

            // TODO: Call backend to remove responses from "response" and recheck buffers
//            removeResponsesAndRecheck(room);
        }
    }


    /**
     * New method to open a custom prompt dialog.
     * When the chat bubble is clicked, show a dialog to collect custom prompt and modifier.
     * On clicking GO, fetch location then call fetchMapRoomUserIdAndSendPrompt.
     * When successful, open the FeedbackFragment.
     */
    private void openCustomPromptDialog(RoomRecommendModel room) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_custom_prompt, null);
        Spinner spinnerModifier = dialogView.findViewById(R.id.spinnerModifier);
        EditText etCustomPrompt = dialogView.findViewById(R.id.etCustomPrompt);
        Button btnSendPrompt = dialogView.findViewById(R.id.btnSendPrompt);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"health", "comfort", "balanced"}
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModifier.setAdapter(spinnerAdapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView).setCancelable(true);
        final AlertDialog dialog = builder.create();

        btnSendPrompt.setOnClickListener(v -> {
            String customPrompt = etCustomPrompt.getText().toString().trim();

            if (customPrompt.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a custom prompt", Toast.LENGTH_SHORT).show();
                return;
            }

            selectedModifier = spinnerModifier.getSelectedItem().toString();

            Log.d(TAG, "Selected Modifier: " + selectedModifier);
            Log.d(TAG, "Custom Prompt: " + customPrompt);

            // STEP 1 → Fetch Location first
            fetchLocation(() -> {
                // STEP 2 → Then Fetch MapRoomUserId & Send Initial Prompt
                fetchMapRoomUserIdAndSendPrompt(room.getRoomId(), customPrompt, () -> {
                    // STEP 3 → After Sending Prompt Successfully, Go to Feedback Fragment
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.nav_host_fragment_activity_main, new FeedbackFragment())
                            .addToBackStack(null)
                            .commit();
                });
            });

            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Overloaded fetchMapRoomUserIdAndSendPrompt to accept a completion callback.
     */
    private void fetchMapRoomUserIdAndSendPrompt(String roomId, String customPrompt, Runnable onSuccess) {
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
                                        sendInitialPrompt(customPrompt, onSuccess);
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

    /**
     * Updates sendInitialPrompt() to accept a success callback.
     * This function sends the POST request to http://nitdgp2.a.pinggy.link/acgpt
     * using the following JSON format:
     * {
     *   "lat": 23.5428341,
     *   "long": 87.3441529,
     *   "mapRoomUserId": "67f0dc88aa84fb3650853a76",
     *   "modifier": "health",
     *   "customprompt": "cool and ventilated"
     * }
     */
    private void sendInitialPrompt(String customPrompt, Runnable onSuccess) {
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

                        // Start checking for response buffer after sending prompt
                        fetchPromptResponseInterval(mapRoomUserId);

                        if (onSuccess != null) {
                            onSuccess.run();
                        }
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

    private void fetchPromptResponseInterval(String mapRoomUserId) {
        Handler handler = new Handler(Looper.getMainLooper());

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Polling started for mapRoomUserId: " + mapRoomUserId);

                JSONObject json = new JSONObject();
                try {
                    json.put("mapRoomUserId", mapRoomUserId);
                    Log.d(TAG, "Polling request body: " + json.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String url = "http://52.250.54.24:3500/api/responseBuffer/find";

                RequestQueue queue = Volley.newRequestQueue(requireContext());
                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        json,
                        response -> {
                            Log.d(TAG, "Polling response received: " + response.toString());
                            String responseString = response.toString();  // Now declared here properly
                            Log.d(TAG, "Polling Response JSON String: " + responseString);

                            if (response.has("_id")) {
                                Log.d(TAG, "Response buffer found for mapRoomUserId: " + mapRoomUserId);
                                handler.removeCallbacks(this);  // Stop polling

                                // Save response using the shared ViewModel
                                sharedViewModel.addResponse(mapRoomUserId, responseString);

                                // Update UI for room list if needed
                                if (isAdded() && getActivity() != null) {
                                    requireActivity().runOnUiThread(() -> {
                                        // Your room list update code goes here
                                        for (int i = 0; i < roomList.size(); i++) {
                                            if (roomList.get(i).getRoomId().equals(mapRoomUserId)) {
                                                roomList.get(i).setHasNewRecommendation(true);
                                                roomrecommendAdapter.notifyItemChanged(i);
                                                Log.d(TAG, "newRecDot shown at position: " + i);
                                                break;
                                            }
                                        }
                                        // Optionally navigate to FeedbackFragment


                                        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                                        Bundle args = new Bundle();
                                        args.putString("mapRoomUserId", mapRoomUserId); // Pass arguments if needed

                                        navController.navigate(R.id.action_navigation_recommend_to_feedbackFragment, args);

                                    });
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




    /**
     * Fetch prompt response (if desired).
     * Existing implementation.
     */
//

    // ------------------------------------------------------------------------
    // Existing code for fetching user rooms and room details from your backend
    // ------------------------------------------------------------------------
    private void fetchUserRooms(Runnable onComplete) {
        String userId = userViewModel.getUserId().getValue();
        if (userId == null || userId.isEmpty()) {
            showToast("User ID not found");
            return;
        }
        showLoading();
        executorService.execute(() -> {
            try {
                JSONObject requestJson = new JSONObject();
                requestJson.put("userId", userId);

                RequestBody body = RequestBody.create(
                        MediaType.get("application/json; charset=utf-8"),
                        requestJson.toString()
                );

                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(API_URL_ROOM_LIST)
                        .post(body)
                        .header("Content-Type", "application/json")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }

                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "User rooms mapping response: " + responseBody);

                    mapRoomUserToRoomId.clear(); // Clear previous data
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray dataArray = jsonResponse.getJSONArray("data");

                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject roomData = dataArray.getJSONObject(i);
                        String mapRoomUserId = roomData.getString("_id");
                        String roomId = roomData.getString("roomId");
                        mapRoomUserToRoomId.put(mapRoomUserId, roomId);
                    }

                    Log.d(TAG, "Fetched MapRoomUser to Room ID map: " + mapRoomUserToRoomId);

                    requireActivity().runOnUiThread(onComplete);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching user rooms", e);
                showToast("Failed to fetch user rooms");
                showNoData();
            }
        });
    }
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        noDataTextView.setVisibility(View.GONE);
    }

    private void showRecyclerView() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        noDataTextView.setVisibility(View.GONE);
    }

    private void showNoData() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        noDataTextView.setVisibility(View.VISIBLE);
    }
    private void fetchRoomDetails(List<String> roomIds) {
        executorService.execute(() -> {
            try {
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(API_URL_ROOMS)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }

                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "All rooms response: " + responseBody);

                    List<RoomRecommendModel> matchedRooms = new ArrayList<>();
                    JSONArray roomsArray = new JSONArray(responseBody);

                    for (int i = 0; i < roomsArray.length(); i++) {
                        JSONObject roomJson = roomsArray.getJSONObject(i);
                        String roomId = roomJson.getString("_id");

                        if (roomIds.contains(roomId)) {
                            String roomName = roomJson.getString("roomName");
                            String roomDesc = roomJson.optString("roomDesc", "No description available");
                            int length = roomJson.getInt("length");
                            int breadth = roomJson.getInt("breadth");

                            matchedRooms.add(new RoomRecommendModel(roomId, roomName, roomDesc, length, breadth));
                        }
                    }

                    requireActivity().runOnUiThread(() -> {
                        showRecyclerView();
                        roomList.clear();
                        roomList.addAll(matchedRooms);

                        if (roomrecommendAdapter != null) {
                            roomrecommendAdapter.notifyDataSetChanged();
                        }

                        Log.d(TAG, "Updated UI with matched room details.");
                    });
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error fetching room details", e);
                showToast("Failed to fetch room details");
            }
        });
    }

    private void checkResponseBufferForAllRooms(List<String> mapRoomUserIds, Runnable onComplete) {
        if (mapRoomUserIds == null || mapRoomUserIds.isEmpty()) {
            Log.d(TAG, "No MapRoomUser IDs found. Skipping response buffer check.");
            if (onComplete != null) onComplete.run();
            return;
        }

        int[] processedCount = {0};

        for (String mapRoomUserId : mapRoomUserIds) {
            if (mapRoomUserId == null) {
                Log.e(TAG, "MapRoomUser ID is null. Skipping this ID.");
                processedCount[0]++;
                if (processedCount[0] == mapRoomUserIds.size() && onComplete != null) {
                    onComplete.run();
                }
                continue;
            }

            try {
                // Create JSON payload for the POST request
                JSONObject json = new JSONObject();
                json.put("mapRoomUserId", mapRoomUserId);

                String url = "http://52.250.54.24:3500/api/responseBuffer/find";
                RequestQueue queue = Volley.newRequestQueue(requireContext());

                // Create a POST request
                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        json,
                        response -> {
                            try {
                                // Check if the response contains the "mapRoomUserId" and matches the current mapRoomUserId
                                boolean hasMatchingResponse = response.has("mapRoomUserId") && mapRoomUserId.equals(response.getString("mapRoomUserId"));

                                // Update the UI based on the presence of a matching response
                                requireActivity().runOnUiThread(() -> {
                                    for (int i = 0; i < roomList.size(); i++) {
                                        RoomRecommendModel room = roomList.get(i);
                                        if (room != null && mapRoomUserId.equals(room.getMapRoomUserId())) {
                                            if (hasMatchingResponse) {
                                                room.setHasNewRecommendation(true); // Indicate a new recommendation
                                                room.setResponseFound(true); // Show pause button
                                                Log.d(TAG, "Pause button shown for MapRoomUser ID: " + mapRoomUserId);
                                            } else {
                                                room.setResponseFound(false); // Show play button
                                                Log.d(TAG, "Play button shown for MapRoomUser ID: " + mapRoomUserId);
                                            }
                                            roomrecommendAdapter.notifyItemChanged(i);
                                            break;
                                        }
                                    }
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing response for MapRoomUser ID: " + mapRoomUserId, e);
                            } finally {
                                processedCount[0]++;
                                if (processedCount[0] == mapRoomUserIds.size() && onComplete != null) {
                                    onComplete.run();
                                }
                            }
                        },
                        error -> {
                            Log.e(TAG, "Error checking responseBuffer for MapRoomUser ID: " + mapRoomUserId, error);
                            processedCount[0]++;
                            if (processedCount[0] == mapRoomUserIds.size() && onComplete != null) {
                                onComplete.run();
                            }
                        }
                );

                // Add the request to the Volley queue
                queue.add(request);
            } catch (JSONException e) {
                Log.e(TAG, "Error forming JSON for MapRoomUser ID: " + mapRoomUserId, e);
                processedCount[0]++;
                if (processedCount[0] == mapRoomUserIds.size() && onComplete != null) {
                    onComplete.run();
                }
            }
        }
    }

    private void handlePlayButtonClick(RoomRecommendModel room, ImageButton btnPlayPause, ImageButton btnChatBubble) {
        if (room.isResponseFound()) {
            // If a response is found, show the pause state and the chat bubble
            btnPlayPause.setSelected(true);
            btnChatBubble.setVisibility(View.VISIBLE);
            Log.d(TAG, "Pause button shown for Room ID: " + room.getRoomId());
        } else {
            // If no response is found, show the play state and hide the chat bubble
            btnPlayPause.setSelected(false);
            btnChatBubble.setVisibility(View.GONE);
            Log.d(TAG, "Play button shown for Room ID: " + room.getRoomId());
        }
    }

    // Display toast on UI thread
    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        );
    }
}
