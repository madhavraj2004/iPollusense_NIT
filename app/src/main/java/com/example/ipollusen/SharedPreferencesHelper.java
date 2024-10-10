package com.example.ipollusen;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesHelper {

    private static final String PREFS_NAME = "iPolluSensePrefs";
    private static final String KEY_USER_ID = "USER_ID";

    private SharedPreferences sharedPreferences;

    public SharedPreferencesHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveUserId(String userId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_ID, userId);
        editor.apply(); // or commit()
    }

    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, "");
    }
}
