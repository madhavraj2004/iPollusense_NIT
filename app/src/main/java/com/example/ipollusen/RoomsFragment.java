package com.example.ipollusen;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
    // RecyclerViews for two display formats
    private RecyclerView recyclerViewRooms;       // List view using RoomAdapter & RoomModel
//    private RecyclerView recyclerViewRoomCards;     // Card view using RoomCardAdapter & RoomCardModel

    // Adapters and data lists
    private RoomAdapter roomAdapter;
    private List<RoomModel> roomList;
    //private RoomCardAdapter roomCardAdapter;
    //private List<RoomCardModel> roomCardList;

    // Other views
    private ImageView btnAddRoom;
    private UserViewModel userViewModel;

    // API endpoints
    private static final String API_URL_ROOM_LIST = "http://52.250.54.24:3500/api/mapRoomUser/search";
    private static final String API_URL_ROOMS = "http://52.250.54.24:3500/api/room/";

    // Networking
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final OkHttpClient client = new OkHttpClient();

    public RoomsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rooms, container, false);

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        // Initialize list view RecyclerView and adapter
        recyclerViewRooms = view.findViewById(R.id.recyclerViewRooms);
        recyclerViewRooms.setLayoutManager(new LinearLayoutManager(requireContext()));
        roomList = new ArrayList<>();
        roomAdapter = new RoomAdapter(roomList, this);
        recyclerViewRooms.setAdapter(roomAdapter);

        // Initialize card view RecyclerView and adapter
        //recyclerViewRoomCards = view.findViewById(R.id.recyclerViewRoomCards);
        //recyclerViewRoomCards.setLayoutManager(new LinearLayoutManager(requireContext()));
        //roomCardList = new ArrayList<>();
        //roomCardAdapter = new RoomCardAdapter(roomCardList);
        //recyclerViewRoomCards.setAdapter(roomCardAdapter);

        btnAddRoom = view.findViewById(R.id.btnAddRoom);
        //btnAddRoom.setOnClickListener(v -> showAddRoomDialog());
        btnAddRoom.setOnClickListener(v -> {
            Log.d("RoomsFragment", "Navigating to RoomDetailsFragment");
            Navigation.findNavController(v).navigate(R.id.action_roomsFragment_to_roomDetailsFragment);
        });
        Log.d("RoomsFragment", "Initialized views, fetching user rooms...");
        fetchUserRooms();

        return view;
    }

    // Fetch room mapping for the logged-in user (returns matching room IDs)
    private void fetchUserRooms() {
        String userId = userViewModel.getUserId().getValue();
        if (userId == null || userId.isEmpty()) {
            showToast("User ID not found");
            return;
        }

        executor.execute(() -> {
            try {
                JSONObject requestJson = new JSONObject();
                requestJson.put("userId", userId);

                RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"),
                        requestJson.toString());
                Request request = new Request.Builder()
                        .url(API_URL_ROOM_LIST)
                        .post(body)
                        .header("Content-Type", "application/json")
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful())
                    throw new IOException("Unexpected response " + response);

                String responseBody = response.body() != null ? response.body().string() : null;
                Log.d("RoomsFragment", "User rooms mapping response: " + responseBody);

                if (responseBody != null) {
                    JSONObject responseJson = new JSONObject(responseBody);
                    JSONArray dataArray = responseJson.getJSONArray("data");

                    List<String> roomIds = new ArrayList<>();
                    for (int i = 0; i < dataArray.length(); i++) {
                        // Each mapping object contains a "roomId"
                        JSONObject mappingObj = dataArray.getJSONObject(i);
                        roomIds.add(mappingObj.getString("roomId"));
                    }

                    Log.d("RoomsFragment", "Fetched Room IDs: " + roomIds);
                    fetchRoomDetails(roomIds);
                }
            } catch (IOException | JSONException e) {
                Log.e("RoomsFragment", "Error fetching room mapping", e);
                showToast("Failed to fetch rooms");
            }
        });
    }

    // Fetch all room details and update both the list view and card view data lists
    private void fetchRoomDetails(List<String> roomIds) {
        executor.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(API_URL_ROOMS)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful())
                    throw new IOException("Unexpected response " + response);

                String responseBody = response.body() != null ? response.body().string() : null;
                Log.d("RoomsFragment", "All rooms response: " + responseBody);

                if (responseBody != null) {
                    JSONArray roomsArray = new JSONArray(responseBody);
                    List<RoomModel> matchedRooms = new ArrayList<>();
                    //List<RoomCardModel> matchedCardRooms = new ArrayList<>();

                    for (int i = 0; i < roomsArray.length(); i++) {
                        JSONObject roomJson = roomsArray.getJSONObject(i);
                        String roomId = roomJson.getString("_id");

                        // Only add rooms that are mapped to the user
                        if (roomIds.contains(roomId)) {
                            String roomName = roomJson.getString("roomName");
                            String roomDesc = roomJson.optString("roomDesc", "No description available");
                            int length = roomJson.getInt("length");
                            int breadth = roomJson.getInt("breadth");

                            // For this example, nodeIds is not extracted from JSON so we pass an empty string.
                            String nodeIds = "";

                            matchedRooms.add(new RoomModel(roomId, roomName, roomDesc, length, breadth));
                            //matchedCardRooms.add(new RoomCardModel(roomId, roomName, roomDesc, length, breadth, nodeIds));
                        }
                    }

                    requireActivity().runOnUiThread(() -> {
                        roomList.clear();
                        roomList.addAll(matchedRooms);
                        roomAdapter.notifyDataSetChanged();

//                        roomCardList.clear();
//                        roomCardList.addAll(matchedCardRooms);
//                        roomCardAdapter.notifyDataSetChanged();

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
        // Handle click event from the list view adapter
        RoomModel selectedRoom = roomList.get(position);
        Log.d("RoomsFragment", "Room clicked: " + selectedRoom.getRoomName());

        Bundle bundle = new Bundle();
        bundle.putString("roomName", selectedRoom.getRoomName());
        Navigation.findNavController(getView())
                .navigate(R.id.action_roomsFragment_to_roomDetailsFragment, bundle);
    }



    @Override
    public void onEditRoom(int position) {
        showEditRoomDialog(position);
    }

    @Override
    public void onDeleteRoom(int position) {
        // Get the room ID before removal
        String roomId = roomList.get(position).getRoomId();
        Log.d("RoomsFragment", "Room deleted: " + roomList.get(position).getRoomName());
        roomList.remove(position);
        roomAdapter.notifyItemRemoved(position);

        // Remove corresponding RoomCardModel from the card view list
//        for (int i = 0; i < roomCardList.size(); i++) {
//            if (roomCardList.get(i).getRoomId().equals(roomId)) {
//                roomCardList.remove(i);
//                roomCardAdapter.notifyItemRemoved(i);
//                break;
//            }
//        }
        showToast("Room Deleted");
    }

    private void showEditRoomDialog(int position) {
        RoomModel room = roomList.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Room");


        // Also update the corresponding card model
//        for (int i = 0; i < roomCardList.size(); i++) {
//            if (roomCardList.get(i).getRoomId().equals(room.getRoomId())) {
//                roomCardList.get(i).setRoomName(room.getRoomName());
//                roomCardList.get(i).setRoomDesc(room.getRoomDesc());
//                roomCardAdapter.notifyItemChanged(i);
//                break;
//            }
//        }

        Log.d("RoomsFragment", "Room updated: " + room.getRoomName());
        showToast("Room Updated");
    }





    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show()
        );
    }

}
