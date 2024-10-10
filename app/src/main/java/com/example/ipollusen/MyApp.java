package com.example.ipollusen;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
public class MyApp extends Application {

    private FirebaseAuth mAuth;

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApplicationId("1:274963849995:android:05734c20be498f7f82f80e")
                .setApiKey("AIzaSyCRPSKeDU4-rpGbGvlLCG61uCQuIuNgKwo")
                .setProjectId("ipollusense-e7c84")
                .build();
        FirebaseApp.initializeApp(this, options, "ipollusen"); // replace with your actual app details
    }
}
