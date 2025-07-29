// Exercise.java
package com.almogbb.crimpi.workouts;

// Represents a single exercise within a set.
public class Exercise {
    private String description; // e.g., "20mm max pull", "80% body weight pull"
    private int repetitions;    // Number of repetitions for this exercise
    private int durationSeconds; // Duration for the exercise if it's time-based (e.g., 7s hang)
    private int minBodyPercentage; // NEW: Minimum body weight percentage for the pull

    public Exercise(String description, int repetitions) {
        this(description, repetitions, 0, 0); // Default duration and minBodyPercentage to 0
    }

    public Exercise(String description, int repetitions, int durationSeconds) {
        this(description, repetitions, durationSeconds, 0); // Default minBodyPercentage to 0
    }

    public Exercise(String description, int repetitions, int durationSeconds, int minBodyPercentage) {
        this.description = description;
        this.repetitions = repetitions;
        this.durationSeconds = durationSeconds;
        this.minBodyPercentage = minBodyPercentage;
    }

    // Getters
    public String getDescription() {
        return description;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getMinBodyPercentage() {
        return minBodyPercentage;
    }

    // Setters (if needed, but for data classes, often immutable)
    public void setDescription(String description) {
        this.description = description;
    }

    public void setRepetitions(int repetitions) {
        this.repetitions = repetitions;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public void setMinBodyPercentage(int minBodyPercentage) {
        this.minBodyPercentage = minBodyPercentage;
    }
}
