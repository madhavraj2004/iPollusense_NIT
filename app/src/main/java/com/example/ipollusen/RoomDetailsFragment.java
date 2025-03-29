package com.example.ipollusen;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RoomDetailsFragment extends Fragment {
    private EditText inputRoomName, inputRoomDesc, inputLength, inputBreadth, inputNodeIds;
    private Button btnAddAppliance, btnSave;
    private RecyclerView recyclerViewAppliances;
    private ApplianceAdapter applianceAdapter;
    private List<Appliance> appliancesList;
    private UserViewModel userViewModel;

    private static final String API_URL_ROOMS = "http://52.250.54.24:3500/api/room/store";
    private static final String API_URL_MAP_ROOM_USER = "http://52.250.54.24:3500/api/mapRoomUser/store";
    private static final String API_URL_MAP_NODE_USER = "http://52.250.54.24:3500/api/mapNodeUser/store";

    private static final Map<String, String> APPLIANCE_IDS = new HashMap<>();

    static {
        APPLIANCE_IDS.put("Fan", "67e414422ea35fe39a7d7cd0");
        APPLIANCE_IDS.put("Window", "67e414532ea35fe39a7d7cd4");
    }

    public RoomDetailsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_room_details, container, false);

        // Initialize ViewModel
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        // Initialize input fields
        inputRoomName = view.findViewById(R.id.inputRoomName);
        inputRoomDesc = view.findViewById(R.id.inputRoomDesc);
        inputLength = view.findViewById(R.id.inputLength);
        inputBreadth = view.findViewById(R.id.inputBreadth);
        inputNodeIds = view.findViewById(R.id.inputNodeIds); // New field for node IDs

        btnAddAppliance = view.findViewById(R.id.btnAddAppliance);
        btnSave = view.findViewById(R.id.btnSave);
        recyclerViewAppliances = view.findViewById(R.id.recyclerViewAppliances);

        appliancesList = new ArrayList<>();
        applianceAdapter = new ApplianceAdapter(appliancesList);
        recyclerViewAppliances.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewAppliances.setAdapter(applianceAdapter);

        btnAddAppliance.setOnClickListener(v -> showAddApplianceDialog());
        btnSave.setOnClickListener(v -> saveRoom());

        return view;
    }

    private void showAddApplianceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_appliance, null);
        builder.setView(dialogView);

        Spinner spinnerApplianceType = dialogView.findViewById(R.id.spinnerApplianceType);
        Button btnConfirmAdd = dialogView.findViewById(R.id.btnConfirmAdd);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.appliance_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerApplianceType.setAdapter(adapter);

        AlertDialog dialog = builder.create();

        btnConfirmAdd.setOnClickListener(v -> {
            String selectedAppliance = spinnerApplianceType.getSelectedItem().toString();
            String applianceId = APPLIANCE_IDS.get(selectedAppliance);

            if (applianceId != null) {
                appliancesList.add(new Appliance(applianceId, selectedAppliance, "OFF", true, false));
                applianceAdapter.notifyDataSetChanged();
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Invalid appliance selection", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void saveRoom() {
        String roomName = inputRoomName.getText().toString().trim();
        String roomDesc = inputRoomDesc.getText().toString().trim();
        String length = inputLength.getText().toString().trim();
        String breadth = inputBreadth.getText().toString().trim();
        String nodeIdsText = inputNodeIds.getText().toString().trim(); // New input for node IDs

        if (roomName.isEmpty() || roomDesc.isEmpty() || length.isEmpty() || breadth.isEmpty() ||
                appliancesList.isEmpty() || nodeIdsText.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields, add at least one appliance, and enter node IDs", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false); // Disable button to prevent multiple clicks

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                JSONObject roomJson = new JSONObject();
                roomJson.put("roomName", roomName);
                roomJson.put("roomDesc", roomDesc);
                roomJson.put("length", Integer.parseInt(length));
                roomJson.put("breadth", Integer.parseInt(breadth));

                JSONArray appliancesArray = new JSONArray();
                for (Appliance appliance : appliancesList) {
                    JSONObject applianceJson = new JSONObject();
                    applianceJson.put("appliance_id", appliance.getApplianceId());
                    applianceJson.put("is_smart", true);
                    appliancesArray.put(applianceJson);
                }
                roomJson.put("appliances", appliancesArray);
                roomJson.put("nodeIdsArr", new JSONArray()); // Can be adjusted if needed
                roomJson.put("userIdsPresent", new JSONArray());
                roomJson.put("isDeleted", false);

                RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"), roomJson.toString());
                Request request = new Request.Builder()
                        .url(API_URL_ROOMS)
                        .post(body)
                        .header("Content-Type", "application/json")
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : null;

                if (responseBody != null) {
                    JSONObject responseJson = new JSONObject(responseBody);
                    if (responseJson.has("room")) {
                        JSONObject roomData = responseJson.getJSONObject("room");
                        String roomId = roomData.getString("_id");

                        // Map room to user
                        String userId = userViewModel.getUserId().getValue();
                        if (userId != null) {
                            mapRoomToUser(userId, roomId);
                        }

                        // Map node(s) to user. For this sample, we assume a single node ID.
                        // If nodeIdsText contains multiple IDs (comma-separated), you can split and iterate.
                        try {
                            int nodeId = Integer.parseInt(nodeIdsText);
                            if (userId != null) {
                                mapNodeToUser(userId, nodeId);
                            }
                        } catch (NumberFormatException e) {
                            // Handle invalid node id format
                            e.printStackTrace();
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
                e.printStackTrace();
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

                RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"), requestJson.toString());
                Request request = new Request.Builder()
                        .url(API_URL_MAP_ROOM_USER)
                        .post(body)
                        .header("Content-Type", "application/json")
                        .build();

                client.newCall(request).execute();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private void mapNodeToUser(String userId, int nodeId) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                JSONObject requestJson = new JSONObject();
                requestJson.put("userId", userId);
                requestJson.put("nodeId", nodeId);

                RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"), requestJson.toString());
                Request request = new Request.Builder()
                        .url(API_URL_MAP_NODE_USER)
                        .post(body)
                        .header("Content-Type", "application/json")
                        .build();

                client.newCall(request).execute();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        });
    }
}
