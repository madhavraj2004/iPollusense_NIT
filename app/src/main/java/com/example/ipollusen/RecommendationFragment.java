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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RecommendationFragment extends Fragment {
    private Map<String, String> roomToMapRoomUserIdMap = new HashMap<>();
    private RecyclerView recyclerView;
    private RoomRecommendAdapter roomrecommendAdapter;
    private List<RoomRecommendModel> roomList;
    private UserViewModel userViewModel;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final OkHttpClient client = new OkHttpClient();
    private static final String TAG = "RecommendationFragment";
    private static final String API_URL_ROOM_LIST = "http://52.250.54.24:3500/api/mapRoomUser/search";
    private SharedResponseViewModel sharedViewModel;
    private ProgressBar progressBar;
    private TextView noDataTextView;
    // Location and prompt sending variables
    private double userLat = 0.0;
    private double userLong = 0.0;
    private String mapRoomUserId = "";
    private String selectedModifier = "health";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
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
        roomrecommendAdapter = new RoomRecommendAdapter(roomList,
                (room, playButton, chatButton) -> handlePlayButtonClick(room, playButton, chatButton),
                room -> openCustomPromptDialog(room),
                position -> {
                    RoomRecommendModel selectedRoom = roomList.get(position);
                    String mapRoomUserId = roomToMapRoomUserIdMap.get(selectedRoom.getRoomId());

                    if (mapRoomUserId == null) {
                        Log.e(TAG, "Error: MapRoomUserId not found for room: " + selectedRoom.getRoomName());
                        showToast("Error: Room mapping not found");
                        return;
                    }

                    // Reset dot if shown
                    if (selectedRoom.isHasNewRecommendation()) {
                        selectedRoom.setHasNewRecommendation(false);
                        roomrecommendAdapter.notifyItemChanged(position);
                    }

                    // Create bundle with mapRoomUserId
                    Bundle args = new Bundle();
                    args.putString("mapRoomUserId", mapRoomUserId);

                    // Navigate to Feedback Fragment with mapRoomUserId
                    NavController navController = Navigation.findNavController(requireView());
                    NavOptions navOptions = new NavOptions.Builder()
                            .setPopUpTo(R.id.navigation_recommend, false)
                            .build();

                    navController.navigate(
                            R.id.action_navigation_recommend_to_feedbackFragment,
                            args,
                            navOptions
                    );
                }
        );
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

        recyclerView.setAdapter(roomrecommendAdapter);


        fetchUserRooms();


        return view;
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
     * New handler for play button clicks.
     * When not selected:
     *   - Set selected to true (so the selector shows pause),
     *   - Make chat bubble visible,
     *   - And send registration (ACGPT endpoint) with an empty custom prompt.
     * When already selected, treat it as a pause action and remove responses.
     */
    // Update the handlePlayButtonClick method
    private void handlePlayButtonClick(RoomRecommendModel room, ImageButton playButton, ImageButton chatButton) {
        String mapRoomUserId = roomToMapRoomUserIdMap.get(room.getRoomId());
        if (mapRoomUserId == null) {
            Log.e(TAG, "Play Button Click Error: MapRoomUserId not found for roomId: " + room.getRoomId());
            showToast("Error: Room mapping not found");
            return;
        }

        Log.d(TAG, String.format("Play Button Clicked - Room: %s, RoomId: %s, MapRoomUserId: %s, Current State: %s, Time: %s, User: %s",
                room.getRoomName(), room.getRoomId(), mapRoomUserId,
                playButton.isSelected() ? "Selected" : "Not Selected",
                "2025-04-25 11:38:51", "madhavraj2004"));

        if (!playButton.isSelected()) {
            // Handle Play Button Click
            Log.d(TAG, "Checking response buffer for room: " + room.getRoomName());
            JSONObject json = new JSONObject();
            try {
                json.put("mapRoomUserId", mapRoomUserId);
                Log.d(TAG, "Response Buffer Check Request - MapRoomUserId: " + mapRoomUserId);

                RequestQueue queue = Volley.newRequestQueue(requireContext());
                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        "http://52.250.54.24:3500/api/responseBuffer/find",
                        json,
                        response -> {
                            if (response.has("_id")) {
                                String responseId = response.optString("_id");
                                Log.d(TAG, String.format("Response Buffer Found - Room: %s, ResponseId: %s",
                                        room.getRoomName(), responseId));

                                playButton.setSelected(true);
                                chatButton.setVisibility(View.VISIBLE);
                                room.setHasExistingResponse(true);
                                Log.d(TAG, "UI Updated: Play button selected, Chat bubble shown");

                            } else if (response.optString("message", "").equals("Response Buffer item not found")) {
                                Log.d(TAG, "No Response Buffer found, preparing to send default prompt");

                                fetchLocation(() -> {
                                    try {
                                        JSONObject promptJson = new JSONObject();
                                        promptJson.put("lat", userLat);
                                        promptJson.put("long", userLong);
                                        promptJson.put("mapRoomUserId", mapRoomUserId);
                                        promptJson.put("modifier", "balanced");
                                        promptJson.put("customprompt", " ");

                                        Log.d(TAG, String.format("Sending Default Prompt - Room: %s\nPrompt Data: %s",
                                                room.getRoomName(), promptJson.toString()));

                                        JsonObjectRequest promptRequest = new JsonObjectRequest(
                                                Request.Method.POST,
                                                "http://nitdgp2.a.pinggy.link/acgpt",
                                                promptJson,
                                                promptResponse -> {
                                                    Log.d(TAG, String.format("Default Prompt Sent Successfully - Room: %s\nResponse: %s",
                                                            room.getRoomName(), promptResponse.toString()));

                                                    requireActivity().runOnUiThread(() -> {
                                                        playButton.setSelected(true);
                                                        chatButton.setVisibility(View.VISIBLE);
                                                        room.setHasExistingResponse(true);
                                                        Log.d(TAG, "UI Updated after prompt send: Play button selected, Chat bubble shown");
                                                    });

                                                    Log.d(TAG, "Starting response polling for room: " + room.getRoomName());
                                                    fetchPromptResponseInterval(mapRoomUserId);
                                                },
                                                error -> {
                                                    Log.e(TAG, String.format("Error Sending Default Prompt - Room: %s\nError: %s",
                                                            room.getRoomName(), error.toString()));

                                                    requireActivity().runOnUiThread(() -> {
                                                        playButton.setSelected(false);
                                                        chatButton.setVisibility(View.GONE);
                                                        showToast("Failed to send prompt");
                                                        Log.d(TAG, "UI Reset due to prompt send error");
                                                    });
                                                }
                                        );

                                        promptRequest.setRetryPolicy(new DefaultRetryPolicy(
                                                20000,
                                                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                                        ));

                                        queue.add(promptRequest);

                                    } catch (JSONException e) {
                                        Log.e(TAG, String.format("JSON Creation Error - Room: %s\nError: %s",
                                                room.getRoomName(), e.getMessage()));
                                        showToast("Error creating prompt");
                                    }
                                });
                            }
                        },
                        error -> {
                            Log.e(TAG, String.format("Response Buffer Check Error - Room: %s\nError: %s",
                                    room.getRoomName(), error.toString()));
                            showToast("Error checking response buffer");
                        }
                );
                queue.add(request);
            } catch (JSONException e) {
                Log.e(TAG, String.format("JSON Creation Error for Buffer Check - Room: %s\nError: %s",
                        room.getRoomName(), e.getMessage()));
                showToast("Error checking response buffer");
            }
        } else {
            // Handle Pause Button Click
            Log.d(TAG, String.format("Pause Button Clicked - Room: %s, Starting pause process", room.getRoomName()));

            JSONObject json = new JSONObject();
            try {
                json.put("mapRoomUserId", mapRoomUserId);
                Log.d(TAG, "Checking response buffer to get responseID - MapRoomUserId: " + mapRoomUserId);

                RequestQueue queue = Volley.newRequestQueue(requireContext());
                JsonObjectRequest responseBufferRequest = new JsonObjectRequest(
                        Request.Method.POST,
                        "http://52.250.54.24:3500/api/responseBuffer/find",
                        json,
                        response -> {
                            try {
                                if (response.has("_id")) {
                                    String responseId = response.getString("_id");
                                    Log.d(TAG, String.format("Response Buffer Found - ResponseID: %s", responseId));

                                    JSONObject recheckJson = new JSONObject();
                                    recheckJson.put("responseID", responseId);
                                    Log.d(TAG, "Finding recheck buffer with ResponseID: " + responseId);

                                    JsonObjectRequest recheckFindRequest = new JsonObjectRequest(
                                            Request.Method.POST,
                                            "http://52.250.54.24:3500/api/recheckBuffer/find",
                                            recheckJson,
                                            recheckResponse -> {
                                                try {
                                                    if (recheckResponse.has("_id")) {
                                                        String recheckId = recheckResponse.getString("_id");
                                                        Log.d(TAG, String.format("Recheck Buffer Found - RecheckID: %s", recheckId));

                                                        JSONObject deleteJson = new JSONObject();
                                                        deleteJson.put("_id", recheckId);
                                                        Log.d(TAG, "Deleting recheck buffer with ID: " + recheckId);

                                                        JsonObjectRequest deleteRequest = new JsonObjectRequest(
                                                                Request.Method.POST,
                                                                "http://52.250.54.24:3500/api/recheckBuffer/delete",
                                                                deleteJson,
                                                                deleteResponse -> {
                                                                    Log.d(TAG, String.format("Recheck Buffer Deleted Successfully - Room: %s",
                                                                            room.getRoomName()));

                                                                    requireActivity().runOnUiThread(() -> {
                                                                        playButton.setSelected(false);
                                                                        chatButton.setVisibility(View.GONE);
                                                                        room.setHasExistingResponse(false);
                                                                        Log.d(TAG, "UI Updated: Play button unselected, Chat bubble hidden");
                                                                    });
                                                                },
                                                                error -> Log.e(TAG, String.format("Error Deleting Recheck Buffer - Room: %s\nError: %s",
                                                                        room.getRoomName(), error.toString()))
                                                        );
                                                        queue.add(deleteRequest);
                                                    } else {
                                                        Log.d(TAG, "No recheck buffer found, proceeding with UI update");
                                                        requireActivity().runOnUiThread(() -> {
                                                            playButton.setSelected(false);
                                                            chatButton.setVisibility(View.GONE);
                                                            room.setHasExistingResponse(false);
                                                        });
                                                    }
                                                } catch (JSONException e) {
                                                    Log.e(TAG, "Error processing recheck buffer deletion", e);
                                                }
                                            },
                                            error -> Log.e(TAG, String.format("Error Finding Recheck Buffer - Room: %s\nError: %s",
                                                    room.getRoomName(), error.toString()))
                                    );
                                    queue.add(recheckFindRequest);
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "Error processing response buffer", e);
                            }
                        },
                        error -> Log.e(TAG, String.format("Error Finding Response Buffer - Room: %s\nError: %s",
                                room.getRoomName(), error.toString()))
                );
                queue.add(responseBufferRequest);

            } catch (JSONException e) {
                Log.e(TAG, String.format("JSON Creation Error - Room: %s\nError: %s",
                        room.getRoomName(), e.getMessage()));
            }
        }
    }

    // New method to check response buffer
    private void checkResponseBuffer(String mapRoomUserId, RoomRecommendModel room, ImageButton btnPlayPause, ImageButton btnChatBubble) {
        JSONObject json = new JSONObject();
        try {
            json.put("mapRoomUserId", mapRoomUserId);
            String url = "http://52.250.54.24:3500/api/responseBuffer/find";

            RequestQueue queue = Volley.newRequestQueue(requireContext());
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    json,
                    response -> {
                        if (response.has("_id")) {
                            // Response buffer exists, navigate to feedback fragment
                            NavController navController = Navigation.findNavController(requireView());
                            Bundle args = new Bundle();
                            args.putString("mapRoomUserId", mapRoomUserId);
                            NavOptions navOptions = new NavOptions.Builder()
                                    .setPopUpTo(R.id.navigation_recommend, false)
                                    .build();
                            navController.navigate(R.id.action_navigation_recommend_to_feedbackFragment, args, navOptions);
                        } else if (response.optString("message", "").equals("Response Buffer item not found")) {
                            // No response in buffer, show play button and chat bubble
                            requireActivity().runOnUiThread(() -> {
                                btnPlayPause.setSelected(true);
                                btnChatBubble.setVisibility(View.VISIBLE);
                                Log.d(TAG, "No response in buffer, showing play button and chat bubble");
                            });
                        }
                    },
                    error -> {
                        Log.e(TAG, "Volley error while checking response buffer", error);
                        // On error, default to showing play button and chat bubble
                        requireActivity().runOnUiThread(() -> {
                            btnPlayPause.setSelected(true);
                            btnChatBubble.setVisibility(View.VISIBLE);
                        });
                    }
            );
            queue.add(request);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON for response buffer check", e);
        }
    }    /**
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

    // Update fetchPromptResponseInterval to not navigate to feedback fragment
    private void fetchPromptResponseInterval(String mapRoomUserId) {
        Handler handler = new Handler(Looper.getMainLooper());

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                JSONObject json = new JSONObject();
                try {
                    json.put("mapRoomUserId", mapRoomUserId);

                    RequestQueue queue = Volley.newRequestQueue(requireContext());
                    JsonObjectRequest request = new JsonObjectRequest(
                            Request.Method.POST,
                            "http://52.250.54.24:3500/api/responseBuffer/find",
                            json,
                            response -> {
                                if (response.has("_id")) {
                                    Log.d(TAG, "Response buffer found for mapRoomUserId: " + mapRoomUserId);
                                    handler.removeCallbacks(this);  // Stop polling

                                    // Save response using the shared ViewModel
                                    sharedViewModel.addResponse(mapRoomUserId, response.toString());

                                    // Update UI to show new recommendation dot
                                    if (isAdded() && getActivity() != null) {
                                        requireActivity().runOnUiThread(() -> {
                                            for (int i = 0; i < roomList.size(); i++) {
                                                if (roomList.get(i).getRoomId().equals(mapRoomUserId)) {
                                                    roomList.get(i).setHasNewRecommendation(true);
                                                    roomrecommendAdapter.notifyItemChanged(i);
                                                    break;
                                                }
                                            }
                                        });
                                    }
                                } else {
                                    Log.d(TAG, "No response buffer found. Retrying in 5 seconds...");
                                    handler.postDelayed(this, 5000);
                                }
                            },
                            error -> {
                                Log.e(TAG, "Error checking response buffer", error);
                                handler.postDelayed(this, 5000);
                            }
                    );
                    queue.add(request);
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating JSON for response buffer check", e);
                }
            }
        };

        handler.post(runnable);
    }


    // ------------------------------------------------------------------------
    // Existing code for fetching user rooms and room details from your backend
    // ------------------------------------------------------------------------
    private void fetchUserRooms() {
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

                    List<String> roomIds = new ArrayList<>();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray dataArray = jsonResponse.getJSONArray("data");

                    for (int i = 0; i < dataArray.length(); i++) {
                        roomIds.add(dataArray.getJSONObject(i).getString("roomId"));
                    }

                    Log.d(TAG, "Fetched Room IDs: " + roomIds);
                    fetchRoomDetails(roomIds);
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error fetching user rooms", e);
                showToast("Failed to fetch user rooms");
                showNoData();
            }
        });
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
                        roomrecommendAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Updated UI with matched room details.");

                        // Call the mapping function after UI is updated
                        fetchMapRoomUserIdsAndCreateMapping();
                    });
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error fetching room details", e);
                showToast("Failed to fetch room details");
            }
        });
    }
    private void fetchMapRoomUserIdsAndCreateMapping() {
        String userId = userViewModel.getUserId().getValue();
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID not available for mapping");
            return;
        }

        try {
            JSONObject requestJson = new JSONObject();
            requestJson.put("userId", userId);

            RequestQueue queue = Volley.newRequestQueue(requireContext());
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    "http://52.250.54.24:3500/api/mapRoomUser/search",
                    requestJson,
                    response -> {
                        try {
                            JSONArray items = response.optJSONArray("data");
                            if (items != null) {
                                for (int i = 0; i < roomList.size(); i++) {
                                    RoomRecommendModel room = roomList.get(i);
                                    String roomId = room.getRoomId();

                                    // Find matching mapRoomUserId for this room
                                    for (int j = 0; j < items.length(); j++) {
                                        JSONObject item = items.getJSONObject(j);
                                        if (roomId.equals(item.optString("roomId"))) {
                                            String mapRoomUserId = item.optString("_id");
                                            roomToMapRoomUserIdMap.put(roomId, mapRoomUserId);
                                            Log.d(TAG, String.format("Room Mapping - Position: %d, Room Name: %s, RoomId: %s, MapRoomUserId: %s",
                                                    i, room.getRoomName(), roomId, mapRoomUserId));
                                            break;
                                        }
                                    }
                                }
                                Log.d(TAG, "Complete Room to MapRoomUserId mapping: " + roomToMapRoomUserIdMap.toString());
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing mapRoomUser response", e);
                        }
                    },
                    error -> Log.e(TAG, "Error fetching mapRoomUserIds", error)
            );

            request.setRetryPolicy(new DefaultRetryPolicy(
                    20000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            queue.add(request);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating request JSON for mapRoomUser", e);
        }
    }
    // Display toast on UI thread
    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        );
    }
}
