package com.almogbb.crimpi.workouts;

import java.io.Serializable;
import java.util.List;

public class WorkoutSet implements Serializable {
    private List<Exercise> exercises; // List of exercises in this set

    public WorkoutSet(List<Exercise> exercises) {
        this.exercises = exercises;
    }

    // Getters
    public List<Exercise> getExercises() {
        return exercises;
    }


    // Setters (if needed)
    public void setExercises(List<Exercise> exercises) {
        this.exercises = exercises;
    }

}