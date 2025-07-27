package com.almogbb.crimpi.workouts;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public abstract class Workout {

    protected WorkoutListener listener;

    // Common state
    protected boolean workoutStarted = false;
    protected boolean targetSet = false;
    protected float targetForcePercentage = -1f;
    protected static final float MAX_FORCE_VALUE = 100f;

    protected Handler handler = new Handler(Looper.getMainLooper());

    // NEW: Timer related attributes
    protected long startTimeMillis; // Timestamp when workout (or current active segment) started
    protected long elapsedTimeMillis; // Total time spent in ACTIVE state
    protected long pausedTimeMillis; // Total time accumulated while PAUSED
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
            float currentForcePercentage = force / MAX_FORCE_VALUE;
            below = (currentForcePercentage < targetForcePercentage);
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
        float percentage = forceValue / MAX_FORCE_VALUE;
        if (percentage > 1f) percentage = 1f;
        targetForcePercentage = percentage;
        targetSet = true;
        if (listener != null) listener.onTargetSet(targetForcePercentage);
    }

    // NEW: Getters for timer data
    public long getElapsedTimeSeconds() {
        if (workoutStarted) { // Only count time if workout is active
            return (elapsedTimeMillis + (System.currentTimeMillis() - startTimeMillis)) / 1000;
        } else {
            return elapsedTimeMillis / 1000; // Return accumulated time if paused/stopped
        }
    }

    // NEW: Internal Timer Management
    protected void startTimer() {
        handler.removeCallbacks(timerRunnable); // Ensure no duplicate runnables
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
