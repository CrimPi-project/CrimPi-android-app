package com.almogbb.crimpi.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class UserDataManager {

    private static final String TAG = "UserDataManager";
    private static final String PREFS_NAME = "CrimpiUserPrefs";
    private static final String KEY_BODY_WEIGHT = "bodyWeight";
    private final Context appContext; // Use application context to prevent lea
    public UserDataManager(Context context) {
        // Store the application context to avoid memory leaks
        this.appContext = context.getApplicationContext();
    }

    public void saveBodyWeight(float weight) {
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(KEY_BODY_WEIGHT, weight);
        editor.apply(); // Apply asynchronously
        Log.d(TAG, "Body weight saved: " + weight);
    }

    public float getBodyWeight() {
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        float weight = prefs.getFloat(KEY_BODY_WEIGHT, -1.0f); // -1.0f as a default indicating not set
        Log.d(TAG, "Body weight retrieved: " + weight);
        return weight;
    }
}

