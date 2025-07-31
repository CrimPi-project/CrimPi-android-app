package com.almogbb.crimpi.workouts;

import java.io.Serializable;

// Represents a single exercise within a set.
public class Exercise implements Serializable {
    private String description; // e.g., "20mm max pull", "80% body weight pull"
    private int repetitions;    // Number of repetitions for this exercise
    // Removed durationSeconds as per your updated requirements
    private int minBodyPercentage; // NEW: Minimum body weight percentage for the pull

    // Constructor without minBodyPercentage (defaults to 0)
    public Exercise(String description, int repetitions) {
        this(description, repetitions, 0);
    }

    // Constructor with minBodyPercentage
    public Exercise(String description, int repetitions, int minBodyPercentage) {
        this.description = description;
        this.repetitions = repetitions;
        this.minBodyPercentage = minBodyPercentage;
    }

    // Getters
    public String getDescription() {
        return description;
    }

    public int getRepetitions() {
        return repetitions;
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

    public void setMinBodyPercentage(int minBodyPercentage) {
        this.minBodyPercentage = minBodyPercentage;
    }
}