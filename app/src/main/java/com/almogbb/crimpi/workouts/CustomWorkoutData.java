package com.almogbb.crimpi.workouts;

import java.io.Serializable; // Import Serializable
import java.util.List;

// This data class will be used for JSON serialization/deserialization.
// It now implements Serializable to be passed via Bundle.
public class CustomWorkoutData implements Serializable { // ADDED: implements Serializable
    private String id; // Unique ID for the workout
    private String name;
    private String description;
    private int totalDurationSeconds; // Calculated total duration for display
    private int totalSets;

    private final int restBetweenRepetitions;

    private final int restBetweenSets;
    private List<WorkoutSet> workoutSets;

    public CustomWorkoutData(String id, String name, String description,
                             int totalDurationSeconds, int totalSets,
                             List<WorkoutSet> workoutSets,int restBetweenRepetitions,int restBetweenSets) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.totalDurationSeconds = totalDurationSeconds;
        this.totalSets = totalSets;
        this.workoutSets = workoutSets;
        this.restBetweenRepetitions = restBetweenRepetitions;
        this.restBetweenSets = restBetweenSets;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public int getTotalSets() {
        return totalSets;
    }

    public List<WorkoutSet> getWorkoutSets() {
        return workoutSets;
    }

    // Setters (if needed)
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTotalDurationSeconds(int totalDurationSeconds) {
        this.totalDurationSeconds = totalDurationSeconds;
    }

    public void setTotalSets(int totalSets) {
        this.totalSets = totalSets;
    }

    public void setWorkoutSets(List<WorkoutSet> workoutSets) {
        this.workoutSets = workoutSets;
    }

    public int getRestBetweenRepetitions() {
        return restBetweenRepetitions;
    }

    public int getRestBetweenSets() {
        return restBetweenSets;
    }
}
