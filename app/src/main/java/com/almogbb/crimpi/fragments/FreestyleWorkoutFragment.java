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
import android.widget.ImageView;
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
import com.almogbb.crimpi.data.UserDataManager;
import com.almogbb.crimpi.workouts.FreestyleWorkout;
import com.almogbb.crimpi.workouts.Workout;
import com.almogbb.crimpi.workouts.WorkoutListener;

public class FreestyleWorkoutFragment extends Fragment implements WorkoutListener {

    private static final String TAG = "FreestyleWorkoutFrag";
    private Workout workout;
    private Button startButton;
    private TextView forceTextView;
    private TextView countdownTextView;
    private TextView unitKgTextView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private View targetLine;
    private View forceBar;
    private View forceBarTrack;

    private TextView bodyPercentageTextView;
    private UserDataManager userDataManager;
    private TextView timerTextView;
    private ImageView timerIcon;
    private static final float MAX_FORCE_VALUE = 100.0f; // Example: 100 kg or 100 N

    public FreestyleWorkoutFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userDataManager = new UserDataManager(requireContext().getApplicationContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_freestyle_workout, container, false);

        workout = new FreestyleWorkout();
        workout.setListener(this);

        startButton = view.findViewById(R.id.startButton);
        forceTextView = view.findViewById(R.id.forceTextView);
        countdownTextView = view.findViewById(R.id.countdownTextView);
        unitKgTextView = view.findViewById(R.id.unitKgTextView);
        forceBar = view.findViewById(R.id.forceBar);
        forceBarTrack = view.findViewById(R.id.forceBarTrack);
        targetLine = view.findViewById(R.id.targetLine);
        bodyPercentageTextView = view.findViewById(R.id.bodyPercentageTextView);
        timerTextView = view.findViewById(R.id.timerTextView);
        timerIcon = view.findViewById(R.id.timerIcon);

        startButton.setVisibility(View.VISIBLE);
        forceTextView.setVisibility(View.GONE);
        countdownTextView.setVisibility(View.GONE);
        unitKgTextView.setVisibility(View.GONE);
        forceBar.setVisibility(View.GONE);
        forceBarTrack.setVisibility(View.GONE);
        targetLine.setVisibility(View.GONE);
        bodyPercentageTextView.setVisibility(View.GONE);
        timerTextView.setVisibility(View.GONE);
        timerIcon.setVisibility(View.GONE);


        // Set initial text for received number
        forceTextView.setText(R.string.n_a);

//        startButton.setOnClickListener(v -> {
//            BluetoothManager bluetoothManager = (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
//            if (getActivity() instanceof MainActivity) {
//                MainActivity main = (MainActivity) getActivity();
//                if (main.bluetoothGatt != null && bluetoothManager != null &&
//                        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
//
//                    int state = bluetoothManager.getConnectionState(main.bluetoothGatt.getDevice(), BluetoothProfile.GATT);
//                    if (!workout.isRunning()) {
//                        if (state == BluetoothProfile.STATE_CONNECTED) {
//                            boolean sent = main.sendCommandToPico("start");
//                            if (!sent) {
//                                Toast.makeText(requireContext(), "Failed to send start command", Toast.LENGTH_SHORT).show();
//
//                            } else {
//                                workout.start();
//                            }
//
//                        } else {
//                            Toast.makeText(requireContext(), R.string.not_connected_to_a_crimpi_device_please_connect, Toast.LENGTH_SHORT).show();
//                        }
//
//                    } else {
//                        resetWorkoutState();
//                        boolean sent = main.sendCommandToPico("stop");
//                        if (!sent) {
//                            Toast.makeText(requireContext(), "Failed to send stop command", Toast.LENGTH_SHORT).show();
//                        }
//                        workout.stop();
//                    }
//                } else {
//                    Toast.makeText(requireContext(), R.string.not_connected_to_a_crimpi_device_please_connect, Toast.LENGTH_SHORT).show();
//                }
//            }
//        });

        startButton.setOnClickListener(v -> {
            BluetoothManager bluetoothManager = (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
            if (getActivity() instanceof MainActivity) {
                MainActivity main = (MainActivity) getActivity();
                if (main.bluetoothGatt != null && bluetoothManager != null &&
                        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    int state = bluetoothManager.getConnectionState(main.bluetoothGatt.getDevice(), BluetoothProfile.GATT);
                    if (!workout.isRunning()) {
                        if (state == BluetoothProfile.STATE_CONNECTED) {
                            workout.start();
                        } else {
                            Toast.makeText(requireContext(), R.string.not_connected_to_a_crimpi_device_please_connect, Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        resetWorkoutState();
                        workout.stop();
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.not_connected_to_a_crimpi_device_please_connect, Toast.LENGTH_SHORT).show();
                }
            }
        });

        forceBarTrack.setOnClickListener(v -> {
            // This will trigger the same logic as the setTargetButton
            try {
                if (workout.isRunning()) {
                    float currentForce = Float.parseFloat(forceTextView.getText().toString());
                    workout.setTarget(currentForce);
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid force value", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void updateBodyPercentage(float currentForce) {
        float bodyWeight = userDataManager.getBodyWeight(); // Retrieve saved body weight

        if (bodyWeight > 0) { // Check if a valid body weight is set
            float percentage = (currentForce / bodyWeight) * 100;
            bodyPercentageTextView.setText(String.format(Locale.getDefault(), "Body weight percent: %.1f%%", percentage));
            bodyPercentageTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text_color)); // Default color
        } else {
            // If body weight is not set or invalid, display N/A
            bodyPercentageTextView.setText(R.string.n_a);
            bodyPercentageTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text_color)); // Indicate not set
        }
    }

    @Override
    public void onCountdownTick(int secondsLeft) {
        requireActivity().runOnUiThread(() -> {
            startButton.setVisibility(View.GONE);
            countdownTextView.setVisibility(View.VISIBLE);
            if (secondsLeft > 0) {
                countdownTextView.setText(String.valueOf(secondsLeft));
            } else {
                countdownTextView.setText(R.string.go);
            }

        });
    }

    @Override
    public void onWorkoutStarted() {
        BluetoothManager bluetoothManager = (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
        if (getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            if (main.bluetoothGatt != null && bluetoothManager != null &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {

                int state = bluetoothManager.getConnectionState(main.bluetoothGatt.getDevice(), BluetoothProfile.GATT);
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    boolean sent = main.sendCommandToPico("start");
                    if (!sent) {
                        Toast.makeText(requireContext(), "Failed to send start command", Toast.LENGTH_SHORT).show();

                    }
                }
            }
        }
        requireActivity().runOnUiThread(() -> {
            countdownTextView.setVisibility(View.GONE);
            forceTextView.setVisibility(View.VISIBLE);
            forceBarTrack.setVisibility(View.VISIBLE);
            forceBar.setVisibility(View.VISIBLE);
            startButton.setText(R.string.stop_workout);
            startButton.setVisibility(View.VISIBLE);
            forceTextView.setText(R.string.n_a);
            unitKgTextView.setVisibility(View.VISIBLE);
            forceTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text_color));
            bodyPercentageTextView.setVisibility(View.VISIBLE);
            timerTextView.setVisibility(View.VISIBLE);
            timerIcon.setVisibility(View.VISIBLE);
            timerTextView.setText(R.string.zero);
            updateBodyPercentage(0f);
        });
    }

    @Override
    public void onForceChanged(float forceValue, boolean belowTarget) {
        requireActivity().runOnUiThread(() -> {
            forceTextView.setText(String.format(Locale.getDefault(), "%.2f", forceValue));
            setForceBarPosition(forceValue);
            forceTextView.setTextColor(ContextCompat.getColor(requireContext(),
                    belowTarget ? R.color.below_target : R.color.primary_text_color));
        });
        updateBodyPercentage(forceValue);
    }

    public void onWorkoutCompleted() {
        BluetoothManager bluetoothManager = (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
        if (getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            if (main.bluetoothGatt != null && bluetoothManager != null &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {

                int state = bluetoothManager.getConnectionState(main.bluetoothGatt.getDevice(), BluetoothProfile.GATT);
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    boolean sent = main.sendCommandToPico("stop");
                    if (!sent) {
                        Toast.makeText(requireContext(), "Failed to send stop command", Toast.LENGTH_SHORT).show();

                    }
                }
            }
        }
        requireActivity().runOnUiThread(() -> {
            // Reset all workout‑related UI elements
            resetWorkoutState();

            // Also reset and hide the timer UI
            if (timerTextView != null) {
                timerTextView.setText(getString(R.string.zero)); // or "0 s"
                timerTextView.setVisibility(View.GONE);
            }

            View timerIcon = getView() != null ? getView().findViewById(R.id.timerIcon) : null;
            if (timerIcon != null) {
                timerIcon.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onTargetSet(float targetPercentage) {
        requireActivity().runOnUiThread(this::setTargetLinePosition);
    }

    @Override
    public void onWorkoutProgressUpdated(long elapsedTimeSeconds) {
        if (!isAdded()) return; // Defensive check
        requireActivity().runOnUiThread(() -> timerTextView.setText(String.format(Locale.getDefault(), "%d", elapsedTimeSeconds)));
    }

    public void resetWorkoutState() {
        if (startButton != null) {
            startButton.setText(R.string.start_workout);
            startButton.setVisibility(View.VISIBLE);
        }
        if (forceTextView != null) {
            forceTextView.setText(R.string.n_a);
            forceTextView.setVisibility(View.GONE);
            forceTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text_color));
        }
        if (countdownTextView != null) countdownTextView.setVisibility(View.GONE);
        if (forceBar != null) forceBar.setVisibility(View.GONE);
        if (forceBarTrack != null) forceBarTrack.setVisibility(View.GONE);
        if (targetLine != null) targetLine.setVisibility(View.GONE);
        if (unitKgTextView != null) unitKgTextView.setVisibility(View.GONE);
        if (bodyPercentageTextView != null) {
            bodyPercentageTextView.setText(R.string.n_a);
            bodyPercentageTextView.setVisibility(View.GONE);
        }
        if (timerIcon != null) timerIcon.setVisibility(View.GONE);
        if (timerTextView != null) {
            timerTextView.setVisibility(View.GONE);
            timerTextView.setText(R.string.zero);
            workout.setElapsedTimeMillis(0);
        }
        setForceBarPosition(0.0f);
        handler.removeCallbacksAndMessages(null);
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
        if (forceBar == null || targetLine == null || forceTextView == null) {
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

            } else {
                Log.w(TAG, "Force bar LayoutParams are null, cannot set target line.");
            }
        });
    }

    public void updateForceFromBLE(float value) {
        if (workout != null) {
            workout.updateForce(value);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (workout != null) {
            workout.setListener(null);
            workout.stop();
            workout = null;
        }
    }
}