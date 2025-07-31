package com.almogbb.crimpi.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.almogbb.crimpi.R;
import com.almogbb.crimpi.workouts.CustomWorkoutData;
import com.almogbb.crimpi.workouts.Exercise;
import com.almogbb.crimpi.workouts.WorkoutSet;

import java.util.List;

public class WorkoutsAdapter extends RecyclerView.Adapter<WorkoutsAdapter.WorkoutViewHolder> {

    private List<CustomWorkoutData> workouts;
    private OnItemClickListener listener;
    private int expandedPosition = -1; // To keep track of the currently expanded item

    // Interface for click events
    public interface OnItemClickListener {
        void onItemClick(CustomWorkoutData workoutData); // For the entire item click (expand/collapse)
        void onGoButtonClick(CustomWorkoutData workoutData); // For the "GO!" button click
    }

    public WorkoutsAdapter(List<CustomWorkoutData> workouts, OnItemClickListener listener) {
        this.workouts = workouts;
        this.listener = listener;
    }

    // Method to update the data in the adapter
    public void updateWorkouts(List<CustomWorkoutData> newWorkouts) {
        this.workouts = newWorkouts;
        notifyDataSetChanged(); // Notify RecyclerView that data has changed
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout, parent, false);
        return new WorkoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        CustomWorkoutData currentWorkout = workouts.get(position);

        // Bind data to the collapsed view elements
        holder.workoutNameTextView.setText(currentWorkout.getName());

        // Format duration (e.g., 600 seconds -> "10m")
        holder.descriptionTextView.setText(String.format(currentWorkout.getDescription()));

        // Handle expansion/collapse
        final boolean isExpanded = position == expandedPosition;
        holder.expandedDetailsLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // Clear previous exercise views and add new ones if expanded
        holder.exercisesContainerLayout.removeAllViews(); // Clear existing views
        if (isExpanded) {
            // Iterate through each WorkoutSet
            for (WorkoutSet workoutSet : currentWorkout.getWorkoutSets()) {
                // Iterate through each Exercise within the WorkoutSet
                for (Exercise exercise : workoutSet.getExercises()) {
                    TextView exerciseTextView = new TextView(holder.itemView.getContext());
                    String exerciseText = exercise.getDescription();
                    if (exercise.getRepetitions() > 0) {
                        exerciseText += String.format(" x %d", exercise.getRepetitions());
                    }

                    exerciseTextView.setText(exerciseText);
                    exerciseTextView.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
                    exerciseTextView.setTextSize(16); // Match the tools:text size in XML
                    // Add some margin between exercises if needed
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 4, 0, 0); // Top margin for spacing
                    exerciseTextView.setLayoutParams(params);
                    holder.exercisesContainerLayout.addView(exerciseTextView);
                }
                // Optionally, add a separator or rest indicator between sets if desired
                // For now, just adding exercises
            }
        }

        // Set click listener for the entire item to toggle expansion
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int previousExpandedPosition = expandedPosition;
                if (isExpanded) {
                    expandedPosition = -1; // Collapse this item
                } else {
                    expandedPosition = holder.getAdapterPosition(); // Expand this item
                }
                notifyItemChanged(holder.getAdapterPosition()); // Notify this item changed
                if (previousExpandedPosition != -1 && previousExpandedPosition != expandedPosition) {
                    notifyItemChanged(previousExpandedPosition); // Notify previously expanded item changed
                }
                listener.onItemClick(currentWorkout); // Also notify the fragment about the item click
            }
        });

        // Set click listener for the "GO!" button
        holder.goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onGoButtonClick(currentWorkout);
            }
        });
    }

    @Override
    public int getItemCount() {
        return workouts.size();
    }

    // ViewHolder class to hold references to the views in item_workout.xml
    public static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        TextView workoutNameTextView;
        TextView descriptionTextView;
        TextView setsTextView;
        LinearLayout expandedDetailsLayout;
        LinearLayout exercisesContainerLayout; // Container for dynamically added exercise TextViews
        Button goButton;

        public WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            workoutNameTextView = itemView.findViewById(R.id.text_view_workout_name);
            descriptionTextView = itemView.findViewById(R.id.text_view_duration);
            setsTextView = itemView.findViewById(R.id.text_view_sets);
            expandedDetailsLayout = itemView.findViewById(R.id.layout_expanded_details);
            exercisesContainerLayout = itemView.findViewById(R.id.layout_exercises_container);
            goButton = itemView.findViewById(R.id.button_go);
        }
    }
}
