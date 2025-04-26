package com.example.ipollusen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RoomDetailsFragment extends Fragment {
    private EditText inputRoomName, inputRoomDesc, inputLength, inputBreadth, inputNodeIds;
    private Button btnAddAppliance, btnSave;
    private RecyclerView recyclerViewAppliances;

    // We'll use ApplianceJsonAdapter to display the JSON data from ApplianceFragment.
    private ApplianceJsonAdapter applianceJsonAdapter;
    private ArrayList<JSONObject> applianceJsonList = new ArrayList<>();
    private RoomDetailsViewModel roomDetailsViewModel;
    private UserViewModel userViewModel;
    // If you also want to persist room details across navigation, you might add a dedicated ViewModel.
    // For this example, we still use onSaveInstanceState to restore UI fields.

    private static final String API_URL_ROOMS = "http://52.250.54.24:3500/api/room/store";
    private static final String API_URL_MAP_ROOM_USER = "http://52.250.54.24:3500/api/mapRoomUser/store";
    private static final String API_URL_MAP_NODE_USER = "http://52.250.54.24:3500/api/mapNodeUser/store";

    private static final Map<String, String> APPLIANCE_IDS = new HashMap<>();
    private static final String TAG = "RoomDetailsFragment";
    // This will hold the JSON string from ApplianceFragment.
    private String applianceJsonData = null;
    private String roomId = null;
    private boolean isEditMode = false;
    // Keys for saving instance state.
    private static final String KEY_ROOM_NAME = "roomName";
    private static final String KEY_ROOM_DESC = "roomDesc";
    private static final String KEY_LENGTH = "length";
    private static final String KEY_BREADTH = "breadth";
    private static final String KEY_NODE_IDS = "nodeIds";
    private static final String KEY_APPLIANCE_DATA = "applianceData";

    public RoomDetailsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_room_details, container, false);
        roomDetailsViewModel = new ViewModelProvider(requireActivity()).get(RoomDetailsViewModel.class);

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        if (getArguments() != null) {
            roomId = getArguments().getString("roomId");
            isEditMode = getArguments().getBoolean("isEditing", false);
            String roomName = getArguments().getString("roomName", "");

            Log.d(TAG, "Room Details - ID: " + roomId +
                    ", Name: " + roomName +
                    ", Edit Mode: " + isEditMode);
            if (roomId != null) {
                fetchRoomDetails(roomId);
            }
        }
        inputRoomName = view.findViewById(R.id.inputRoomName);
        inputRoomDesc = view.findViewById(R.id.inputRoomDesc);
        inputLength = view.findViewById(R.id.inputLength);
        inputBreadth = view.findViewById(R.id.inputBreadth);
        inputNodeIds = view.findViewById(R.id.inputNodeIds);

        btnAddAppliance = view.findViewById(R.id.btnAddAppliance);
        btnSave = view.findViewById(R.id.btnSave);
        recyclerViewAppliances = view.findViewById(R.id.recyclerViewAppliances);
// Update UI based on edit mode
        if (isEditMode) {
            btnSave.setText("Update Room");
            // If you have a title TextView, you can update it too
            // titleTextView.setText("Edit Room");
        } else {
            btnSave.setText("Save Room");
            // titleTextView.setText("Add New Room");
        }
        // Restore saved values if available.
        if (savedInstanceState != null) {
            inputRoomName.setText(savedInstanceState.getString(KEY_ROOM_NAME, ""));
            inputRoomDesc.setText(savedInstanceState.getString(KEY_ROOM_DESC, ""));
            inputLength.setText(savedInstanceState.getString(KEY_LENGTH, ""));
            inputBreadth.setText(savedInstanceState.getString(KEY_BREADTH, ""));
            inputNodeIds.setText(savedInstanceState.getString(KEY_NODE_IDS, ""));
            applianceJsonData = savedInstanceState.getString(KEY_APPLIANCE_DATA, null);
        }

        // Initialize adapter for appliance JSON display.
        applianceJsonAdapter = new ApplianceJsonAdapter(applianceJsonList);
        recyclerViewAppliances.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewAppliances.setAdapter(applianceJsonAdapter);
        restoreDataFromViewModel();
        btnAddAppliance.setOnClickListener(v -> {
            saveDataToViewModel();
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.action_roomDetailsFragment_to_applianceFragment);
        });

        btnSave.setOnClickListener(v -> saveRoom());

        // Listen for the appliance JSON result from ApplianceFragment.
        getParentFragmentManager().setFragmentResultListener("appliance_data_result", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                applianceJsonData = bundle.getString("appliance_data");
                roomDetailsViewModel.setApplianceJsonData(applianceJsonData);
                Log.d(TAG, "Received appliance data: " + applianceJsonData);
                // Parse and update the RecyclerView.
                if (applianceJsonData != null) {
                    try {
                        JSONObject applianceDataObj = new JSONObject(applianceJsonData);
                        JSONArray appliancesArray = applianceDataObj.optJSONArray("appliances");
                        applianceJsonList.clear();
                        if (appliancesArray != null) {
                            for (int i = 0; i < appliancesArray.length(); i++) {
                                applianceJsonList.add(appliancesArray.getJSONObject(i));
                            }
                        }
                        applianceJsonAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        return view;
    }
    private void fetchRoomDetails(String roomId) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                // Create request body with room ID
                JSONObject requestJson = new JSONObject();
                requestJson.put("_id", roomId);

                RequestBody body = RequestBody.create(
                        MediaType.get("application/json; charset=utf-8"),
                        requestJson.toString()
                );

                // Build the request
                Request request = new Request.Builder()
                        .url("http://52.250.54.24:3500/api/room/show")
                        .post(body)
                        .header("Content-Type", "application/json")
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : null;

                if (responseBody != null) {
                    JSONObject roomData = new JSONObject(responseBody);

                    // Update UI on main thread
                    requireActivity().runOnUiThread(() -> {
                        try {
                            // Update room details
                            inputRoomName.setText(roomData.getString("roomName"));
                            inputRoomDesc.setText(roomData.getString("roomDesc"));
                            inputLength.setText(String.valueOf(roomData.getInt("length")));
                            inputBreadth.setText(String.valueOf(roomData.getInt("breadth")));

                            // Handle node IDs
                            JSONArray nodeIdsArr = roomData.getJSONArray("nodeIdsArr");
                            StringBuilder nodeIdsBuilder = new StringBuilder();
                            for (int i = 0; i < nodeIdsArr.length(); i++) {
                                if (i > 0) nodeIdsBuilder.append(",");
                                nodeIdsBuilder.append(nodeIdsArr.get(i));
                            }
                            inputNodeIds.setText(nodeIdsBuilder.toString());

                            // Handle appliances
                            JSONArray appliances = roomData.getJSONArray("appliances");
                            applianceJsonList.clear();
                            for (int i = 0; i < appliances.length(); i++) {
                                JSONObject appliance = appliances.getJSONObject(i);
                                applianceJsonList.add(appliance);
                            }
                            applianceJsonAdapter.notifyDataSetChanged();

                            // Update the stored appliance data for saving later
                            JSONObject applianceData = new JSONObject();
                            applianceData.put("appliances", appliances);
                            applianceJsonData = applianceData.toString();

                            // Enable/disable fields based on edit mode
                            boolean isEditable = isEditMode;
                            inputRoomName.setEnabled(isEditable);
                            inputRoomDesc.setEnabled(isEditable);
                            inputLength.setEnabled(isEditable);
                            inputBreadth.setEnabled(isEditable);
                            inputNodeIds.setEnabled(isEditable);
                            btnAddAppliance.setEnabled(isEditable);
                            btnSave.setVisibility(isEditable ? View.VISIBLE : View.GONE);

                            Log.d(TAG, "Room details loaded successfully - ID: " + roomId);

                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing room details", e);
                            showToast("Error parsing room details");
                        }
                    });
                } else {
                    Log.e(TAG, "Empty response when fetching room details");
                    requireActivity().runOnUiThread(() ->
                            showToast("Failed to fetch room details")
                    );
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error fetching room details: " + e.getMessage(), e);
                requireActivity().runOnUiThread(() ->
                        showToast("Error fetching room details")
                );
            }
        });
    }
    private void updateRoom() {
        String roomName = inputRoomName.getText().toString().trim();
        String roomDesc = inputRoomDesc.getText().toString().trim();
        String lengthStr = inputLength.getText().toString().trim();
        String breadthStr = inputBreadth.getText().toString().trim();
        String nodeIdsText = inputNodeIds.getText().toString().trim();

        if (roomName.isEmpty() || roomDesc.isEmpty() || lengthStr.isEmpty() || breadthStr.isEmpty() ||
                applianceJsonData == null || applianceJsonData.isEmpty() || nodeIdsText.isEmpty()) {
            showToast("Please fill all fields, add at least one appliance, and enter node IDs");
            return;
        }

        btnSave.setEnabled(false);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                // Create the main JSON object
                JSONObject updateJson = new JSONObject();

                // Add room ID
                updateJson.put("_id", roomId);

                // Add basic room details
                updateJson.put("roomName", roomName);
                updateJson.put("roomDesc", roomDesc);
                updateJson.put("length", Integer.parseInt(lengthStr));
                updateJson.put("breadth", Integer.parseInt(breadthStr));

                // Add nodeIdsArr
                JSONArray nodeIdsArr = new JSONArray();
                String[] nodeIds = nodeIdsText.split(",");
                for (String idStr : nodeIds) {
                    try {
                        nodeIdsArr.put(Integer.parseInt(idStr.trim()));
                    } catch (NumberFormatException nfe) {
                        Log.w(TAG, "Invalid node id: " + idStr);
                    }
                }
                updateJson.put("nodeIdsArr", nodeIdsArr);

                // Add user ID from ViewModel
                String userId = userViewModel.getUserId().getValue();
                if (userId != null) {
                    JSONArray userIdsPresent = new JSONArray();
                    userIdsPresent.put(userId);
                    updateJson.put("userIdsPresent", userIdsPresent);
                }

                // Handle appliances
                if (applianceJsonData != null) {
                    JSONObject applianceDataObj = new JSONObject(applianceJsonData);
                    JSONArray appliances = applianceDataObj.getJSONArray("appliances");
                    updateJson.put("appliances", appliances);
                }

                Log.d(TAG, "Update JSON: " + updateJson.toString(2));

                // Create request
                RequestBody body = RequestBody.create(
                        MediaType.get("application/json; charset=utf-8"),
                        updateJson.toString()
                );

                Request request = new Request.Builder()
                        .url("http://52.250.54.24:3500/api/room/update")
                        .post(body)
                        .header("Content-Type", "application/json")
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : null;

                if (response.isSuccessful() && responseBody != null) {
                    Log.d(TAG, "Update Response: " + responseBody);

                    requireActivity().runOnUiThread(() -> {
                        showToast("Room updated successfully");
                        // Navigate back
                        requireActivity().getSupportFragmentManager().popBackStack();
                    });
                } else {
                    throw new IOException("Unexpected response " + response);
                }

            } catch (JSONException | IOException e) {
                Log.e(TAG, "Error updating room: " + e.getMessage(), e);
                requireActivity().runOnUiThread(() -> {
                    showToast("Error updating room");
                    btnSave.setEnabled(true);
                });
            }
        });
    }
    /**
     * Helper method to show toast messages on the main thread
     */
    private void showToast(String message) {
        requireActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        );
    }
    private void restoreDataFromViewModel() {
        inputRoomName.setText(roomDetailsViewModel.getRoomName());
        inputRoomDesc.setText(roomDetailsViewModel.getRoomDesc());
        inputLength.setText(roomDetailsViewModel.getLength());
        inputBreadth.setText(roomDetailsViewModel.getBreadth());
        inputNodeIds.setText(roomDetailsViewModel.getNodeIds());


    }

    private void saveDataToViewModel() {
        roomDetailsViewModel.setRoomName(inputRoomName.getText().toString());
        roomDetailsViewModel.setRoomDesc(inputRoomDesc.getText().toString());
        roomDetailsViewModel.setLength(inputLength.getText().toString());
        roomDetailsViewModel.setBreadth(inputBreadth.getText().toString());
        roomDetailsViewModel.setNodeIds(inputNodeIds.getText().toString());
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save user-entered values.
        outState.putString(KEY_ROOM_NAME, inputRoomName.getText().toString());
        outState.putString(KEY_ROOM_DESC, inputRoomDesc.getText().toString());
        outState.putString(KEY_LENGTH, inputLength.getText().toString());
        outState.putString(KEY_BREADTH, inputBreadth.getText().toString());
        outState.putString(KEY_NODE_IDS, inputNodeIds.getText().toString());
        outState.putString(KEY_APPLIANCE_DATA, applianceJsonData);
    }

    /**
     * When "Save Room" is clicked we build a RoomModel instance containing all the current details
     * (room name, description, dimensions, node IDs, and appliance data) and then convert that
     * into JSON for the API request.
     */
    private void saveRoom() {
        String roomName = inputRoomName.getText().toString().trim();
        String roomDesc = inputRoomDesc.getText().toString().trim();
        String lengthStr = inputLength.getText().toString().trim();
        String breadthStr = inputBreadth.getText().toString().trim();
        String nodeIdsText = inputNodeIds.getText().toString().trim();

        if (roomName.isEmpty() || roomDesc.isEmpty() || lengthStr.isEmpty() || breadthStr.isEmpty() ||
                applianceJsonData == null || applianceJsonData.isEmpty() || nodeIdsText.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields, add at least one appliance, and enter node IDs", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                // Build the JSON object
                JSONObject roomJson = new JSONObject();

                // Add room ID if in edit mode
                if (isEditMode && roomId != null) {
                    roomJson.put("_id", roomId);
                }

                // Add basic room details
                roomJson.put("roomName", roomName);
                roomJson.put("roomDesc", roomDesc);
                roomJson.put("length", Integer.parseInt(lengthStr));
                roomJson.put("breadth", Integer.parseInt(breadthStr));

                // Parse and add nodeIdsArr
                JSONArray nodeIdsArr = new JSONArray();
                String[] nodeIds = nodeIdsText.split(",");
                for (String idStr : nodeIds) {
                    try {
                        nodeIdsArr.put(Integer.parseInt(idStr.trim()));
                    } catch (NumberFormatException nfe) {
                        Log.w(TAG, "Invalid node id: " + idStr);
                    }
                }
                roomJson.put("nodeIdsArr", nodeIdsArr);

                // Add user ID
                String userId = userViewModel.getUserId().getValue();
                if (userId != null) {
                    JSONArray userIdsPresent = new JSONArray();
                    userIdsPresent.put(userId);
                    roomJson.put("userIdsPresent", userIdsPresent);
                }

                // Handle appliances
                if (applianceJsonData != null) {
                    JSONObject applianceDataObj = new JSONObject(applianceJsonData);
                    JSONArray originalAppliances = applianceDataObj.getJSONArray("appliances");
                    JSONArray finalApplianceArray = new JSONArray();

                    for (int i = 0; i < originalAppliances.length(); i++) {
                        JSONObject applianceObj = originalAppliances.getJSONObject(i);
                        JSONObject finalAppliance = new JSONObject();

                        // Only copy essential appliance fields
                        finalAppliance.put("_id", applianceObj.getString("_id"));
                        finalAppliance.put("applianceName", applianceObj.getString("applianceName"));

                        // Handle appliance prompts
                        JSONArray originalPrompts = applianceObj.getJSONArray("appliancePrompts");
                        JSONArray finalPrompts = new JSONArray();

                        for (int j = 0; j < originalPrompts.length(); j++) {
                            JSONObject prompt = originalPrompts.getJSONObject(j);
                            JSONObject finalPrompt = new JSONObject();

                            // Only copy essential prompt fields
                            finalPrompt.put("name", prompt.getString("name"));
                            finalPrompt.put("description", prompt.optString("description", ""));
                            finalPrompt.put("type", prompt.optString("type", "String"));
                            finalPrompt.put("value", prompt.optString("value", ""));

                            if (prompt.has("valid_range")) {
                                finalPrompt.put("valid_range", prompt.getJSONArray("valid_range"));
                            }

                            finalPrompts.put(finalPrompt);
                        }

                        finalAppliance.put("appliancePrompts", finalPrompts);
                        finalApplianceArray.put(finalAppliance);
                    }

                    roomJson.put("appliances", finalApplianceArray);
                }

                // Remove any unwanted fields that might have come from the original data
                if (roomJson.has("isDeleted")) roomJson.remove("isDeleted");
                if (roomJson.has("createdAt")) roomJson.remove("createdAt");
                if (roomJson.has("updatedAt")) roomJson.remove("updatedAt");
                if (roomJson.has("__v")) roomJson.remove("__v");

                Log.d(TAG, "Final Room JSON: " + roomJson.toString(2));

                // Determine URL based on mode
                String url = isEditMode ?
                        "http://52.250.54.24:3500/api/room/update" :
                        API_URL_ROOMS;

                // Create request
                RequestBody body = RequestBody.create(
                        MediaType.get("application/json; charset=utf-8"),
                        roomJson.toString()
                );

                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .header("Content-Type", "application/json")
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : null;
                Log.d(TAG, "API Response: " + responseBody);

                if (responseBody != null) {
                    JSONObject responseJson = new JSONObject(responseBody);

                    if (!isEditMode && responseJson.has("room")) {
                        // Handle new room creation
                        JSONObject roomData = responseJson.getJSONObject("room");
                        String newRoomId = roomData.getString("_id");

                        if (userId != null) {
                            mapRoomToUser(userId, newRoomId);
                            mapNodeToUser(userId, nodeIdsText);
                        }
                    }

                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(),
                                isEditMode ? "Room updated successfully!" : "Room saved successfully!",
                                Toast.LENGTH_SHORT).show();

                        // Navigate to ApplianceFragment instead of going back
                        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                        navController.navigate(R.id.action_roomDetailsFragment_to_nav_rooms);
                    });
                } else {
                    throw new IOException("Empty response");
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error " + (isEditMode ? "updating" : "saving") + " room: " + e.getMessage(), e);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(),
                            "Error " + (isEditMode ? "updating" : "saving") + " room",
                            Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                });
            }
        });
    }

    private void mapRoomToUser(String userId, String roomId) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                JSONObject requestJson = new JSONObject();
                requestJson.put("userId", userId);
                requestJson.put("roomId", roomId);

                RequestBody body = RequestBody.create(
                        MediaType.get("application/json; charset=utf-8"), requestJson.toString());

                Request request = new Request.Builder()
                        .url(API_URL_MAP_ROOM_USER)
                        .post(body)
                        .header("Content-Type", "application/json")
                        .build();

                Log.d("API_CALL", "Sending request to: " + API_URL_MAP_ROOM_USER);
                Log.d("API_CALL", "Request Body: " + requestJson.toString());

                Response response = client.newCall(request).execute();
                Log.d("API_CALL", "Response Code: " + response.code());
                Log.d("API_CALL", "Response Body: " +
                        (response.body() != null ? response.body().string() : "null"));

            } catch (IOException | JSONException e) {
                Log.e("API_CALL", "Error in mapRoomToUser", e);
            }
        });
    }

    private void mapNodeToUser(String userId, String nodeId) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                JSONObject requestJson = new JSONObject();
                requestJson.put("userId", userId);
                requestJson.put("nodeId", nodeId);

                RequestBody body = RequestBody.create(
                        MediaType.get("application/json; charset=utf-8"), requestJson.toString());

                Request request = new Request.Builder()
                        .url(API_URL_MAP_NODE_USER)
                        .post(body)
                        .header("Content-Type", "application/json")
                        .build();

                Log.d("API_CALL", "Sending request to: " + API_URL_MAP_NODE_USER);
                Log.d("API_CALL", "Request Body: " + requestJson.toString());

                Response response = client.newCall(request).execute();
                Log.d("API_CALL", "Response Code: " + response.code());
                Log.d("API_CALL", "Response Body: " +
                        (response.body() != null ? response.body().string() : "null"));

                if (response.isSuccessful()) {
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getActivity(), "Node mapped successfully!", Toast.LENGTH_SHORT).show()
                        );
                    }
                } else {
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getActivity(), "Failed to map node", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            } catch (IOException | JSONException e) {
                Log.e("API_CALL", "Error in mapNodeToUser", e);
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Error mapping node", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }
}
