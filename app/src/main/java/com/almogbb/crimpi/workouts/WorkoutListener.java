package com.almogbb.crimpi.workouts;

public interface WorkoutListener {
    /** Called each second during countdown before workout starts */
    void onCountdownTick(int secondsLeft);

    /** Called when workout actually starts */
    void onWorkoutStarted();

    /** Called when a new force reading is processed */
    void onForceChanged(float forceValue, boolean belowTarget);

    /** Called when workout is stopped or ended */
    void onWorkoutCompleted();

    /** Called when a target force is set */
    void onTargetSet(float targetPercentage);

    /** NEW: Called when the workout timer updates (e.g., every second) */
    void onWorkoutProgressUpdated(long elapsedTimeSeconds);
}
