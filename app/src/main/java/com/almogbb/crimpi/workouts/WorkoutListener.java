package com.almogbb.crimpi.workouts;

public interface WorkoutListener {
    /** Called each second during countdown before workout starts */
    void onCountdownTick(int secondsLeft);

    /** Called when workout actually starts */
    void onWorkoutStarted();

    /** Called when a new force reading is processed */
    void onForceChanged(float forceValue, boolean belowTarget);

    /** Called when workout is stopped or ended */
    void onWorkoutStopped();

    /** Called when a target force is set */
    void onTargetSet(float targetPercentage);
}