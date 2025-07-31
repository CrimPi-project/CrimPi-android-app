package com.almogbb.crimpi.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.almogbb.crimpi.MainActivity;
import com.almogbb.crimpi.R;
import com.almogbb.crimpi.adapters.WorkoutsAdapter;
import com.almogbb.crimpi.workouts.CustomWorkoutData;
import com.almogbb.crimpi.workouts.Exercise;
import com.almogbb.crimpi.workouts.WorkoutSet;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.almogbb.crimpi.utils.WorkoutJsonManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// MyWorkoutsFragment now implements AddWorkoutDialogListener AND DeleteDialogListener
public class MyWorkoutsFragment extends Fragment implements
        AddWorkoutDialogFragment.AddWorkoutDialogListener,
        DeleteConfirmationDialogFragment.DeleteDialogListener { // NEW: Implement DeleteDialogListener

    private RecyclerView workoutsRecyclerView;
    private WorkoutsAdapter workoutsAdapter;
    private List<CustomWorkoutData> workoutsList;
    private WorkoutJsonManager workoutJsonManager;

    private static final String ADD_WORKOUT_DIALOG_TAG = "AddWorkoutDialog";
    private static final String DELETE_CONFIRMATION_DIALOG_TAG = "DeleteConfirmationDialog"; // NEW: Tag for delete dialog

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_workouts, container, false);

        workoutsRecyclerView = view.findViewById(R.id.workouts_recycler_view);
        FloatingActionButton newWorkoutFab = view.findViewById(R.id.new_workout_fab);

        workoutsList = new ArrayList<>();
        workoutJsonManager = new WorkoutJsonManager(requireContext());

        workoutsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        workoutsAdapter = new WorkoutsAdapter(workoutsList, new WorkoutsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(CustomWorkoutData workoutData) {
                System.out.println("Workout item clicked: " + workoutData.getName());
            }

            @Override
            public void onGoButtonClick(CustomWorkoutData workoutData) {
                System.out.println("GO! button clicked for workout: " + workoutData.getName());
                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    // Create a new instance of CustomWorkoutFragment and pass the workoutData
                    CustomWorkoutFragment customWorkoutFragment = CustomWorkoutFragment.newInstance(workoutData);
                    mainActivity.loadFragment(customWorkoutFragment);
                }
            }
        });
        workoutsRecyclerView.setAdapter(workoutsAdapter);
        newWorkoutFab.setOnClickListener(v -> showAddWorkoutDialog());
        setupSwipeToDelete();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadWorkoutsFromJson();
    }

    private void showAddWorkoutDialog() {
        FragmentManager fm = getParentFragmentManager();
        AddWorkoutDialogFragment dialog = new AddWorkoutDialogFragment();
        dialog.setTargetFragment(MyWorkoutsFragment.this, 0);
        dialog.show(fm, ADD_WORKOUT_DIALOG_TAG);
    }

    @Override
    public void onWorkoutSaved(CustomWorkoutData newWorkout) {
        System.out.println("New workout received: " + newWorkout.getName());
        workoutsList.add(newWorkout);
        workoutsAdapter.updateWorkouts(workoutsList);
        workoutJsonManager.saveWorkouts(workoutsList);
    }

    private void loadWorkoutsFromJson() {
        List<CustomWorkoutData> loadedWorkouts = workoutJsonManager.loadWorkouts();
        workoutsList.clear();
        workoutsList.addAll(loadedWorkouts);
        workoutsAdapter.updateWorkouts(workoutsList);

        if (workoutsList.isEmpty()) {
            loadDummyWorkouts();
        }
    }

    private void loadDummyWorkouts() {
        List<CustomWorkoutData> dummyWorkouts = new ArrayList<>();

        // Workout 1
        List<WorkoutSet> workoutExample = new ArrayList<>();
        workoutExample.add(new WorkoutSet(
                Arrays.asList(
                        new Exercise("Exercise 1", 3,  0),
                        new Exercise("Exercise 2", 3, 0)
                ),
                60
        ));
        dummyWorkouts.add(new CustomWorkoutData(
                "example",
                "Workout Example",
                "A basic strength workout.",
                600,
                2,
                workoutExample
        ));

        // Add dummy workouts if the list is empty (i.e., no workouts loaded from JSON)
        if (workoutsList.isEmpty()) {
            workoutsList.addAll(dummyWorkouts);
            workoutJsonManager.saveWorkouts(workoutsList); // Save these initial dummy workouts
        }
        workoutsAdapter.updateWorkouts(workoutsList);
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) {
                    return;
                }

                // Show the custom delete confirmation dialog
                DeleteConfirmationDialogFragment dialog = DeleteConfirmationDialogFragment.newInstance(position);
                dialog.setTargetFragment(MyWorkoutsFragment.this, 0); // Set this fragment as the target
                dialog.show(getParentFragmentManager(), DELETE_CONFIRMATION_DIALOG_TAG);
            }
        };

        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(workoutsRecyclerView);
    }

    // NEW: Implementation of DeleteDialogListener methods
    @Override
    public void onDeleteConfirmed(int position) {
        // User confirmed deletion, proceed with removal
        CustomWorkoutData deletedWorkout = workoutsList.remove(position);
        workoutsAdapter.notifyItemRemoved(position);
        workoutJsonManager.saveWorkouts(workoutsList); // Save updated list to JSON
        System.out.println("Workout deleted: " + deletedWorkout.getName());
    }

    @Override
    public void onDeleteCancelled(int position) {
        // User cancelled, revert the swipe animation
        workoutsAdapter.notifyItemChanged(position);
    }
}
