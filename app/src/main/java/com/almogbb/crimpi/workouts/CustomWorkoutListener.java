package com.almogbb.crimpi.workouts;

public interface CustomWorkoutListener extends WorkoutListener {
    // Methods specific to CustomWorkout
    void onCurrentWorkoutProgress(String primaryStatus, String secondaryStatus);

    void onExerciseTimerUpdated(long remainingTimeMillis);

    void onRestStarted(long totalRestDurationMillis);

    void onRestTimerUpdated(long remainingRestTimeMillis);

    void onMinBodyPercentageUpdated(int minBodyPercentage);

    void onRestEnded();
}