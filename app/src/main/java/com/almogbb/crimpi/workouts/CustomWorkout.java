// CustomWorkout.java
package com.almogbb.crimpi.workouts;

import android.content.Context;

import com.almogbb.crimpi.data.UserDataManager;

import java.util.List;
import java.util.Locale;

public class CustomWorkout extends Workout {

    private CustomWorkoutData customWorkoutData;
    private int currentSetIndex;
    private int currentExerciseIndex;
    private int currentRepetitionCount; // NEW: To track current repetition for an exercise
    private long exerciseStartTime; // This now represents the start time of the *current repetition*
    private long restStartTime;
    private WorkoutState currentState;

    // Fixed durations as per your new requirements
    private static final long EXERCISE_DURATION_PER_REP_MILLIS = 10 * 1000; // 10 seconds per repetition
    private static final long REST_BETWEEN_REPETITIONS_MILLIS = 10 * 1000; // 10 seconds rest after each repetition
    private static final long REST_BETWEEN_EXERCISES_MILLIS = 10 * 1000; // 10 seconds
    private static final long REST_AFTER_SET_MILLIS = 90 * 1000; // 90 seconds

    private Runnable runnable;
    private UserDataManager userDataManager;
    private enum WorkoutState {
        EXERCISE,
        REST_BETWEEN_REPETITIONS, // State for rest between individual repetitions
        REST_BETWEEN_EXERCISES,
        REST_AFTER_SET,
        COMPLETED
    }

    private final CustomWorkoutListener customWorkoutListener;

    public CustomWorkout(CustomWorkoutData customWorkoutData, CustomWorkoutListener listener, Context context) {
        super();
        this.customWorkoutData = customWorkoutData;
        this.currentSetIndex = 0;
        this.currentExerciseIndex = 0;
        this.currentRepetitionCount = 0; // Initialize repetition count
        this.currentState = WorkoutState.EXERCISE;
        super.setListener(listener);
        this.customWorkoutListener = listener;
        this.userDataManager = new UserDataManager(context.getApplicationContext());
    }

    @Override
    public void start() {
        if (!workoutStarted) {
            workoutStarted = true;
            startTimeMillis = System.currentTimeMillis(); // startTimeMillis is from Workout superclass
            if (customWorkoutListener != null) {
                customWorkoutListener.onWorkoutStarted();
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

    private void startWorkoutLoop() {
        runnable = new Runnable() {
            @Override
            public void run() {
                if (!workoutStarted) return;

                long currentTime = System.currentTimeMillis();
                switch (currentState) {
                    case EXERCISE:
                        handleExerciseState(currentTime);
                        break;
                    case REST_BETWEEN_REPETITIONS:
                        handleRestBetweenRepetitionsState(currentTime);
                        break;
                    case REST_BETWEEN_EXERCISES:
                        handleRestBetweenExercisesState(currentTime);
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

                if (handler != null) {
                    handler.postDelayed(this, 100); // Update every 100ms
                }
            }
        };
        if (handler != null) {
            handler.post(runnable);
        }

        // Initialize start times for the first exercise/repetition
        exerciseStartTime = System.currentTimeMillis();
        updateMinBodyPercentageForCurrentExercise();
    }

    private void handleExerciseState(long currentTime) {
        List<WorkoutSet> sets = customWorkoutData.getWorkoutSets();
        if (currentSetIndex >= sets.size()) {
            currentState = WorkoutState.COMPLETED;
            return;
        }

        WorkoutSet currentWorkoutSet = sets.get(currentSetIndex);
        List<Exercise> exercises = currentWorkoutSet.getExercises();

        if (currentExerciseIndex >= exercises.size()) {
            // All exercises in the current set are done, move to rest after set
            currentState = WorkoutState.REST_AFTER_SET;
            restStartTime = currentTime;
            if (customWorkoutListener != null) {
                customWorkoutListener.onRestStarted(REST_AFTER_SET_MILLIS);
            }
            return;
        }

        Exercise currentExercise = exercises.get(currentExerciseIndex);

        // Calculate remaining time for the current repetition
        long remainingTimeInCurrentRep = EXERCISE_DURATION_PER_REP_MILLIS - (currentTime - exerciseStartTime);

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
            customWorkoutListener.onExerciseTimerUpdated(remainingTimeInCurrentRep);
        }

        // Check if the current repetition is complete
        if (remainingTimeInCurrentRep <= 0) {
            // Repetition is finished
            if (currentRepetitionCount + 1 < currentExercise.getRepetitions()) {
                // More repetitions for the current exercise, go to rest between repetitions
                currentState = WorkoutState.REST_BETWEEN_REPETITIONS;
                restStartTime = currentTime;
                if (customWorkoutListener != null) {
                    customWorkoutListener.onRestStarted(REST_BETWEEN_REPETITIONS_MILLIS);
                }
            } else {
                // All repetitions for the current exercise are done
                currentExerciseIndex++; // Move to the next exercise
                currentRepetitionCount = 0; // Reset repetition count for the new exercise

                if (currentExerciseIndex < exercises.size()) {
                    // More exercises in this set, go to rest between exercises
                    currentState = WorkoutState.REST_BETWEEN_EXERCISES;
                    restStartTime = currentTime;
                    if (customWorkoutListener != null) {
                        customWorkoutListener.onRestStarted(REST_BETWEEN_EXERCISES_MILLIS);
                    }
                } else {
                    // All exercises in this set are done, go to rest after set
                    currentState = WorkoutState.REST_AFTER_SET;
                    restStartTime = currentTime;
                    if (customWorkoutListener != null) {
                        customWorkoutListener.onRestStarted(REST_AFTER_SET_MILLIS);
                    }
                }
                updateMinBodyPercentageForCurrentExercise();
            }
        }
    }

    private void handleRestBetweenRepetitionsState(long currentTime) {
        long remainingRestTime = REST_BETWEEN_REPETITIONS_MILLIS - (currentTime - restStartTime);
        if (customWorkoutListener != null) {
            customWorkoutListener.onRestTimerUpdated(remainingRestTime);
        }

        if (remainingRestTime <= 0) {
            // Rest completed, move back to EXERCISE state for the next repetition
            currentState = WorkoutState.EXERCISE;
            exerciseStartTime = currentTime; // CRITICAL: Reset exerciseStartTime for the new repetition
            currentRepetitionCount++; // Increment repetition count *after* rest, before new rep starts
            if (customWorkoutListener != null) {
                customWorkoutListener.onRestEnded();
            }
        }
    }

    private void handleRestBetweenExercisesState(long currentTime) {
        long remainingRestTime = REST_BETWEEN_EXERCISES_MILLIS - (currentTime - restStartTime);
        if (customWorkoutListener != null) {
            customWorkoutListener.onRestTimerUpdated(remainingRestTime);
        }

        if (remainingRestTime <= 0) {
            currentState = WorkoutState.EXERCISE;
            exerciseStartTime = currentTime; // Reset exercise timer for the new exercise
            if (customWorkoutListener != null) {
                customWorkoutListener.onRestEnded();
            }
            updateMinBodyPercentageForCurrentExercise();
        }
    }

    private void handleRestAfterSetState(long currentTime) {
        long remainingRestTime = REST_AFTER_SET_MILLIS - (currentTime - restStartTime);
        if (customWorkoutListener != null) {
            customWorkoutListener.onRestTimerUpdated(remainingRestTime);
        }

        if (remainingRestTime <= 0) {
            currentSetIndex++;
            currentExerciseIndex = 0; // Reset exercise index for the new set
            currentRepetitionCount = 0; // Reset repetition count for the new set

            if (currentSetIndex < customWorkoutData.getWorkoutSets().size()) {
                currentState = WorkoutState.EXERCISE;
                exerciseStartTime = currentTime; // Reset exercise timer for the new set
                if (customWorkoutListener != null) {
                    customWorkoutListener.onRestEnded();
                }
                updateMinBodyPercentageForCurrentExercise();
            } else {
                currentState = WorkoutState.COMPLETED;
            }
        }
    }

    private void updateMinBodyPercentageForCurrentExercise() {
        if (customWorkoutListener != null) {
            if (currentSetIndex < customWorkoutData.getWorkoutSets().size() &&
                    currentExerciseIndex < customWorkoutData.getWorkoutSets().get(currentSetIndex).getExercises().size()) {
                Exercise nextExercise = customWorkoutData.getWorkoutSets().get(currentSetIndex).getExercises().get(currentExerciseIndex);

            }
        }
    }

    public String getCurrentWorkoutStatus() {
        List<WorkoutSet> sets = customWorkoutData.getWorkoutSets();
        if (currentSetIndex >= sets.size()) {
            return "Workout Completed";
        }

        WorkoutSet currentWorkoutSet = sets.get(currentSetIndex);
        List<Exercise> exercises = currentWorkoutSet.getExercises();

        String status = "";
        switch (currentState) {
            case EXERCISE:
                if (currentExerciseIndex < exercises.size()) {
                    Exercise currentExercise = exercises.get(currentExerciseIndex);
                    status = String.format("Set %d/%d - %s x %d (Rep %d/%d)",
                            currentSetIndex + 1, sets.size(),
                            currentExercise.getDescription(),
                            currentExercise.getRepetitions(),
                            currentRepetitionCount + 1,
                            currentExercise.getRepetitions());
                    if (currentExercise.getMinBodyPercentage() > 0) {
                        status += String.format(" (Min Body: %d%%)", currentExercise.getMinBodyPercentage());
                    }
                }
                break;
            case REST_BETWEEN_REPETITIONS:
                if (currentSetIndex < sets.size() && currentExerciseIndex < exercises.size()) {
                    Exercise currentExercise = exercises.get(currentExerciseIndex); // Use currentExercise
                    status = String.format("Set %d/%d - Rest (Repetition) - Next Rep: %d/%d of %s",
                            currentSetIndex + 1, sets.size(),
                            currentRepetitionCount + 2, currentExercise.getRepetitions(), currentExercise.getDescription());
                } else {
                    status = String.format("Set %d/%d - Rest (Repetition)", currentSetIndex + 1, sets.size());
                }
                break;
            case REST_BETWEEN_EXERCISES:
                status = String.format("Set %d/%d - Rest (Exercise)", currentSetIndex + 1, sets.size());
                break;
            case REST_AFTER_SET:
                status = String.format("Set %d/%d - Rest (Set)", currentSetIndex + 1, sets.size());
                break;
            case COMPLETED:
                status = "Workout Completed";
                break;
        }
        return status;
    }
}
