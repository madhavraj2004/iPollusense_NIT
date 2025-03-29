package com.example.ipollusen;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RoomsFragment extends Fragment implements RoomAdapter.OnRoomClickListener {
    private RecyclerView recyclerViewRooms;
    private RoomAdapter roomAdapter;
    private List<RoomModel> roomList;
    private UserViewModel userViewModel;
    private ImageView btnAddRoom;

    private static final String API_URL_ROOM_LIST = "http://52.250.54.24:3500/api/mapRoomUser/search";
    private static final String API_URL_ROOMS = "http://52.250.54.24:3500/api/room/";

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final OkHttpClient client = new OkHttpClient();

    public RoomsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rooms, container, false);

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        recyclerViewRooms = view.findViewById(R.id.recyclerViewRooms);
        recyclerViewRooms.setLayoutManager(new LinearLayoutManager(requireContext()));

        roomList = new ArrayList<>();
        roomAdapter = new RoomAdapter(roomList, this);
        recyclerViewRooms.setAdapter(roomAdapter);

        btnAddRoom = view.findViewById(R.id.btnAddRoom);
        btnAddRoom.setOnClickListener(v -> showAddRoomDialog());

        Log.d("RoomsFragment", "onCreateView: Initialized views, fetching user rooms...");
        fetchUserRooms();
        return view;
    }

    // ✅ Fetch rooms linked to the logged-in user
    private void fetchUserRooms() {
        String userId = userViewModel.getUserId().getValue();
        Log.d("RoomsFragment", "Fetching rooms for user ID: " + userId);

        if (userId == null || userId.isEmpty()) {
            showToast("User ID not found");
            return;
        }

        executor.execute(() -> {
            try {
                JSONObject requestJson = new JSONObject();
                requestJson.put("userId", userId);

                RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"), requestJson.toString());
                Request request = new Request.Builder()
                        .url(API_URL_ROOM_LIST)
                        .post(body)
                        .header("Content-Type", "application/json")
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected response " + response);

                String responseBody = response.body() != null ? response.body().string() : null;
                Log.d("RoomsFragment", "User rooms response: " + responseBody);

                if (responseBody != null) {
                    JSONObject responseJson = new JSONObject(responseBody);
                    JSONArray data = responseJson.getJSONArray("data");

                    List<String> roomIds = new ArrayList<>();
                    for (int i = 0; i < data.length(); i++) {
                        roomIds.add(data.getString(i));
                    }

                    Log.d("RoomsFragment", "Fetched Room IDs: " + roomIds);
                    fetchRoomDetails(roomIds);
                }
            } catch (IOException | JSONException e) {
                Log.e("RoomsFragment", "Error fetching rooms", e);
                showToast("Failed to fetch rooms");
            }
        });
    }

    // ✅ Fetch room details based on the room IDs
    private void fetchRoomDetails(List<String> roomIds) {
        Log.d("RoomsFragment", "Fetching room details for IDs: " + roomIds);
        executor.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(API_URL_ROOMS)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected response " + response);

                String responseBody = response.body() != null ? response.body().string() : null;
                Log.d("RoomsFragment", "All rooms response: " + responseBody);

                if (responseBody != null) {
                    JSONArray roomsArray = new JSONArray(responseBody);
                    List<RoomModel> matchedRooms = new ArrayList<>();

                    for (int i = 0; i < roomsArray.length(); i++) {
                        JSONObject roomJson = roomsArray.getJSONObject(i);
                        String roomId = roomJson.getString("_id");

                        if (roomIds.contains(roomId)) {
                            String name = roomJson.getString("roomName");
                            String desc = roomJson.optString("roomDesc", "No description available");
                            matchedRooms.add(new RoomModel(roomId, name, desc));
                        }
                    }

                    requireActivity().runOnUiThread(() -> {
                        roomList.clear();
                        roomList.addAll(matchedRooms);
                        roomAdapter.notifyDataSetChanged();
                        Log.d("RoomsFragment", "Updated UI with fetched room details.");
                    });
                }
            } catch (IOException | JSONException e) {
                Log.e("RoomsFragment", "Error fetching room details", e);
                showToast("Failed to fetch room details");
            }
        });
    }

    @Override
    public void onRoomClick(int position) {
        RoomModel selectedRoom = roomList.get(position);
        Log.d("RoomsFragment", "Room clicked: " + selectedRoom.getRoomName());

        Bundle bundle = new Bundle();
        bundle.putString("roomName", selectedRoom.getRoomName());

        Navigation.findNavController(getView()).navigate(R.id.action_roomsFragment_to_roomDetailsFragment, bundle);
    }

    private void showAddRoomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Room");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_room, null);
        EditText inputRoomName = dialogView.findViewById(R.id.inputRoomName);
        EditText inputRoomDesc = dialogView.findViewById(R.id.inputRoomDesc);
        builder.setView(dialogView);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String roomName = inputRoomName.getText().toString().trim();
            String roomDesc = inputRoomDesc.getText().toString().trim();

            if (!roomName.isEmpty()) {
                RoomModel newRoom = new RoomModel(roomName, roomDesc);
                roomList.add(newRoom);
                roomAdapter.notifyItemInserted(roomList.size() - 1);
                Log.d("RoomsFragment", "Room Added: " + roomName);
                showToast("Room Added");
            } else {
                showToast("Room name cannot be empty");
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    public void onEditRoom(int position) {
        showEditRoomDialog(position);
    }

    @Override
    public void onDeleteRoom(int position) {
        Log.d("RoomsFragment", "Room deleted: " + roomList.get(position).getRoomName());
        roomList.remove(position);
        roomAdapter.notifyItemRemoved(position);
        showToast("Room Deleted");
    }

    private void showEditRoomDialog(int position) {
        RoomModel room = roomList.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Room");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_room, null);
        EditText inputRoomName = dialogView.findViewById(R.id.inputRoomName);
        EditText inputRoomDesc = dialogView.findViewById(R.id.inputRoomDesc);

        inputRoomName.setText(room.getRoomName());
        inputRoomDesc.setText(room.getRoomDesc());
        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            room.setRoomName(inputRoomName.getText().toString().trim());
            room.setRoomDesc(inputRoomDesc.getText().toString().trim());
            roomAdapter.notifyItemChanged(position);
            Log.d("RoomsFragment", "Room updated: " + room.getRoomName());
            showToast("Room Updated");
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
