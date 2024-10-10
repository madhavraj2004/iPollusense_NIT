package com.example.ipollusen;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MyApplication extends Application {
    private static MyApplication instance;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth auth;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initializeFirebase();
        initializeSharedPreferences();
    }

    private void initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this);
            auth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the case where FirebaseApp could not be initialized properly
        }
    }

    private void initializeSharedPreferences() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public static MyApplication getInstance() {
        return instance;
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public void clearUserData() {
        sharedPreferences.edit().clear().apply();
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser(); // Get current user on demand
    }

    public void signOut() {
        if (auth != null) {
            auth.signOut();
        }
    }
}
