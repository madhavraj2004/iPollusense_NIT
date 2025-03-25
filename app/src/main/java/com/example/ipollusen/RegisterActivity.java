package com.example.ipollusen;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ipollusen.databinding.ActivityRegisterBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);

        // Initialize View Binding
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth & Firestore
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Setup Gender Dropdown
        setupGenderDropdown();

        // Set Click Listeners
        binding.buttonRegister.setOnClickListener(v -> registerUser());
        binding.buttonLogin.setOnClickListener(v -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));
    }

    private void setupGenderDropdown() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerGender.setAdapter(adapter);
    }

    private void registerUser() {
        String name = binding.editTextName.getText().toString().trim();
        String email = binding.editTextEmail.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();
        String ageStr = binding.editTextAge.getText().toString().trim();
        String gender = binding.spinnerGender.getSelectedItem().toString();
        String ethnicity = binding.editTextEthnicity.getText().toString().trim();
        String otherInfo = binding.editTextOtherInfo.getText().toString().trim();

        if (TextUtils.isEmpty(otherInfo)) {
            otherInfo = ""; // Ensure "otherInfo" is never null
        }

        // Validate Required Fields
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) ||
                TextUtils.isEmpty(ageStr) || TextUtils.isEmpty(ethnicity)) {
            Toast.makeText(this, "All fields except 'Other Info' are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (gender.equals("Gender")) { // Prevent default value from being sent
            Toast.makeText(this, "Please select a valid gender", Toast.LENGTH_SHORT).show();
            return;
        }

        final int age = Integer.parseInt(ageStr);

        // Register user with Firebase Authentication
        String finalOtherInfo = otherInfo;
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();

                        // Create User Data Map for Firestore
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("name", name);
                        userMap.put("email", email);
                        userMap.put("age", age);
                        userMap.put("gender", gender);
                        userMap.put("ethnicity", ethnicity);
                        userMap.put("otherInfo", finalOtherInfo);

                        // Save user to Firestore
                        firestore.collection("users").document(userId)
                                .set(userMap)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        // Send Data to API
                                        sendUserDataToAPI(name, email, age, gender, ethnicity, finalOtherInfo);
                                    } else {
                                        Toast.makeText(RegisterActivity.this, "Failed to save user data.", Toast.LENGTH_SHORT).show();
                                    }
                                });

                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void sendUserDataToAPI(String name, String email, int age, String gender, String ethnicity, String otherInfo) {
        String url = "http://52.250.54.24:3500/api/users/store";

        OkHttpClient client = new OkHttpClient();

        // Create JSON Request Body
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("name", name);
            jsonBody.put("email", email);
            jsonBody.put("age", age);
            jsonBody.put("gender", gender);
            jsonBody.put("ethnicity", ethnicity);
            jsonBody.put("other_info", otherInfo);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        // Execute the Request in a Background Thread
        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "User successfully sent to API.");
                } else {
                    Log.e(TAG, "Failed to send user to API: " + response.message());
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception while sending data to API", e);
            }
        }).start();
    }


}
