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

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        inputRoomName = view.findViewById(R.id.inputRoomName);
        inputRoomDesc = view.findViewById(R.id.inputRoomDesc);
        inputLength = view.findViewById(R.id.inputLength);
        inputBreadth = view.findViewById(R.id.inputBreadth);
        inputNodeIds = view.findViewById(R.id.inputNodeIds);

        btnAddAppliance = view.findViewById(R.id.btnAddAppliance);
        btnSave = view.findViewById(R.id.btnSave);
        recyclerViewAppliances = view.findViewById(R.id.recyclerViewAppliances);

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

        btnAddAppliance.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.action_roomDetailsFragment_to_applianceFragment);
        });

        btnSave.setOnClickListener(v -> saveRoom());

        // Listen for the appliance JSON result from ApplianceFragment.
        getParentFragmentManager().setFragmentResultListener("appliance_data_result", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                applianceJsonData = bundle.getString("appliance_data");
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

        // Create a RoomModel instance with the current values.
        RoomModel room = new RoomModel("temp", roomName, roomDesc,
                Integer.parseInt(lengthStr), Integer.parseInt(breadthStr),
                nodeIdsText, applianceJsonData);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                // Build the JSON object from the RoomModel.
                JSONObject roomJson = new JSONObject();
                roomJson.put("roomName", room.getRoomName());
                roomJson.put("roomDesc", room.getRoomDesc());
                roomJson.put("length", room.getLength());
                roomJson.put("breadth", room.getBreadth());

                // Parse nodeIdsText (assumed comma-separated numbers).
                JSONArray nodeIdsArr = new JSONArray();
                String[] nodeIds = room.getNodeIds().split(",");
                for (String idStr : nodeIds) {
                    try {
                        nodeIdsArr.put(Integer.parseInt(idStr.trim()));
                    } catch (NumberFormatException nfe) {
                        Log.w(TAG, "Invalid node id: " + idStr);
                    }
                }
                roomJson.put("nodeIdsArr", nodeIdsArr);


                // Build appliances JSON from the appliance data of the RoomModel.
                JSONObject applianceDataObj = new JSONObject(room.getApplianceData());
                JSONArray originalAppliances = applianceDataObj.getJSONArray("appliances");
                JSONArray finalApplianceArray = new JSONArray();

                for (int i = 0; i < originalAppliances.length(); i++) {
                    JSONObject applianceObj = originalAppliances.getJSONObject(i);
                    JSONObject finalAppliance = new JSONObject();
                    finalAppliance.put("_id", applianceObj.getString("_id"));
                    finalAppliance.put("applianceName", applianceObj.getString("applianceName"));

                    JSONArray originalStates = applianceObj.getJSONArray("appliancePrompts");
                    JSONArray finalStates = new JSONArray();

                    for (int j = 0; j < originalStates.length(); j++) {
                        JSONObject prompt = originalStates.getJSONObject(j);
                        JSONObject state = new JSONObject();
                        state.put("name", prompt.getString("name"));
                        state.put("description", prompt.optString("description", ""));
                        state.put("type", prompt.optString("type", "String"));
                        // Use optString so that if "value" isn't present we supply a default (empty string)
                        state.put("value", prompt.optString("value", ""));
                        if (prompt.has("valid_range")) {
                            state.put("valid_range", prompt.getJSONArray("valid_range"));
                        }
                        finalStates.put(state);
                    }
                    finalAppliance.put("appliancePrompts", finalStates);
                    finalApplianceArray.put(finalAppliance);
                }

                roomJson.put("appliances", finalApplianceArray);

                Log.d(TAG, "Final Room JSON: " + roomJson.toString(2));

                RequestBody body = RequestBody.create(
                        MediaType.get("application/json; charset=utf-8"), roomJson.toString());
                Request request = new Request.Builder()
                        .url(API_URL_ROOMS)
                        .post(body)
                        .header("Content-Type", "application/json")
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : null;
                Log.d(TAG, "API Response: " + responseBody);

                if (responseBody != null) {
                    JSONObject responseJson = new JSONObject(responseBody);
                    if (responseJson.has("room")) {
                        JSONObject roomData = responseJson.getJSONObject("room");
                        String roomId = roomData.getString("_id");

                        String userId = userViewModel.getUserId().getValue();
                        if (userId != null) {
                            mapRoomToUser(userId, roomId);
                            mapNodeToUser(userId, nodeIdsText);
                        }

                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Room saved successfully!", Toast.LENGTH_SHORT).show();
                            requireActivity().getSupportFragmentManager().popBackStack();
                        });
                    } else {
                        throw new JSONException("Invalid response format");
                    }
                } else {
                    throw new IOException("Empty response");
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error saving room: " + e.getMessage(), e);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error saving room", Toast.LENGTH_SHORT).show();
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
