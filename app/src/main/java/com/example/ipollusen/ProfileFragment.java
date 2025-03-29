package com.example.ipollusen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView nameTextView, emailTextView, ageTextView, genderTextView, ethnicityTextView, otherInfoTextView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        nameTextView = view.findViewById(R.id.nameTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        ageTextView = view.findViewById(R.id.ageTextView);
        genderTextView = view.findViewById(R.id.genderTextView);
        ethnicityTextView = view.findViewById(R.id.ethnicityTextView);
        otherInfoTextView = view.findViewById(R.id.otherInfoTextView);

        // Initialize Firebase Auth & Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserData();

        return view;
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            // Reference the document in "users" collection
            DocumentReference userRef = db.collection("users").document(uid);
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Get each field and set it in the UI
                    String name = documentSnapshot.getString("name");
                    String email = documentSnapshot.getString("email");
                    Long ageLong = documentSnapshot.getLong("age");
                    String gender = documentSnapshot.getString("gender");
                    String ethnicity = documentSnapshot.getString("ethnicity");
                    String otherInfo = documentSnapshot.getString("otherInfo");

                    nameTextView.setText("Name: " + (name != null ? name : ""));
                    emailTextView.setText("Email: " + (email != null ? email : ""));
                    ageTextView.setText("Age: " + (ageLong != null ? ageLong.toString() : ""));
                    genderTextView.setText("Gender: " + (gender != null ? gender : ""));
                    ethnicityTextView.setText("Ethnicity: " + (ethnicity != null ? ethnicity : ""));
                    otherInfoTextView.setText("Other Info: " + (otherInfo != null ? otherInfo : ""));
                } else {
                    Toast.makeText(getActivity(), "User details not found", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e ->
                    Toast.makeText(getActivity(), "Failed to load user data", Toast.LENGTH_SHORT).show());
        }
    }
}
