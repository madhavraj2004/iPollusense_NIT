package com.example.ipollusen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recommendation, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewRecommendations);
        roomList = new ArrayList<>();

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        userViewModel.getUserId().observe(getViewLifecycleOwner(), userId -> {
            if (!isFetching) {
                isFetching = true;
                fetchRoomIds(userId);
            }
        });

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        roomAdapter = new RoomAdapter(roomList, position -> openRecommendationPrompt(roomList.get(position)));
        recyclerView.setAdapter(roomAdapter);

        return view;
    }

    private void openRecommendationPrompt(RoomModel room) {
        RecommendationPromptFragment promptFragment = RecommendationPromptFragment.newInstance(
                room.getRoomId(),
                room.getRoomName(),
                room.getRoomDesc()
        );

        promptFragment.show(getParentFragmentManager(), "RecommendationPromptFragment");
    }

    private void fetchRoomIds(String userId) {
        Log.d("RecommendationFragment", "Fetching room IDs for userId: " + userId);
        executorService.execute(() -> {
            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("userId", userId);

                Request request = new Request.Builder()
                        .url("http://52.250.54.24:3500/api/mapRoomUser/search")
                        .post(RequestBody.create(requestBody.toString(), MediaType.get("application/json")))
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Log.d("RecommendationFragment", "Room IDs Response: " + responseData);
                    parseRoomIds(responseData);
                } else {
                    Log.e("RecommendationFragment", "Failed to fetch room IDs: " + response.code());
                }
            } catch (IOException | JSONException e) {
                Log.e("RecommendationFragment", "Error fetching room IDs", e);
            }
        });
    }

    private void parseRoomIds(String json) {
        Log.d("RecommendationFragment", "Parsing room IDs from JSON: " + json);
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray dataArray = jsonObject.optJSONArray("data");

            if (dataArray == null || dataArray.length() == 0) {
                Log.w("RecommendationFragment", "No rooms found.");
                return;
            }

            List<String> roomIds = new ArrayList<>();
            for (int i = 0; i < dataArray.length(); i++) {
                roomIds.add(dataArray.getJSONObject(i).optString("roomId", ""));
            }

            fetchAllRoomDetails(roomIds);
        } catch (JSONException e) {
            Log.e("RecommendationFragment", "JSON Parsing Error", e);
        }
    }

    private void fetchAllRoomDetails(List<String> roomIds) {
        executorService.execute(() -> {
            List<RoomModel> newRooms = new ArrayList<>();

            for (String roomId : roomIds) {
                if (roomId.isEmpty()) {
                    continue;
                }

                try {
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("_id", roomId);

                    Request request = new Request.Builder()
                            .url("http://52.250.54.24:3500/api/room/show")
                            .post(RequestBody.create(requestBody.toString(), MediaType.get("application/json")))
                            .build();

                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        Log.d("RecommendationFragment", "Room Details Response for " + roomId + ": " + responseData);
                        RoomModel room = parseRoomDetails(responseData);
                        if (room != null) {
                            newRooms.add(room);
                        }
                    } else {
                        Log.e("RecommendationFragment", "Failed to fetch room details: " + response.code());
                    }
                } catch (IOException | JSONException e) {
                    Log.e("RecommendationFragment", "Error fetching room details", e);
                }
            }

            if (!newRooms.isEmpty()) {
                requireActivity().runOnUiThread(() -> {
                    roomList.clear();
                    roomList.addAll(newRooms);
                    roomAdapter.notifyDataSetChanged();
                    isFetching = false;
                });
            } else {
                isFetching = false;
            }
        });
    }

    private RoomModel parseRoomDetails(String json) {
        Log.d("RecommendationFragment", "Parsing room details: " + json);
        try {
            JSONObject jsonObject = new JSONObject(json);

            String roomId = jsonObject.optString("_id", "");
            String roomName = jsonObject.optString("roomName", "Unnamed Room");
            String roomDesc = jsonObject.optString("roomDesc", "No description available");

            if (roomId.isEmpty() || roomName.equals("Unnamed Room")) {
                Log.w("RecommendationFragment", "Invalid room details received.");
                return null;
            }

            return new RoomModel(roomId, roomName, roomDesc);
        } catch (JSONException e) {
            Log.e("RecommendationFragment", "JSON Parsing Error", e);
            return null;
        }
    }
}
