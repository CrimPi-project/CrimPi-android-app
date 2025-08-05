package com.almogbb.crimpi.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast; // Using Toast for simple feedback

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.almogbb.crimpi.R;
import com.almogbb.crimpi.workouts.CustomWorkoutData;
import com.almogbb.crimpi.workouts.Exercise;
import com.almogbb.crimpi.workouts.WorkoutSet;
import com.google.android.material.floatingactionbutton.FloatingActionButton; // New import for FAB
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays; // For creating single WorkoutSet
import java.util.List;
import java.util.UUID; // For generating unique IDs

public class AddWorkoutDialogFragment extends DialogFragment {

    private TextInputEditText editTextWorkoutName;
    private TextInputEditText editTextWorkoutDescription;

    private TextInputEditText editTextRestBetweenRepetitions;
    private TextInputEditText editTextRepetitionDuration;
    private TextInputEditText editTextRestBetweenSets;
    private LinearLayout layoutExercisesContainerMain; // Main container for all exercise lines

    // Listener to communicate back to MyWorkoutsFragment
    public interface AddWorkoutDialogListener {
        void onWorkoutSaved(CustomWorkoutData newWorkout);
    }

    private AddWorkoutDialogListener listener;

    // This method ensures the listener is set correctly
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (AddWorkoutDialogListener) getTargetFragment(); // Use getTargetFragment if setting target
            if (listener == null) {
                listener = (AddWorkoutDialogListener) context; // Fallback to context if not set as target
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement AddWorkoutDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity(), R.style.AlertDialogTransparent); // Use a dialog theme
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_workout, null);
        builder.setView(view);

        // Initialize views
        editTextWorkoutName = view.findViewById(R.id.edit_text_workout_name);
        editTextWorkoutDescription = view.findViewById(R.id.edit_text_workout_description);
        editTextRestBetweenRepetitions = view.findViewById(R.id.edit_text_rest_between_repetitions);
        editTextRestBetweenSets = view.findViewById(R.id.edit_text_rest_between_sets);
        editTextRepetitionDuration = view.findViewById(R.id.edit_text_repetition_duration);
        layoutExercisesContainerMain = view.findViewById(R.id.layout_exercises_container_main); // New ID
        // The new mini plus FAB
        FloatingActionButton fabAddExerciseMain = view.findViewById(R.id.fab_add_exercise_main); // New ID
        // The "Add" button at the bottom
        Button buttonAdd = view.findViewById(R.id.button_add); // New ID for the main "Add" button

        // Set up listeners
        fabAddExerciseMain.setOnClickListener(v -> addExerciseView(layoutExercisesContainerMain)); // Add exercise directly
        buttonAdd.setOnClickListener(v -> saveWorkout()); // Save workout on "Add" button click

        // Add an initial empty exercise line when the dialog opens
        addExerciseView(layoutExercisesContainerMain);

        return builder.create();
    }

    // Helper method to add a new exercise UI block
    private void addExerciseView(LinearLayout parentLayout) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View exerciseView = inflater.inflate(R.layout.item_add_exercise, parentLayout, false);

        Button buttonRemoveExercise = exerciseView.findViewById(R.id.button_remove_exercise);
        buttonRemoveExercise.setOnClickListener(v -> {
            // Only allow removing if there's more than one exercise
            if (parentLayout.getChildCount() > 1) {
                parentLayout.removeView(exerciseView);
            } else {
                Toast.makeText(getContext(), "A workout must have at least one exercise.", Toast.LENGTH_SHORT).show();
            }
        });

        parentLayout.addView(exerciseView);
    }

    private void saveWorkout() {
        String workoutName = editTextWorkoutName.getText().toString().trim();
        String workoutDescription = editTextWorkoutDescription.getText().toString().trim();
        String restsBetweenRepetitionsStr = editTextRestBetweenRepetitions.getText().toString().trim();
        String restsBetweenSetsStr = editTextRestBetweenSets.getText().toString().trim();
        String repetitionDurationStr = editTextRepetitionDuration.getText().toString().trim();

        int restAfterRepetitionsSeconds = Integer.parseInt(restsBetweenRepetitionsStr); // Default value
        int restAfterSetSeconds = Integer.parseInt(restsBetweenSetsStr); // Default value
        int repetitionDurationSeconds = Integer.parseInt(repetitionDurationStr); // Default value

        if (workoutName.isEmpty()) {
            Toast.makeText(getContext(), "Workout name cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (restsBetweenRepetitionsStr.isEmpty() || restsBetweenSetsStr.isEmpty()){
            Toast.makeText(getContext(), "Please specify rest time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (repetitionDurationStr.isEmpty()){
            Toast.makeText(getContext(), "Please specify repetition duration", Toast.LENGTH_SHORT).show();
            return;
        }


        List<Exercise> exercises = new ArrayList<>();
        // Iterate through each dynamically added exercise view
        for (int i = 0; i < layoutExercisesContainerMain.getChildCount(); i++) {
            View exerciseView = layoutExercisesContainerMain.getChildAt(i);
            TextInputEditText editTextExerciseDescription = exerciseView.findViewById(R.id.edit_text_exercise_description);
            TextInputEditText editTextRepetitions = exerciseView.findViewById(R.id.edit_text_repetitions);
            TextInputEditText editTextMinBodyPercentage = exerciseView.findViewById(R.id.edit_text_min_body_percentage); // New ID

            String exerciseDescription = editTextExerciseDescription.getText().toString().trim();
            int repetitions = 0;
            int minBodyPercentage = 0; // Default to 0

            if (exerciseDescription.isEmpty()) {
                Toast.makeText(getContext(), "Exercise description cannot be empty for exercise " + (i + 1), Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                String repsText = editTextRepetitions.getText().toString().trim();
                if (!repsText.isEmpty()) {
                    repetitions = Integer.parseInt(repsText);
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid repetitions for exercise " + (i + 1), Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                String minBodyText = editTextMinBodyPercentage.getText().toString().trim();
                if (!minBodyText.isEmpty()) {
                    minBodyPercentage = Integer.parseInt(minBodyText);
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid Min Body % for exercise " + (i + 1), Toast.LENGTH_SHORT).show();
                return;
            }

            exercises.add(new Exercise(exerciseDescription, repetitions, minBodyPercentage)); // DurationSeconds set to 0 as per new design
        }

        if (exercises.isEmpty()) {
            Toast.makeText(getContext(), "Please add at least one exercise.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Wrap all exercises into a single WorkoutSet for compatibility with CustomWorkoutData
        // The restAfterSetSeconds for this single set can be a default or user-defined if you add a field for it later.
        // For now, let's use a default rest of 60 seconds after this "super set" of exercises.
        WorkoutSet workoutSets = new WorkoutSet(exercises);

        // Create the new CustomWorkoutData object
        CustomWorkoutData newWorkout = new CustomWorkoutData(
                UUID.randomUUID().toString(), // Generate a unique ID
                workoutName,
                workoutDescription,
                repetitionDurationSeconds, // Use the estimated duration
                1, // Only 1 "set" in terms of WorkoutSet objects for this simplified input
                workoutSets, // Wrap the single set in a list
                restAfterRepetitionsSeconds,
                restAfterSetSeconds
        );

        // Notify the listener (MyWorkoutsFragment) that a new workout has been saved
        if (listener != null) {
            listener.onWorkoutSaved(newWorkout);
        }

        dismiss(); // Close the dialog
    }
}
