// HomeFragment.java
package com.almogbb.crimpi.fragments;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.almogbb.crimpi.MainActivity;
import com.almogbb.crimpi.R;

public class HomeFragment extends Fragment {

    // Declare UI elements that will be in this fragment's layout
    private ImageView centralLogoImageView;
    private TextView instructionTextView;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        // Initialize UI elements from the inflated layout
        centralLogoImageView = view.findViewById(R.id.centralLogoImageView);
        instructionTextView = view.findViewById(R.id.instructionTextView);

        // Set initial text for instruction
        instructionTextView.setText(R.string.crimpi_connect);

        // Initially set visibility
        centralLogoImageView.setVisibility(View.VISIBLE);
        instructionTextView.setVisibility(View.VISIBLE);

        return view;
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get BluetoothAdapter
        BluetoothManager bluetoothManager = (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = (bluetoothManager != null) ? bluetoothManager.getAdapter() : null;

        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            instructionTextView.setText(R.string.bluetooth_not_supported);
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            // Bluetooth is OFF
            showDisconnectedStateUI();
            return;
        }

        // If you want to check for an existing GATT connection, you need a reference to the device.
        // For example, if MainActivity holds `bluetoothGatt`, you can ask it:
        if (getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            if (main.bluetoothGatt != null &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {

                int state = bluetoothManager.getConnectionState(main.bluetoothGatt.getDevice(), BluetoothProfile.GATT);
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    showConnectedStateUI();
                } else {
                    showDisconnectedStateUI();
                }
            } else {
                showDisconnectedStateUI();
            }
        }
    }

    // Public method to allow MainActivity to update instruction text
    public void updateInstructionText(String text) {
        if (instructionTextView != null) {
            instructionTextView.setText(text);
        }
    }

    // REMOVED: public void updateReceivedNumber(String number) method

    // Public methods to control UI visibility based on connection state
    public void showDisconnectedStateUI() {
        if (centralLogoImageView != null) {
            centralLogoImageView.setVisibility(View.VISIBLE);
            centralLogoImageView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary_text_color));
        }
        if (instructionTextView != null) {
            instructionTextView.setVisibility(View.VISIBLE);
            instructionTextView.setText(R.string.crimpi_connect);
        }
    }

    public void showConnectedStateUI() {
        // When connected, show the logo and update the instruction text
        if (centralLogoImageView != null) {
            centralLogoImageView.setVisibility(View.VISIBLE);
            centralLogoImageView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.bluetooth_connected_blue));        }
        if (instructionTextView != null) {
            instructionTextView.setVisibility(View.VISIBLE);
            instructionTextView.setText(getString(R.string.connected_to_a_crimpi_device));
        }



    }
}
