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

}
