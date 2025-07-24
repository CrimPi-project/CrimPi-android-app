package com.almogbb.crimpi.fragments;

import java.util.Locale;

import android.Manifest;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.animation.ValueAnimator;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams;

import com.almogbb.crimpi.MainActivity;
import com.almogbb.crimpi.R;

public class FreestyleWorkoutFragment extends Fragment {

    private static final String TAG = "FreestyleWorkoutFrag";

    private Button startButton;
    private TextView receivedNumberTextView;
    private TextView countdownTextView;

    private TextView unitKgTextView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int countdownValue = 3;
    private View targetLine; // Reference to the target line View
    private Button setTargetButton; // Reference to the Set Target Button
    private View forceBar;
    private View forceBarTrack;
    private static final float MAX_FORCE_VALUE = 100.0f; // Example: 100 kg or 100 N

    private float targetForcePercentage = -1.0f; // Stores the capped percentage of the target force
    private boolean targetSet = false; // Flag to indicate if a target has been set
    private boolean workoutStarted = false;

    public FreestyleWorkoutFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_freestyle_workout, container, false);

        startButton = view.findViewById(R.id.startButton);
        receivedNumberTextView = view.findViewById(R.id.freestyleReceivedNumberTextView);
        countdownTextView = view.findViewById(R.id.countdownTextView);
        unitKgTextView = view.findViewById(R.id.unitKgTextView);
        forceBar = view.findViewById(R.id.forceBar);
        forceBarTrack = view.findViewById(R.id.forceBarTrack);
        targetLine = view.findViewById(R.id.targetLine);
        setTargetButton = view.findViewById(R.id.setTargetButton);

        // Set initial visibility
        startButton.setVisibility(View.VISIBLE);
        receivedNumberTextView.setVisibility(View.GONE);
        countdownTextView.setVisibility(View.GONE);
        unitKgTextView.setVisibility(View.GONE);
        forceBar.setVisibility(View.GONE);
        forceBarTrack.setVisibility(View.GONE);
        targetLine.setVisibility(View.GONE);
        setTargetButton.setVisibility(View.GONE);

        // Set initial text for received number
        receivedNumberTextView.setText(R.string.n_a);

        startButton.setOnClickListener(v -> {
            BluetoothManager bluetoothManager = (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
            if (getActivity() instanceof MainActivity) {
                MainActivity main = (MainActivity) getActivity();
                if (main.bluetoothGatt != null && bluetoothManager != null &&
                        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {

                    int state = bluetoothManager.getConnectionState(main.bluetoothGatt.getDevice(), BluetoothProfile.GATT);
                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        if (!workoutStarted) { // Only start countdown if workout hasn't started
                            startCountdown();
                        } else { // If workout is active, this button becomes "Stop Workout"
                            stopWorkout();
                        }
                    } else {
                        Toast.makeText(requireContext(), R.string.not_connected_to_a_crimpi_device_please_connect, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.not_connected_to_a_crimpi_device_please_connect, Toast.LENGTH_SHORT).show();
                }
            }
        });

        setTargetButton.setOnClickListener(v -> {
            if (workoutStarted) { // Only allow setting target if workout is active
                setTargetLinePosition();
            }
        });
        return view;
    }

    private void startCountdown() {
        startButton.setVisibility(View.GONE);
        countdownTextView.setVisibility(View.VISIBLE);
        countdownValue = 3;
        countdownTextView.setText(String.valueOf(countdownValue));

        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (countdownValue > 0) {
                    countdownTextView.setText(String.valueOf(countdownValue));
                    countdownValue--;
                    handler.postDelayed(this, 1000);
                } else {
                    countdownTextView.setVisibility(View.GONE);
                    receivedNumberTextView.setVisibility(View.VISIBLE);
                    forceBarTrack.setVisibility(View.VISIBLE);
                    forceBar.setVisibility(View.VISIBLE);
                    startButton.setText(R.string.stop_workout);
                    startButton.setVisibility(View.VISIBLE);
                    receivedNumberTextView.setText(R.string.n_a);
                    unitKgTextView.setVisibility(View.VISIBLE);
                    setTargetButton.setVisibility(View.VISIBLE);
                    receivedNumberTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text_color));
                    workoutStarted = true;
                }
            }
        };
        handler.post(countdownRunnable);
    }

    private void stopWorkout() {
        handler.removeCallbacksAndMessages(null);
        workoutStarted = false;
        resetWorkoutState();
    }

    public void resetWorkoutState() {
        workoutStarted = false;
        if (startButton != null) {
            startButton.setText(R.string.start_workout);
            startButton.setVisibility(View.VISIBLE);
        }
        if (receivedNumberTextView != null) {
            receivedNumberTextView.setText(R.string.n_a);
            receivedNumberTextView.setVisibility(View.GONE);
            receivedNumberTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text_color));
        }
        if (countdownTextView != null) countdownTextView.setVisibility(View.GONE);
        if (forceBar != null) forceBar.setVisibility(View.GONE);
        if (forceBarTrack != null) forceBarTrack.setVisibility(View.GONE);
        if (targetLine != null) targetLine.setVisibility(View.GONE);
        if (setTargetButton != null) setTargetButton.setVisibility(View.GONE);
        if (unitKgTextView != null) unitKgTextView.setVisibility(View.GONE);
        setForceBarPosition(0.0f); // NEW: Call setForceBarPosition to reset to bottom
        targetForcePercentage = -1.0f;
        targetSet = false;
        handler.removeCallbacksAndMessages(null);
    }

    public void updateReceivedNumber(String number) {
        if (receivedNumberTextView != null && workoutStarted) {
            try {
                final float force = Float.parseFloat(number);
                // Display the number with two decimal places
                receivedNumberTextView.setText(String.format(Locale.getDefault(), "%.2f", force));
                setForceBarPosition(force);

                // NEW: Compare force to target and update text color
                if (targetSet) {
                    float currentForcePercentage = force / MAX_FORCE_VALUE;
                    if (currentForcePercentage < targetForcePercentage) {
                        receivedNumberTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.below_target));
                    } else {
                        receivedNumberTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text_color));
                    }
                } else {
                    // If target is not set, keep default color
                    receivedNumberTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text_color));
                }

            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid number format for force: " + number, e);
                receivedNumberTextView.setText(R.string.n_a);
                setForceBarPosition(0.0f);
                // On error, reset to default color
                receivedNumberTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text_color));
            }
        } else if (receivedNumberTextView != null) {
            receivedNumberTextView.setText(R.string.n_a);
            setForceBarPosition(0.0f);
            // On not started, reset to default color
            receivedNumberTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text_color));
        }
    }

    private void setForceBarPosition(final float force) { // Renamed from setForceBarHeight
        if (forceBar == null || forceBarTrack == null) {
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

        forceBarTrack.post(() -> {
            int trackHeight = forceBarTrack.getHeight();
            int actualBarHeight = forceBar.getHeight(); // Get the fixed height of the horizontal line (e.g., 3dp)

            if (trackHeight > 0 && actualBarHeight > 0) {
                // Calculate the maximum vertical travel for the line (from bottom to top of track)
                int maxVerticalTravel = trackHeight - actualBarHeight;

                // Calculate the new bottom margin based on force percentage
                // 0% force -> 0 margin (line at bottom)
                // 100% force -> maxVerticalTravel (line at top)
                final int newMarginBottom = (int) (maxVerticalTravel * cappedForcePercentage);

                LayoutParams currentParams = (LayoutParams) forceBar.getLayoutParams();
                int currentMarginBottom = (currentParams != null) ? currentParams.bottomMargin : 0;

                if (newMarginBottom != currentMarginBottom) {
                    ValueAnimator animator = ValueAnimator.ofInt(currentMarginBottom, newMarginBottom);
                    animator.addUpdateListener(animation -> {
                        int animatedMargin = (int) animation.getAnimatedValue();
                        LayoutParams params = (LayoutParams) forceBar.getLayoutParams();
                        if (params == null) {
                            params = new LayoutParams(LayoutParams.MATCH_CONSTRAINT, actualBarHeight);
                            params.bottomToBottom = R.id.forceBarTrack;
                            params.startToStart = R.id.forceBarTrack;
                            params.endToEnd = R.id.forceBarTrack;
                        }
                        params.bottomMargin = animatedMargin; // Animate bottom margin
                        forceBar.setLayoutParams(params);
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

    private void setTargetLinePosition() {
        if (forceBar == null || targetLine == null || receivedNumberTextView == null) {
            Log.e(TAG, "Force bar, target line, or receivedNumberTextView not initialized.");
            return;
        }

        // Ensure layout is complete before getting margins
        forceBar.post(() -> {
            LayoutParams forceBarParams = (LayoutParams) forceBar.getLayoutParams();
            if (forceBarParams != null) {
                int currentForceBarMarginBottom = forceBarParams.bottomMargin;

                LayoutParams targetLineParams = (LayoutParams) targetLine.getLayoutParams();
                if (targetLineParams == null) {
                    // Initialize params if null (first time setting)
                    targetLineParams = new LayoutParams(LayoutParams.MATCH_CONSTRAINT, targetLine.getHeight());
                    targetLineParams.bottomToBottom = R.id.forceBarTrack;
                    targetLineParams.startToStart = R.id.forceBarTrack;
                    targetLineParams.endToEnd = R.id.forceBarTrack;
                }
                targetLineParams.bottomMargin = currentForceBarMarginBottom; // Set target line to force bar's current margin
                targetLine.setLayoutParams(targetLineParams);
                targetLine.setVisibility(View.VISIBLE);

                // Store the current force percentage as the target
                try {
                    float currentForce = Float.parseFloat(receivedNumberTextView.getText().toString());
                    targetForcePercentage = currentForce / MAX_FORCE_VALUE;
                    if (targetForcePercentage > 1.0f) {
                        targetForcePercentage = 1.0f; // Cap at 100%
                    }
                    targetSet = true;
                    Log.d(TAG, "Target line set at margin: " + currentForceBarMarginBottom + ", Target Force Percentage: " + targetForcePercentage);
                    Toast.makeText(requireContext(), "Target set!", Toast.LENGTH_SHORT).show();

                    // Immediately re-evaluate color based on newly set target
                    updateReceivedNumber(receivedNumberTextView.getText().toString());

                } catch (NumberFormatException e) {
                    Log.e(TAG, "Could not parse current force for target setting: " + receivedNumberTextView.getText().toString(), e);
                    Toast.makeText(requireContext(), "Error setting target: Invalid force value.", Toast.LENGTH_SHORT).show();
                    targetSet = false; // Don't set target if parsing fails
                }
            } else {
                Log.w(TAG, "Force bar LayoutParams are null, cannot set target line.");
            }
        });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }
}