package com.almogbb.crimpi.utils;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.almogbb.crimpi.workouts.CustomWorkoutData;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class WorkoutJsonManager {

    private static final String FILE_NAME = "custom_workouts.json";
    private final Context context;
    private final Gson gson;

    public WorkoutJsonManager(Context context) {
        this.context = context;
        // Use GsonBuilder to pretty print JSON for readability during development
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Saves a list of CustomWorkoutData objects to a JSON file.
     * @param workouts The list of workouts to save.
     */
    public void saveWorkouts(List<CustomWorkoutData> workouts) {
        File file = new File(context.getFilesDir(), FILE_NAME);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(workouts, writer);
            System.out.println("Workouts saved to: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error saving workouts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads a list of CustomWorkoutData objects from a JSON file.
     * @return A list of CustomWorkoutData, or an empty list if the file does not exist or an error occurs.
     */
    public List<CustomWorkoutData> loadWorkouts() {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            System.out.println("Workouts file does not exist. Returning empty list.");
            return new ArrayList<>(); // Return empty list if file doesn't exist
        }

        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<CustomWorkoutData>>() {}.getType();
            List<CustomWorkoutData> workouts = gson.fromJson(reader, listType);
            System.out.println("Workouts loaded from: " + file.getAbsolutePath());
            return workouts != null ? workouts : new ArrayList<>(); // Return empty list if parsing results in null
        } catch (IOException e) {
            System.err.println("Error loading workouts: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>(); // Return empty list on error
        }
    }

    /**
     * Clears all saved workouts by deleting the JSON file.
     */
    public void clearWorkouts() {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("Workouts file deleted.");
            } else {
                System.err.println("Failed to delete workouts file.");
            }
        }
    }
}
