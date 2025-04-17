package com.example.ipollusen;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserViewModel extends ViewModel {

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<String> userName = new MutableLiveData<>();
    private final MutableLiveData<String> userEmail = new MutableLiveData<>();
    private final MutableLiveData<String> userId = new MutableLiveData<>();
    public UserViewModel() {
        fetchUserData();
    }

    private void fetchUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            userId.setValue(uid);

            DocumentReference userRef = db.collection("users").document(uid);
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String email = documentSnapshot.getString("email");
                    userEmail.setValue(email);
                }
            });
        }
    }



    public void setUserId(String id) {
        userId.setValue(id);
    }

    public LiveData<String> getUserId() {
        return userId;
    }

    // Save the FCM Token to Firestore
    public void saveFCMToken(String token) {
        String uid = userId.getValue();
        if (uid != null) {
            DocumentReference userRef = db.collection("users").document(uid);
            userRef.update("fcm_token", token).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("UserViewModel", "FCM Token saved successfully.");
                } else {
                    Log.e("UserViewModel", "Failed to save FCM token", task.getException());
                }
            });
        }
    }
    public LiveData<String> getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String email) {
        userEmail.setValue(email);
    }

    public LiveData<String> getUserName() {
        return userName;
    }

    public void setUserName(String name) {
        userName.setValue(name);
    }
}
