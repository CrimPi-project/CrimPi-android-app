// CustomWorkout.java
package com.almogbb.crimpi.workouts;

import android.content.Context;

import com.almogbb.crimpi.data.UserDataManager;

import java.util.List;
import java.util.Locale;

public class CustomWorkout extends Workout {

    private final CustomWorkoutData customWorkoutData;
    private int currentExerciseIndex;
    private int currentRepetitionCount; // NEW: To track current repetition for an exercise
    private long exerciseStartTime; // This now represents the start time of the *current repetition*
    private long restStartTime;

    private WorkoutState currentState;

    // Fixed durations as per your new requirements


    private long currentExerciseDurationMillis; // Duration for the current repetition of an exercise
    private final long restBetweenRepetitionsMillis;  // Rest after each repetition within a set
    private final long restAfterSetMillis;

    private Runnable runnable;
    private final UserDataManager userDataManager;

    public  enum WorkoutState {
        COUNTDOWN_BEFORE_START,
        EXERCISE,
        REST_BETWEEN_REPETITIONS,
        REST_AFTER_SET,
        COMPLETED
    }

    private final CustomWorkoutListener customWorkoutListener;

    public CustomWorkout(CustomWorkoutData customWorkoutData, CustomWorkoutListener listener, Context context) {
        super();
        this.customWorkoutData = customWorkoutData;
        this.currentExerciseIndex = 0;
        this.currentRepetitionCount = 0; // Initialize repetition count
        this.currentState = WorkoutState.COUNTDOWN_BEFORE_START;
        super.setListener(listener);
        this.customWorkoutListener = listener;
        this.userDataManager = new UserDataManager(context.getApplicationContext());
        this.restBetweenRepetitionsMillis = customWorkoutData.getRestBetweenRepetitions() * 1000L;
        this.restAfterSetMillis = customWorkoutData.getRestBetweenSets() * 1000L;
    }

    @Override
    public void start() {
        if (!workoutStarted) {
            workoutStarted = true;
            startTimeMillis = System.currentTimeMillis();

            restStartTime = startTimeMillis;

            if (customWorkoutListener != null) {
                customWorkoutListener.onRestStarted(4000,true); // 3 sec countdown
            }

            startWorkoutLoop();
        }
    }


    @Override
    public void stop() {
        if (workoutStarted) {
            workoutStarted = false;
            if (handler != null) {
                handler.removeCallbacks(runnable);
            }
            if (customWorkoutListener != null) {
                customWorkoutListener.onWorkoutCompleted();
            }
        }
    }

    private void handleCountdownBeforeStartState(long currentTime) {
        long countdownDuration = 4000L;
        long elapsed = currentTime - restStartTime;
        long remaining = countdownDuration - elapsed;

        if (customWorkoutListener != null) {
            customWorkoutListener.onRestTimerUpdated(remaining,true);
        }

        if (remaining <= 0) {
            currentState = WorkoutState.EXERCISE;
            exerciseStartTime = currentTime;
            updateCurrentExerciseDuration();

            if (customWorkoutListener != null) {
                customWorkoutListener.onRestEnded(true);
                customWorkoutListener.onWorkoutStarted(); // Trigger the UI changes AFTER countdown
            }
        }
    }

    private void startWorkoutLoop() {
        runnable = new Runnable() {
            @Override
            public void run() {
                if (!workoutStarted) return;

                long currentTime = System.currentTimeMillis();

                switch (currentState) {
                    case COUNTDOWN_BEFORE_START:
                        handleCountdownBeforeStartState(currentTime);
                        break;
                    case EXERCISE:
                        handleExerciseState(currentTime);
                        break;
                    case REST_BETWEEN_REPETITIONS: // NEW: Use this state
                        handleRestBetweenRepetitionsState(currentTime);
                        break;
                    case REST_AFTER_SET:
                        handleRestAfterSetState(currentTime);
                        break;
                    case COMPLETED:
                        stop();
                        if (customWorkoutListener != null) {
                            customWorkoutListener.onWorkoutCompleted();
                        }
                        return;
                }

                handler.postDelayed(this, 100);
            }
        };
        handler.post(runnable);
        exerciseStartTime = System.currentTimeMillis();
        updateCurrentExerciseDuration();
    }

    // In CustomWorkout.java
    private void handleExerciseState(long currentTime) {
        WorkoutSet workoutSets = customWorkoutData.getWorkoutSets();
        List<Exercise> exercises = workoutSets.getExercises();
        if (currentExerciseIndex == exercises.size()) {
            currentState = WorkoutState.COMPLETED;
            return;
        }

        // Ensure we are on a valid exercise within the current set
        if (currentExerciseIndex >= exercises.size()) {
            // This case should ideally not be reached if logic is correct,
            // but as a fallback, move to rest after set if somehow past exercises
            currentState = WorkoutState.REST_AFTER_SET;
            restStartTime = currentTime;
            if (customWorkoutListener != null) {
                customWorkoutListener.onRestStarted(restAfterSetMillis,false); // Use instance variable
            }
            return;
        }

        Exercise currentExercise = exercises.get(currentExerciseIndex);

        // NEW: Dynamically set the duration for the current repetition
        currentExerciseDurationMillis = this.customWorkoutData.getRepetitionDuration() * 1000L;

        if (customWorkoutListener != null) {
            String exerciseInfo = String.format(Locale.getDefault(), "%s - Rep %d/%d",
                    currentExercise.getDescription(),
                    currentRepetitionCount + 1,
                    currentExercise.getRepetitions());
            String minBodyWeight = "";
            if (currentExercise.getMinBodyPercentage() > 0) {
                float bodyWeight = this.userDataManager.getBodyWeight();
                minBodyWeight = String.format(Locale.getDefault(), "Minimum body weight: %d%%", currentExercise.getMinBodyPercentage());
                customWorkoutListener.onMinBodyPercentageUpdated(currentExercise.getMinBodyPercentage());
                targetForce = bodyWeight * (currentExercise.getMinBodyPercentage() / 100f);
                targetSet = true;
            } else {
                targetSet = false;
                targetForce = -1f;
            }

            customWorkoutListener.onCurrentWorkoutProgress(
                    exerciseInfo,
                    minBodyWeight
            );
            customWorkoutListener.onMinBodyPercentageUpdated(currentExercise.getMinBodyPercentage());
            customWorkoutListener.onExerciseTimerUpdated(currentExerciseDurationMillis - (currentTime - exerciseStartTime));
        }

        if (currentTime - exerciseStartTime >= currentExerciseDurationMillis) {
            if (currentRepetitionCount + 1 < currentExercise.getRepetitions()) {
                // More repetitions → rest between reps
                currentState = WorkoutState.REST_BETWEEN_REPETITIONS;
                restStartTime = currentTime;
                if (customWorkoutListener != null) {
                    customWorkoutListener.onRestStarted(restBetweenRepetitionsMillis,false);
                }
            } else {
                // All repetitions for this exercise are done
                if (currentExerciseIndex == exercises.size() - 1) {
                    // ➤ This was the last exercise of the last set → finish workout
                    currentState = WorkoutState.COMPLETED;
                    if (customWorkoutListener != null) {
                        customWorkoutListener.onWorkoutCompleted(); // If you have such a method
                    }
                } else {
                    // ➤ Not the last exercise → continue with rest
                    currentState = WorkoutState.REST_AFTER_SET; // Rest before next exercise
                    restStartTime = currentTime;

                    if (customWorkoutListener != null) {
                        customWorkoutListener.onRestStarted(restAfterSetMillis,false);
                    }
                }
            }
        }
    }

    private void handleRestBetweenRepetitionsState(long currentTime) {
        long remainingRestTime = restBetweenRepetitionsMillis - (currentTime - restStartTime); // Use instance variable
        if (customWorkoutListener != null) {
            customWorkoutListener.onRestTimerUpdated(remainingRestTime,false);
        }

        if (remainingRestTime <= 0) {
            // Rest completed, move back to EXERCISE state for the next repetition
            currentState = WorkoutState.EXERCISE;
            exerciseStartTime = currentTime; // CRITICAL: Reset exerciseStartTime for the new repetition
            currentRepetitionCount++; // Increment repetition count *after* rest, before new rep starts
            // NEW: Update duration for the next repetition (same exercise)
            updateCurrentExerciseDuration();
            if (customWorkoutListener != null) {
                customWorkoutListener.onRestEnded(false);
            }
        }
    }

    // In CustomWorkout.java
    private void handleRestAfterSetState(long currentTime) {
        long remainingRestTime = restAfterSetMillis - (currentTime - restStartTime);

        if (customWorkoutListener != null) {
            customWorkoutListener.onRestTimerUpdated(remainingRestTime,false);
        }

        if (remainingRestTime <= 0) {
            WorkoutSet currentWorkoutSet = this.customWorkoutData.getWorkoutSets();
            List<Exercise> exercises = currentWorkoutSet.getExercises();

            if (currentExerciseIndex + 1 < exercises.size()) {
                // ➤ More exercises left in this set
                currentExerciseIndex++;
                currentRepetitionCount = 0;
                currentState = WorkoutState.EXERCISE;
                exerciseStartTime = currentTime;
                updateCurrentExerciseDuration();

                if (customWorkoutListener != null) {
                    customWorkoutListener.onRestEnded(false);
                }

            } else {
                // ➤ Last exercise in the set is done → move to next set
                currentExerciseIndex = 0;
                currentRepetitionCount = 0;
                currentState = WorkoutState.COMPLETED;

            }
        }
    }

    public WorkoutState getCurrentState() {
        return currentState;
    }

    private void updateCurrentExerciseDuration() {
        WorkoutSet currentWorkoutSet = this.customWorkoutData.getWorkoutSets();
        if (currentExerciseIndex < currentWorkoutSet.getExercises().size()) {
            this.currentExerciseDurationMillis = this.customWorkoutData.getRepetitionDuration() * 1000L;
        } else {
            this.currentExerciseDurationMillis = 0; // No valid exercise, duration is 0
        }
    }

}
