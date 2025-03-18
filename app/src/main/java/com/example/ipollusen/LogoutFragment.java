package com.example.ipollusen;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.example.ipollusen.R;
public class LogoutFragment extends Fragment {

    private Button logoutButton;
    private static final String TAG = "LogoutFragment";
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_logout, container, false);
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        logoutButton = view.findViewById(R.id.logoutButton);

        // Set click listener for the logout button
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Logout button clicked");
                confirmLogout();  // Confirm before logging out
            }
        });

        return view;  // Return the view
    }

    private void confirmLogout() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("No", null)
                .show();
    }

    private void logout() {
        // Sign out from Firebase
        mAuth.signOut();

        // Clear shared preferences
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();  // Clear all stored data
        editor.apply();  // Apply changes

        // Redirect to LoginActivity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear activity stack
        startActivity(intent);
        requireActivity().finish();  // Close the activity
    }
}
