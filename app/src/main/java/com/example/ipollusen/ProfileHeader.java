package com.example.ipollusen;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class ProfileHeader extends AppCompatActivity {

    private static final String TAG = "ProfileHeader";

    private TextView greetingTextView, emailTextView, userIdTextView;
    private ImageView profileImageView;
    private ImageButton backArrowButton;
    private UserViewModel userViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nav_header_main);  // Ensure this matches your layout

        // Initialize UI elements
        greetingTextView = findViewById(R.id.greetingTextView);
        emailTextView = findViewById(R.id.emailTextView);
        userIdTextView = findViewById(R.id.userIdTextView);
        profileImageView = findViewById(R.id.profileImageView);
        backArrowButton = findViewById(R.id.backArrowButton);

        // Get email from UserViewModel
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        String userEmail = userViewModel.getUserEmail().getValue();

        if (userEmail != null && !userEmail.isEmpty()) {
            fetchUserDetails(userEmail);
        }

        // Handle back button click (Close the activity)
        backArrowButton.setOnClickListener(v -> finish());

        // Handle profile image click (Re-fetch user details)
        profileImageView.setOnClickListener(v -> fetchUserDetails(userEmail));
    }

    public void fetchUserDetails(String email) {
        new Thread(() -> {
            try {
                // Step 1: Fetch User by Email (Fixed email for API)
                JSONObject emailRequest = new JSONObject();
                emailRequest.put("email", "raj.madhav2004@gmail.com"); // Hardcoded email as per requirement

                URL url = new URL("http://52.250.54.24:3500/api/users/search");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(emailRequest.toString().getBytes());
                os.flush();
                os.close();

                Scanner scanner = new Scanner(conn.getInputStream());
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();

                JSONObject jsonResponse = new JSONObject(response);
                JSONObject userData = jsonResponse.getJSONObject("data");

                String userId = userData.getString("_id");
                String userName = userData.getString("name");
                String userEmail = userData.getString("email");

                // Step 2: Fetch additional user details using _id
                JSONObject idRequest = new JSONObject();
                idRequest.put("_id", userId);

                URL idUrl = new URL("http://52.250.54.24:3500/api/users/show");
                HttpURLConnection idConn = (HttpURLConnection) idUrl.openConnection();
                idConn.setRequestMethod("POST");
                idConn.setRequestProperty("Content-Type", "application/json");
                idConn.setDoOutput(true);

                OutputStream osId = idConn.getOutputStream();
                osId.write(idRequest.toString().getBytes());
                osId.flush();
                osId.close();

                Scanner idScanner = new Scanner(idConn.getInputStream());
                String idResponse = idScanner.useDelimiter("\\A").next();
                idScanner.close();

                JSONObject idJsonResponse = new JSONObject(idResponse);
                JSONObject fullUserData = idJsonResponse.getJSONObject("data");

                String profileImageUrl = fullUserData.optString("profileImage", "");

                runOnUiThread(() -> {
                    greetingTextView.setText("Hi, " + userName);
                    emailTextView.setText(userEmail);
                    userIdTextView.setText("User ID: " + userId);

                    if (!profileImageUrl.isEmpty()) {
                        Glide.with(ProfileHeader.this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_person)
                                .into(profileImageView);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error fetching user details", e);
            }
        }).start();
    }
}
