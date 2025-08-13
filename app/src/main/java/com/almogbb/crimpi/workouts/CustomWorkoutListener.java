package com.almogbb.crimpi.workouts;

public interface CustomWorkoutListener extends WorkoutListener {
    // Methods specific to CustomWorkout
    void onCurrentWorkoutProgress(String primaryStatus, String secondaryStatus);

    void onExerciseTimerUpdated(long remainingTimeMillis);

    void onRestStarted(long totalRestDurationMillis,boolean isStartCountdown);

    void onRestTimerUpdated(long remainingRestTimeMillis,boolean isStartCountdown);

    void onMinBodyPercentageUpdated(int minBodyPercentage);

    void onRestEnded(boolean isStartCountdown);
}