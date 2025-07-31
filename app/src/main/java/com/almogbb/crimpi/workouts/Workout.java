package com.almogbb.crimpi.workouts;

import android.os.Handler;
import android.os.Looper;

public abstract class Workout {

    protected WorkoutListener listener;

    // Common state
    protected boolean workoutStarted = false;
    protected boolean targetSet = false;
    protected float targetForce = -1f;
    protected static final float MAX_FORCE_VALUE = 100f;

    protected Handler handler = new Handler(Looper.getMainLooper());

    // NEW: Timer related attributes
    protected long startTimeMillis; // Timestamp when workout (or current active segment) started
    protected long elapsedTimeMillis; // Total time spent in ACTIVE state
    protected Runnable timerRunnable; // Runnable for timer updates

    public void setListener(WorkoutListener listener) {
        this.listener = listener;
    }

    // Abstract methods to implement per workout type
    public abstract void start();

    public abstract void stop();

    // Common methods can be overridden
    public void updateForce(float force) {
        if (!workoutStarted) return;

        // calculate below/above target
        boolean below = false;
        if (targetSet) {
            below = (force < targetForce);
        }
        // Notify fragment
        if (listener != null) {
            listener.onForceChanged(force, below);
        }
    }

    public boolean isRunning() {
        return workoutStarted;
    }

    public void setTarget(float forceValue) {
        if (!workoutStarted) return;
        this.targetForce = forceValue; // Store the absolute target force
        this.targetSet = true;

        if (listener != null) {
            // Calculate a UI-friendly percentage for the listener, relative to MAX_FORCE_VALUE
            float uiTargetPercentageForUI = forceValue / MAX_FORCE_VALUE;
            if (uiTargetPercentageForUI > 1f) uiTargetPercentageForUI = 1f;
            listener.onTargetSet(uiTargetPercentageForUI); // Pass the UI-friendly percentage
        }
    }

    // NEW: Getters for timer data
    public long getElapsedTimeSeconds() {
        if (workoutStarted) { // Only count time if workout is active
            return (elapsedTimeMillis + (System.currentTimeMillis() - startTimeMillis)) / 1000;
        } else {
            return elapsedTimeMillis / 1000; // Return accumulated time if paused/stopped
        }
    }
    public void setElapsedTimeMillis(long elapsedTimeMillis) {
        this.elapsedTimeMillis = elapsedTimeMillis;
    }

    // NEW: Internal Timer Management
    protected void startTimer() {
        handler.removeCallbacks(timerRunnable);
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (workoutStarted) { // Only update if workout is active
                    long currentElapsedTime = getElapsedTimeSeconds(); // Use the getter
                    if (listener != null) {
                        listener.onWorkoutProgressUpdated(currentElapsedTime);
                    }
                    handler.postDelayed(this, 1000); // Post again after 1 second
                }
            }
        };
        handler.post(timerRunnable);
    }

    protected void stopTimer() {
        handler.removeCallbacks(timerRunnable);
    }
}
