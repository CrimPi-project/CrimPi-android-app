// FreestyleWorkoutFragment.java
package com.almogbb.crimpi;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class FreestyleWorkoutFragment extends Fragment {

    private Button startButton;
    private TextView receivedNumberTextView;
    private TextView countdownTextView; // New TextView for countdown
    private Handler handler = new Handler(Looper.getMainLooper()); // Handler for countdown
    private int countdownValue = 3; // Initial countdown value
    private boolean workoutStarted = false; // Flag to indicate if workout is active

    public FreestyleWorkoutFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_freestyle_workout, container, false);

        // Initialize UI elements
        startButton = view.findViewById(R.id.startButton);
        receivedNumberTextView = view.findViewById(R.id.freestyleReceivedNumberTextView);
        countdownTextView = view.findViewById(R.id.countdownTextView); // Initialize countdown TextView

        // Set initial visibility
        startButton.setVisibility(View.VISIBLE);
        receivedNumberTextView.setVisibility(View.GONE);
        countdownTextView.setVisibility(View.GONE);

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
                        startCountdown();
                    } else {
                        Toast.makeText(requireContext(), R.string.not_connected_to_a_crimpi_device_please_connect, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.not_connected_to_a_crimpi_device_please_connect, Toast.LENGTH_SHORT).show();
                }
            }

        });

        return view;
    }

    private void startCountdown() {
        startButton.setVisibility(View.GONE); // Hide start button
        countdownTextView.setVisibility(View.VISIBLE); // Show countdown text
        countdownValue = 3; // Reset countdown

        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (countdownValue > 0) {
                    countdownTextView.setText(String.valueOf(countdownValue));
                    countdownValue--;
                    handler.postDelayed(this, 1000); // Repeat after 1 second
                } else {
                    countdownTextView.setVisibility(View.GONE); // Hide countdown
                    receivedNumberTextView.setVisibility(View.VISIBLE); // Show received number
                    workoutStarted = true; // Indicate workout has started
                    // Optionally, reset received number to N/A or 0.00
                    receivedNumberTextView.setText(R.string.n_a);
                }
            }
        };
        handler.post(countdownRunnable); // Start the countdown
    }


    public void updateReceivedNumber(String number) {
        // Only update the received number TextView if the workout has started
        if (workoutStarted && receivedNumberTextView != null) {
            receivedNumberTextView.setText(number);
        } else if (!workoutStarted && receivedNumberTextView != null) {
            // If workout hasn't started, ensure it's "N/A" or empty
            receivedNumberTextView.setText(R.string.n_a);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove any pending callbacks to prevent memory leaks
        handler.removeCallbacksAndMessages(null);
    }

    // You might want to add methods to reset the workout state if the user navigates away
    // or disconnects, but for now, this handles the basic start/countdown.
    public void resetWorkoutState() {
        workoutStarted = false;
        if (startButton != null) startButton.setVisibility(View.VISIBLE);
        if (receivedNumberTextView != null) {
            receivedNumberTextView.setText(R.string.n_a);
            receivedNumberTextView.setVisibility(View.GONE);
        }
        if (countdownTextView != null) countdownTextView.setVisibility(View.GONE);
        handler.removeCallbacksAndMessages(null); // Stop any ongoing countdown
    }
}
