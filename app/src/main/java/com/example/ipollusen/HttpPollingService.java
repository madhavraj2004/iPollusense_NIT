package com.example.ipollusen;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

// OkHttp imports
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class HttpPollingService {
    private static final String BASE_URL = "http://52.250.54.24:3500/";
    private static final int POLLING_INTERVAL = 10000; // 5 seconds
    private static final int PAGE = 1;
    private static final int LIMIT = 10;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Context context;
    private final TextView statusTextView;
    private final DataCallback dataCallback;
    private String nodeValue; // Replaces MQTT_DEVICE
    private boolean isPolling = false;
    private OkHttpClient client;
    private int retryCount = 0;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY = 2000; // 2 seconds

    public interface DataCallback {
        void onDataReceived(String jsonData);
        void onError(String error);
    }

    public HttpPollingService(Context context, TextView statusTextView, DataCallback callback) {
        this.context = context;
        this.statusTextView = statusTextView;
        this.dataCallback = callback;
        setupHttpClient();
    }

    private void setupHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);

        // Add logging interceptor for debug builds
        if (isDebugBuild()) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logging);
        }

        client = builder.build();
    }

    // Helper method to check if app is in debug mode
    private boolean isDebugBuild() {
        try {
            return context.getApplicationInfo() != null &&
                    (context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {
            return false;
        }
    }

    public void setNodeValue(String nodeValue) {
        this.nodeValue = nodeValue;
    }

    public void startPolling() {
        if (!isPolling && nodeValue != null && !nodeValue.isEmpty()) {
            isPolling = true;
            updateStatus("Starting data polling...");
            pollData();
        } else {
            updateStatus("Cannot start polling: No node value set");
        }
    }

    public void stopPolling() {
        isPolling = false;
        handler.removeCallbacksAndMessages(null);
        updateStatus("Polling stopped");
    }

    private void pollData() {
        if (!isPolling) return;

        if (!isNetworkAvailable()) {
            handleError("No internet connection");
            scheduleRetry();
            return;
        }

        String url = String.format("%sapi/node/?page=%d&limit=%d&nodeValue=%s",
                BASE_URL, PAGE, LIMIT, nodeValue);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(() -> {
                    handleError("Connection error: " + e.getMessage());
                    scheduleRetry();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    handler.post(() -> {
                        handleError("Server error: " + response.code());
                        scheduleRetry();
                    });
                    return;
                }

                String responseData = response.body() != null ? response.body().string() : null;
                if (responseData != null) {
                    handler.post(() -> {
                        retryCount = 0; // Reset retry count on successful response
                        handleSuccess(responseData);
                        // Schedule next poll if still polling
                        if (isPolling) {
                            handler.postDelayed(() -> pollData(), POLLING_INTERVAL);
                        }
                    });
                } else {
                    handler.post(() -> {
                        handleError("Empty response from server");
                        scheduleRetry();
                    });
                }
            }
        });
    }



    private void handleError(String error) {
        updateStatus("Error: " + error);
        dataCallback.onError(error);
    }

    private void scheduleRetry() {
        if (retryCount < MAX_RETRY_ATTEMPTS) {
            retryCount++;
            long delay = RETRY_DELAY * retryCount;
            updateStatus("Retrying in " + (delay/1000) + " seconds... (Attempt " + retryCount + "/" + MAX_RETRY_ATTEMPTS + ")");
            handler.postDelayed(this::pollData, delay);
        } else {
            updateStatus("Max retry attempts reached");
            stopPolling();
        }
    }

    private void updateStatus(String message) {
        if (statusTextView != null) {
            handler.post(() -> statusTextView.setText(message));
        }
        Log.d("HttpPolling", message);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager
                    .getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        }
        return false;
    }
    private void handleSuccess(String responseData) {
        try {
            // Parse the response
            JSONObject fullResponse = new JSONObject(responseData);
            JSONArray dataArray = fullResponse.getJSONArray("data");

            if (dataArray.length() > 0) {
                // Get the most recent data point
                JSONObject latestData = dataArray.getJSONObject(0);
                JSONObject activityData = latestData.getJSONObject("activityData");

                // Create the formatted response
                JSONObject formattedResponse = new JSONObject();

                // Add device_id
                formattedResponse.put("device_id", activityData.getInt("device_id"));

                // Add timestamp
                formattedResponse.put("timestamp", activityData.getString("timestamp"));

                // Add sensor data
                formattedResponse.put("data", activityData.getJSONObject("data"));

                // Add calculated data
                formattedResponse.put("calculated", activityData.getJSONObject("calculated"));

                // Add predicted data
                JSONObject predicted = activityData.getJSONObject("predicted");
                // Convert numeric values to string "O" if needed
                JSONObject formattedPredicted = new JSONObject();
                formattedPredicted.put("aqi_dust", "O");
                formattedPredicted.put("aqi_co", "O");
                formattedResponse.put("predicted", formattedPredicted);

                // Add status
                JSONObject status = activityData.getJSONObject("status");
                // Convert numeric values to string "O"
                JSONObject formattedStatus = new JSONObject();
                formattedStatus.put("dust", "O");
                formattedStatus.put("co", "O");
                formattedResponse.put("status", formattedStatus);

                // Log the formatted data
                Log.d("HttpPolling", "Formatted data: " + formattedResponse.toString());

                // Send the formatted data to callback
                updateStatus("Data received and formatted");
                dataCallback.onDataReceived(formattedResponse.toString());

            } else {
                handleError("No data available in response");
            }

            // Schedule next poll if still polling
            if (isPolling) {
                handler.postDelayed(() -> pollData(), POLLING_INTERVAL);
            }

        } catch (JSONException e) {
            Log.e("HttpPolling", "Error parsing response: " + e.getMessage());
            handleError("Error parsing data: " + e.getMessage());
        }
    }
    private String formatDouble(double value) {
        return String.format(Locale.US, "%.6f", value);
    }
}