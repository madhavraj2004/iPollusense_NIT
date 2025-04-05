package com.example.ipollusen;

import static com.example.ipollusen.RoomsFragment.API_URL_ROOMS;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
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

public class RecommendationFragment extends Fragment {
    private RecyclerView recyclerView;
    private RoomAdapter roomAdapter;
    private List<RoomModel> roomList;
    private UserViewModel userViewModel;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final OkHttpClient client = new OkHttpClient();
    private boolean isFetching = false;
    private static final String TAG = "RecommendationFragment";

    private static final String API_URL_ROOM_LIST = "http://52.250.54.24:3500/api/mapRoomUser/search";
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recommendation, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewRecommendations);
        roomList = new ArrayList<>();

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);


        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        roomAdapter = new RoomAdapter(roomList, position -> openRecommendationPrompt(roomList.get(position)));
        recyclerView.setAdapter(roomAdapter);
        fetchUserRooms();
        return view;
    }

    private void openRecommendationPrompt(RoomModel room) {
        RecommendationPromptFragment promptFragment = RecommendationPromptFragment.newInstance(
                room.getRoomId(),
                room.getRoomName(),
                room.getRoomDesc(),
                room.getLength(),
                room.getBreadth(),
                room.getNodeIds()
        );

        // Replace current fragment with RecommendationPromptFragment
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment_activity_main, promptFragment) // Make sure this ID matches your container
                .addToBackStack(null)
                .commit();
    }


    // Fetch room mapping for the logged-in user (returns matching room IDs)
    private void fetchUserRooms() {
        String userId = userViewModel.getUserId().getValue();
        if (userId == null || userId.isEmpty()) {
            showToast("User ID not found");
            return;
        }

        executorService.execute(() -> {
            try {
                JSONObject requestJson = new JSONObject();
                requestJson.put("userId", userId);

                RequestBody body = RequestBody.create(
                        MediaType.get("application/json; charset=utf-8"),
                        requestJson.toString());

                Request request = new Request.Builder()
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
            }
        });
    }

    // Fetch all room details, filter based on mapped room IDs
    private void fetchRoomDetails(List<String> roomIds) {
        executorService.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(API_URL_ROOMS)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }

                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "All rooms response: " + responseBody);

                    List<RoomModel> matchedRooms = new ArrayList<>();
                    JSONArray roomsArray = new JSONArray(responseBody);

                    for (int i = 0; i < roomsArray.length(); i++) {
                        JSONObject roomJson = roomsArray.getJSONObject(i);
                        String roomId = roomJson.getString("_id");

                        if (roomIds.contains(roomId)) {
                            String roomName = roomJson.getString("roomName");
                            String roomDesc = roomJson.optString("roomDesc", "No description available");
                            int length = roomJson.getInt("length");
                            int breadth = roomJson.getInt("breadth");

                            matchedRooms.add(new RoomModel(roomId, roomName, roomDesc, length, breadth));
                        }
                    }

                    requireActivity().runOnUiThread(() -> {
                        roomList.clear();
                        roomList.addAll(matchedRooms);
                        roomAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Updated UI with matched room details.");
                    });
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error fetching room details", e);
                showToast("Failed to fetch room details");
            }
        });
    }

    // Display toast on UI thread
    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        );
    }
}
