package com.almogbb.crimpi.fragments; // Adjust package as needed

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log; // For logging, similar to FreestyleWorkoutFragment
import android.animation.ValueAnimator; // For force bar animation
import android.widget.Toast; // For Toast messages

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams; // For layout params
import androidx.core.content.ContextCompat; // For getting colors

import com.almogbb.crimpi.R; // Make sure R is correctly imported
import com.almogbb.crimpi.workouts.CustomWorkout;
import com.almogbb.crimpi.workouts.CustomWorkoutData;
import com.almogbb.crimpi.workouts.CustomWorkoutListener;
import com.almogbb.crimpi.data.UserDataManager; // Assuming this path for UserDataManager

import java.util.Locale; // For formatting time

public class CustomWorkoutFragment extends Fragment implements CustomWorkoutListener {

    private static final String TAG = "CustomWorkoutFrag"; // Tag for logging

    // Argument key for passing CustomWorkoutData
    private static final String ARG_WORKOUT_DATA = "workout_data";

    private CustomWorkoutData workoutData;
    private CustomWorkout customWorkout;

    // UI Elements
    private TextView primaryStatusTextView;
    private TextView secondaryStatusTextView;
    private TextView exerciseTimerTextView; // Mapped to timerCustomWorkoutTextView
    private TextView restTimerTextView;
    private TextView bodyPercentageTextView; // Mapped to bodyPercentageCustomWorkoutTextView
    private TextView forceValueTextView; // Mapped to forceCustomWorkoutTextView
    private TextView unitKgCustomWorkoutTextView; // Added for "kg" unit
    private View forceBarCustomWorkout; // Force bar view
    private View forceBarCustomWorkoutTrack; // Force bar track view
    private View targetLineCustomWorkout; // Target line view
    private ImageView timerImageView;

    private UserDataManager userDataManager; // For body weight
    private static final float MAX_FORCE_VALUE = 100.0f; // Example: 100 kg or 100 N

    // Factory method to create a new instance of this fragment with workout data
    public static CustomWorkoutFragment newInstance(CustomWorkoutData workoutData) {
        CustomWorkoutFragment fragment = new CustomWorkoutFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_WORKOUT_DATA, workoutData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            workoutData = (CustomWorkoutData) getArguments().getSerializable(ARG_WORKOUT_DATA);
        }
        userDataManager = new UserDataManager(requireContext().getApplicationContext()); // Initialize UserDataManager
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_custom_workout, container, false);

        // Initialize UI elements (connect to IDs in fragment_custom_workout.xml)
        primaryStatusTextView = view.findViewById(R.id.primary_status_text_view);
        secondaryStatusTextView = view.findViewById(R.id.secondary_status_text_view);
        exerciseTimerTextView = view.findViewById(R.id.timerCustomWorkoutTextView);
        restTimerTextView = view.findViewById(R.id.rest_timer_text_view);
        bodyPercentageTextView = view.findViewById(R.id.bodyPercentageCustomWorkoutTextView);
        forceValueTextView = view.findViewById(R.id.forceCustomWorkoutTextView);
        unitKgCustomWorkoutTextView = view.findViewById(R.id.unitKgCustomWorkoutTextView); // Initialize unitKgTextView
        forceBarCustomWorkout = view.findViewById(R.id.forceBarCustomWorkout); // Initialize forceBar
        forceBarCustomWorkoutTrack = view.findViewById(R.id.forceBarCustomWorkoutTrack); // Initialize forceBarTrack
        targetLineCustomWorkout = view.findViewById(R.id.targetLineCustomWorkout); // Initialize targetLine
        timerImageView = view.findViewById(R.id.timerCustomWorkoutIcon); // Initialize timerImageView


        // Set initial visibility for all elements (similar to FreestyleWorkoutFragment)
        forceValueTextView.setVisibility(View.GONE);
        unitKgCustomWorkoutTextView.setVisibility(View.GONE);
        forceBarCustomWorkout.setVisibility(View.GONE);
        forceBarCustomWorkoutTrack.setVisibility(View.GONE);
        targetLineCustomWorkout.setVisibility(View.GONE);
        bodyPercentageTextView.setVisibility(View.GONE); // Initially hidden, shown when workout starts
        exerciseTimerTextView.setVisibility(View.GONE); // Initially hidden, shown when workout starts
        restTimerTextView.setVisibility(View.GONE); // Initially hidden
        timerImageView.setVisibility(View.GONE);

        // Set initial text for force value
        forceValueTextView.setText(R.string.n_a);
        bodyPercentageTextView.setText(R.string.n_a); // Set initial text for body percentage
        exerciseTimerTextView.setText(R.string.zero); // Set initial text for timer

        // Initialize CustomWorkout and START it immediately
        if (workoutData != null) {
            customWorkout = new CustomWorkout(workoutData, this,requireContext());
            customWorkout.start(); // Automatically start the workout when the fragment is created
        } else {
            // Handle case where workoutData is null (e.g., show an error message)
            primaryStatusTextView.setText("Error: No workout data loaded.");
            Log.e(TAG, "Workout data is null in CustomWorkoutFragment.");
        }

        // Add click listener to forceBarCustomWorkoutTrack to set target (similar to Freestyle)
        forceBarCustomWorkoutTrack.setOnClickListener(v -> {
            try {
                if (customWorkout != null && customWorkout.isRunning()) {
                    float currentForce = Float.parseFloat(forceValueTextView.getText().toString());
                    customWorkout.setTarget(currentForce);
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid force value for target", Toast.LENGTH_SHORT).show();
            }
        });


        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (customWorkout != null) {
            customWorkout.stop(); // Stop the workout when the view is destroyed
        }
    }

    // Method to update force from BLE (will be called from MainActivity or BLE service)
    public void updateForceFromBLE(float value) {
        if (customWorkout != null) {
            customWorkout.updateForce(value);
        }
    }

    // --- CustomWorkoutListener Implementations ---

    @Override
    public void onCountdownTick(int secondsLeft) {

    }

    @Override
    public void onWorkoutStarted() {
        requireActivity().runOnUiThread(() -> {
            // Make force-related UI elements visible
            forceValueTextView.setVisibility(View.VISIBLE);
            unitKgCustomWorkoutTextView.setVisibility(View.VISIBLE);
            forceBarCustomWorkoutTrack.setVisibility(View.VISIBLE);
            forceBarCustomWorkout.setVisibility(View.VISIBLE);
            targetLineCustomWorkout.setVisibility(View.VISIBLE); // Show target line when workout starts
            bodyPercentageTextView.setVisibility(View.VISIBLE);
            exerciseTimerTextView.setVisibility(View.VISIBLE); // Show exercise timer
            forceValueTextView.setText(R.string.n_a); // Reset force display
            exerciseTimerTextView.setText(R.string.zero); // Reset timer display
            primaryStatusTextView.setText(R.string.workout_started);
            secondaryStatusTextView.setText("");
            timerImageView.setVisibility(View.VISIBLE);
            restTimerTextView.setVisibility(View.GONE); // Ensure rest timer is hidden
            updateBodyPercentage(0f); // Reset body percentage display
        });
    }

    @Override
    public void onWorkoutCompleted() {
        requireActivity().runOnUiThread(() -> {
            // Hide all workout-related UI elements
            forceValueTextView.setVisibility(View.GONE);
            unitKgCustomWorkoutTextView.setVisibility(View.GONE);
            forceBarCustomWorkoutTrack.setVisibility(View.GONE);
            forceBarCustomWorkout.setVisibility(View.GONE);
            targetLineCustomWorkout.setVisibility(View.GONE);
            bodyPercentageTextView.setVisibility(View.GONE);
            exerciseTimerTextView.setVisibility(View.GONE);
            restTimerTextView.setVisibility(View.GONE);

            primaryStatusTextView.setText(R.string.workout_completed);
            secondaryStatusTextView.setText("");
            forceValueTextView.setText(String.format(Locale.getDefault(), "Force: %.1fkg", 0.0f));
        });
    }

    @Override
    public void onForceChanged(float forceValue, boolean belowTarget) {
        requireActivity().runOnUiThread(() -> {
            if (forceValueTextView != null) {
                forceValueTextView.setText(String.format(Locale.getDefault(), "%.2f", forceValue)); // Format to 2 decimal places
            }
            if (forceValueTextView != null) {
                forceValueTextView.setTextColor(ContextCompat.getColor(requireContext(),
                        belowTarget ? R.color.below_target : R.color.primary_text_color));
            }
            setForceBarPosition(forceValue); // Update force bar position
            updateBodyPercentage(forceValue); // Update body percentage
        });
    }

    @Override
    public void onTargetSet(float targetPercentage) {
        requireActivity().runOnUiThread(() -> {
            // Update the minBodyPercentageTextView to show the target percentage
            if (bodyPercentageTextView != null) {
                bodyPercentageTextView.setText(String.format(Locale.getDefault(), "Target: %.0f%%", targetPercentage * 100));
            }
            setTargetLinePosition(targetPercentage); // Set the visual target line
        });
    }

    @Override
    public void onWorkoutProgressUpdated(long elapsedTimeSeconds) {

    }

    @Override
    public void onCurrentWorkoutProgress(String primaryStatus, String secondaryStatus) {
        requireActivity().runOnUiThread(() -> {
            if (primaryStatusTextView != null) {
                primaryStatusTextView.setText(primaryStatus);
            }
            if (secondaryStatusTextView != null) {
                secondaryStatusTextView.setText(secondaryStatus);
            }
        });
    }

    @Override
    public void onExerciseTimerUpdated(long remainingTimeMillis) {
        requireActivity().runOnUiThread(() -> {
            if (exerciseTimerTextView != null) {
                long seconds = remainingTimeMillis / 1000;
                exerciseTimerTextView.setText(String.format(Locale.getDefault(), "%02d", seconds));
            }
        });
    }

    @Override
    public void onRestStarted(long totalRestDurationMillis) {
        requireActivity().runOnUiThread(() -> {
            if (restTimerTextView != null) restTimerTextView.setVisibility(View.VISIBLE);
            if (exerciseTimerTextView != null) exerciseTimerTextView.setVisibility(View.GONE);
            if (forceValueTextView != null) forceValueTextView.setVisibility(View.GONE);
            if (bodyPercentageTextView != null) bodyPercentageTextView.setVisibility(View.GONE);
            if (primaryStatusTextView != null) primaryStatusTextView.setVisibility(View.GONE);
            if (secondaryStatusTextView != null) secondaryStatusTextView.setVisibility(View.GONE);
            if (forceBarCustomWorkoutTrack != null)
                forceBarCustomWorkoutTrack.setVisibility(View.GONE);
            if (forceBarCustomWorkout != null) forceBarCustomWorkout.setVisibility(View.GONE);
            if (targetLineCustomWorkout != null) targetLineCustomWorkout.setVisibility(View.GONE);
            if (timerImageView != null) timerImageView.setVisibility(View.GONE);
            if (unitKgCustomWorkoutTextView != null)
                unitKgCustomWorkoutTextView.setVisibility(View.GONE);

            onRestTimerUpdated(totalRestDurationMillis);
        });
    }

    @Override
    public void onRestTimerUpdated(long remainingRestTimeMillis) {
        requireActivity().runOnUiThread(() -> {
            if (restTimerTextView != null) {
                long seconds = remainingRestTimeMillis / 1000;
                restTimerTextView.setText(String.format(Locale.getDefault(), "%02d", seconds));
            }
        });
    }

    @Override
    public void onMinBodyPercentageUpdated(int minBodyPercentage) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            float bodyWeight = userDataManager.getBodyWeight(); // Get user's body weight
            if (bodyWeight > 0) {
                // 1. Calculate the absolute force value based on body weight and minBodyPercentage
                float absoluteTargetForce = bodyWeight * (minBodyPercentage / 100.0f);

                // 2. Convert this absolute force to a percentage of the MAX_FORCE_VALUE for UI positioning
                float uiTargetPercentage = absoluteTargetForce / MAX_FORCE_VALUE;

                // Ensure the percentage is clamped between 0 and 1
                uiTargetPercentage = Math.max(0.0f, Math.min(1.0f, uiTargetPercentage));

                setTargetLinePosition(uiTargetPercentage);
                targetLineCustomWorkout.setVisibility(View.VISIBLE); // Ensure it's visible
            } else {
                // If body weight is not set, we cannot calculate the target force, so hide the line.
                Log.w(TAG, "Body weight not set. Cannot calculate minBodyPercentage target line position.");
                targetLineCustomWorkout.setVisibility(View.GONE);
            }
            if (minBodyPercentage <= 0) {
                // If minBodyPercentage is 0 or less, hide the target line
                targetLineCustomWorkout.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onRestEnded() {
        requireActivity().runOnUiThread(() -> {
            if (restTimerTextView != null) restTimerTextView.setVisibility(View.GONE);
            if (exerciseTimerTextView != null) exerciseTimerTextView.setVisibility(View.VISIBLE);
            if (forceValueTextView != null) forceValueTextView.setVisibility(View.VISIBLE);
            if (bodyPercentageTextView != null) bodyPercentageTextView.setVisibility(View.VISIBLE);
            if (primaryStatusTextView != null) primaryStatusTextView.setVisibility(View.VISIBLE);
            if (secondaryStatusTextView != null)
                secondaryStatusTextView.setVisibility(View.VISIBLE);
            if (forceBarCustomWorkoutTrack != null)
                forceBarCustomWorkoutTrack.setVisibility(View.VISIBLE);
            if (forceBarCustomWorkout != null) forceBarCustomWorkout.setVisibility(View.VISIBLE);
            if (targetLineCustomWorkout != null)
                targetLineCustomWorkout.setVisibility(View.VISIBLE);
            if (timerImageView != null) timerImageView.setVisibility(View.VISIBLE);
            if (unitKgCustomWorkoutTextView != null)
                unitKgCustomWorkoutTextView.setVisibility(View.VISIBLE);
        });
    }

    // --- Private Helper Methods for Force Bar and Body Percentage ---

    private void updateBodyPercentage(float currentForce) {
        float bodyWeight = userDataManager.getBodyWeight(); // Retrieve saved body weight

        if (bodyWeight > 0) { // Check if a valid body weight is set
            float percentage = (currentForce / bodyWeight) * 100;
            if (bodyPercentageTextView != null) {
                bodyPercentageTextView.setText(String.format(Locale.getDefault(), "Body weight percent: %.1f%%", percentage));
                // You might want to change color based on proximity to target here, but keeping default for now
                bodyPercentageTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text_color));
            }
        } else {
            // If body weight is not set or invalid, display N/A
            if (bodyPercentageTextView != null) {
                bodyPercentageTextView.setText(R.string.n_a);
                bodyPercentageTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text_color));
            }
        }
    }


    private void setForceBarPosition(final float force) {
        if (forceBarCustomWorkout == null || forceBarCustomWorkoutTrack == null) {
            Log.e(TAG, "Force bar or track not initialized.");
            return;
        }

        final float clampedForce;
        if (force < 0) {
            clampedForce = 0;
        } else {
            clampedForce = force;
        }

        final float forcePercentage = clampedForce / MAX_FORCE_VALUE;
        final float cappedForcePercentage = Math.min(forcePercentage, 1.0f);

        forceBarCustomWorkoutTrack.post(() -> {
            int trackHeight = forceBarCustomWorkoutTrack.getHeight();
            int actualBarHeight = forceBarCustomWorkout.getHeight();

            if (trackHeight > 0 && actualBarHeight > 0) {
                int maxVerticalTravel = trackHeight - actualBarHeight;
                final int newMarginBottom = (int) (maxVerticalTravel * cappedForcePercentage);

                LayoutParams currentParams = (LayoutParams) forceBarCustomWorkout.getLayoutParams();
                int currentMarginBottom = (currentParams != null) ? currentParams.bottomMargin : 0;

                if (newMarginBottom != currentMarginBottom) {
                    ValueAnimator animator = ValueAnimator.ofInt(currentMarginBottom, newMarginBottom);
                    animator.addUpdateListener(animation -> {
                        int animatedMargin = (int) animation.getAnimatedValue();
                        LayoutParams params = (LayoutParams) forceBarCustomWorkout.getLayoutParams();
                        if (params == null) {
                            params = new LayoutParams(LayoutParams.MATCH_CONSTRAINT, actualBarHeight);
                            params.bottomToBottom = R.id.forceBarCustomWorkoutTrack;
                            params.startToStart = R.id.forceBarCustomWorkoutTrack;
                            params.endToEnd = R.id.forceBarCustomWorkoutTrack;
                        }
                        params.bottomMargin = animatedMargin;
                        forceBarCustomWorkout.setLayoutParams(params);
                    });
                    animator.setDuration(100);
                    animator.start();
                }
                Log.d(TAG, "Force: " + clampedForce + ", Track Height: " + trackHeight + ", New Margin Bottom: " + newMarginBottom);
            } else {
                Log.w(TAG, "Force bar track height or actual bar height is 0, cannot set bar position.");
            }
        });
    }

    private void setTargetLinePosition(float targetPercentage) { // Added targetPercentage parameter
        if (forceBarCustomWorkout == null || targetLineCustomWorkout == null || forceBarCustomWorkoutTrack == null) {
            Log.e(TAG, "Force bar, target line, or track not initialized.");
            return;
        }

        forceBarCustomWorkoutTrack.post(() -> {
            int trackHeight = forceBarCustomWorkoutTrack.getHeight();
            int targetLineHeight = targetLineCustomWorkout.getHeight();

            if (trackHeight > 0 && targetLineHeight > 0) {
                int maxVerticalTravel = trackHeight - targetLineHeight;
                // Calculate position based on targetPercentage
                final int newMarginBottom = (int) (maxVerticalTravel * targetPercentage);

                LayoutParams currentParams = (LayoutParams) targetLineCustomWorkout.getLayoutParams();
                int currentMarginBottom = (currentParams != null) ? currentParams.bottomMargin : 0;

                if (newMarginBottom != currentMarginBottom) {
                    ValueAnimator animator = ValueAnimator.ofInt(currentMarginBottom, newMarginBottom);
                    animator.addUpdateListener(animation -> {
                        int animatedMargin = (int) animation.getAnimatedValue();
                        LayoutParams params = (LayoutParams) targetLineCustomWorkout.getLayoutParams();
                        if (params == null) {
                            params = new LayoutParams(LayoutParams.MATCH_CONSTRAINT, targetLineHeight);
                            params.bottomToBottom = R.id.forceBarCustomWorkoutTrack;
                            params.startToStart = R.id.forceBarCustomWorkoutTrack;
                            params.endToEnd = R.id.forceBarCustomWorkoutTrack;
                        }
                        params.bottomMargin = animatedMargin;
                        targetLineCustomWorkout.setLayoutParams(params);
                    });
                    animator.setDuration(100);
                    animator.start();
                }
                targetLineCustomWorkout.setVisibility(View.VISIBLE); // Ensure target line is visible
            } else {
                Log.w(TAG, "Force bar track height or target line height is 0, cannot set target line position.");
            }
        });
    }

    public CustomWorkoutData getWorkoutData() {
        return workoutData;
    }
}
