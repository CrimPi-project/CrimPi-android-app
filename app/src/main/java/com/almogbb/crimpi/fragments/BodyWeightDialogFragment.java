package com.almogbb.crimpi.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast; // For displaying messages

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.almogbb.crimpi.R; // R file for resources
import com.almogbb.crimpi.data.UserDataManager; // Import our UserDataManager

/**
 * A DialogFragment for entering and saving the user's body weight.
 * It uses the dialog_body_weight_input.xml layout.
 */
public class BodyWeightDialogFragment extends DialogFragment {

    private EditText bodyWeightEditText;
    private UserDataManager userDataManager; // Instance of our data manager

    // Interface to communicate back to the calling Activity/Fragment (optional, but good practice)
    public interface BodyWeightDialogListener {
        void onBodyWeightEntered(float weight);
        void onBodyWeightCanceled();
    }

    private BodyWeightDialogListener listener;

    // Use this constructor to set the listener
    public static BodyWeightDialogFragment newInstance(BodyWeightDialogListener listener) {
        BodyWeightDialogFragment fragment = new BodyWeightDialogFragment();
        fragment.listener = listener;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize UserDataManager here. We pass the Application Context to prevent leaks.
        userDataManager = new UserDataManager(requireContext().getApplicationContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this dialog
        View view = inflater.inflate(R.layout.dialog_body_weight_input, container, false);

        // Initialize UI elements from the inflated view
        bodyWeightEditText = view.findViewById(R.id.bodyWeightEditText);
        Button cancelButton = view.findViewById(R.id.cancelButton);
        Button enterButton = view.findViewById(R.id.enterButton);

        // Load and pre-fill current body weight if available
        float currentWeight = userDataManager.getBodyWeight();
        if (currentWeight != -1.0f) { // Check if a weight was previously saved
            bodyWeightEditText.setText(String.valueOf(currentWeight));
        }

        // Set up click listeners for the buttons
        cancelButton.setOnClickListener(v -> {
            // Dismiss the dialog when Cancel is clicked
            dismiss();
            if (listener != null) {
                listener.onBodyWeightCanceled();
            }
        });

        enterButton.setOnClickListener(v -> {
            String weightText = bodyWeightEditText.getText().toString();
            if (weightText.isEmpty()) {
                Toast.makeText(getContext(), R.string.please_enter_body_weight, Toast.LENGTH_SHORT).show();
            } else {
                try {
                    float weight = Float.parseFloat(weightText);
                    if (weight <= 0) {
                        Toast.makeText(getContext(), R.string.body_weight_must_be_positive, Toast.LENGTH_SHORT).show();
                    } else {
                        // Save the weight using our UserDataManager
                        userDataManager.saveBodyWeight(weight);
                        Toast.makeText(getContext(), getString(R.string.body_weight_saved), Toast.LENGTH_SHORT).show();
                        dismiss(); // Dismiss the dialog after saving
                        if (listener != null) {
                            listener.onBodyWeightEntered(weight);
                        }
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), R.string.invalid_body_weight_format, Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }
}
