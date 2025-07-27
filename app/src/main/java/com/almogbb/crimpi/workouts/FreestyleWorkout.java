package com.almogbb.crimpi.workouts;

public class FreestyleWorkout extends Workout {

    private int countdownValue = 3;

    @Override
    public void start() {
        if (workoutStarted) return;
        // Start countdown
        countdownValue = 3;
        if (listener != null) listener.onCountdownTick(countdownValue);
        // countdown done, start workout
        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (countdownValue > 0) {
                    countdownValue--;
                    if (listener != null) listener.onCountdownTick(countdownValue);
                    handler.postDelayed(this, 1000);
                } else {
                    // countdown done, start workout
                    workoutStarted = true;
                    if (listener != null) listener.onWorkoutStarted();
                }
            }
        };
        handler.postDelayed(countdownRunnable, 1000);
    }

    @Override
    public void stop() {
        targetSet = false;
        targetForcePercentage = -1f;
        workoutStarted = false;
        handler.removeCallbacksAndMessages(null);
        if (listener != null) listener.onWorkoutStopped();
    }
}
