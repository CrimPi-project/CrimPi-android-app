package com.almogbb.crimpi.workouts;

import java.io.Serializable;
import java.util.List;

// Represents a single set in a custom workout.
public class WorkoutSet implements Serializable {
    private List<Exercise> exercises; // List of exercises in this set
    private int restAfterSetSeconds;   // Rest time after this set is completed

    public WorkoutSet(List<Exercise> exercises, int restAfterSetSeconds) {
        this.exercises = exercises;
        this.restAfterSetSeconds = restAfterSetSeconds;
    }

    // Getters
    public List<Exercise> getExercises() {
        return exercises;
    }

    public int getRestAfterSetSeconds() {
        return restAfterSetSeconds;
    }

    // Setters (if needed)
    public void setExercises(List<Exercise> exercises) {
        this.exercises = exercises;
    }

    public void setRestAfterSetSeconds(int restAfterSetSeconds) {
        this.restAfterSetSeconds = restAfterSetSeconds;
    }
}