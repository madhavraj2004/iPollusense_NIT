package com.example.ipollusen;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SharedResponseViewModel extends ViewModel {

    private static final String TAG = "SharedResponseVM";

    // LiveData holding responses mapped by mapRoomUserId
    private final MutableLiveData<Map<String, String>> responseMapLiveData;

    public SharedResponseViewModel() {
        responseMapLiveData = new MutableLiveData<>(new HashMap<>());
    }

    // Expose LiveData to observers (read-only)
    public LiveData<Map<String, String>> getResponseMapLiveData() {
        return responseMapLiveData;
    }

    // Get a specific response by mapRoomUserId
    public String getResponse(String mapRoomUserId) {
        Map<String, String> currentMap = responseMapLiveData.getValue();
        if (currentMap != null) {
            return currentMap.get(mapRoomUserId);
        }
        return null;
    }
    private void logLongString(String tag, String message) {
        int maxLogSize = 1000;
        for (int i = 0; i <= message.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = Math.min((i + 1) * maxLogSize, message.length());
            Log.d(tag, message.substring(start, end));  // ✅ Use Log.d, not call method again
        }
    }

    // Add or update response for the given key
    public void addResponse(String mapRoomUserId, String response) {
        logLongString(TAG, "Adding/Updating response for: " + mapRoomUserId);
        logLongString(TAG, "Response: " + response);

        Map<String, String> currentMap = responseMapLiveData.getValue();
        if (currentMap == null) {
            currentMap = new HashMap<>();
        }

        currentMap.put(mapRoomUserId, response);

        // Logging updated map
        for (Map.Entry<String, String> entry : currentMap.entrySet()) {
            logLongString(TAG, "Map Entry => Key: " + entry.getKey() + ", Value: " + entry.getValue());
        }

        // Update LiveData
        responseMapLiveData.setValue(new HashMap<>(currentMap)); // Defensive copy
    }

    // Optionally expose an immutable copy of the map
    public Map<String, String> getResponseMapCopy() {
        Map<String, String> map = responseMapLiveData.getValue();
        return map != null ? Collections.unmodifiableMap(new HashMap<>(map)) : Collections.emptyMap();
    }
}
