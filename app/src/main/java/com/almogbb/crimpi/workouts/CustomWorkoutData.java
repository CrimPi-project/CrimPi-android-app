package com.almogbb.crimpi.workouts;

import java.util.List;

// This data class will be used for JSON serialization/deserialization.
public class CustomWorkoutData {
    private String id; // Unique ID for the workout
    private String name;
    private String description;
    private int totalDurationSeconds; // Calculated total duration for display
    private int totalSets;            // Total number of sets for display
    private List<WorkoutSet> workoutSets;

    public CustomWorkoutData(String id, String name, String description,
                             int totalDurationSeconds, int totalSets,
                             List<WorkoutSet> workoutSets) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.totalDurationSeconds = totalDurationSeconds;
        this.totalSets = totalSets;
        this.workoutSets = workoutSets;
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
}