package com.almogbb.crimpi.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.almogbb.crimpi.R; // Make sure this R is correctly imported

public class DeleteConfirmationDialogFragment extends DialogFragment {

    private static final String ARG_POSITION = "position";

    // Listener interface to communicate back to the calling fragment
    public interface DeleteDialogListener {
        void onDeleteConfirmed(int position);
        void onDeleteCancelled(int position);
    }

    private DeleteDialogListener listener;
    private int itemPosition;

    // Factory method to create an instance of the dialog and pass arguments
    public static DeleteConfirmationDialogFragment newInstance(int position) {
        DeleteConfirmationDialogFragment fragment = new DeleteConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            itemPosition = getArguments().getInt(ARG_POSITION);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Ensure the hosting fragment implements the listener interface
        try {
            listener = (DeleteDialogListener) getTargetFragment();
            if (listener == null) {
                // Fallback to context if target fragment is not set (e.g., dialog shown from activity)
                listener = (DeleteDialogListener) context;
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement DeleteDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity(),R.style.AlertDialogTransparent);
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_delete_workout, null);

        Button noButton = view.findViewById(R.id.dialogDeleteTitleButtonNo);
        Button yesButton = view.findViewById(R.id.dialogDeleteTitleButtonYes);

        // Set up button listeners
        noButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteCancelled(itemPosition); // Notify cancellation
            }
            dismiss(); // Close the dialog
        });

        yesButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteConfirmed(itemPosition); // Notify confirmation
            }
            dismiss(); // Close the dialog
        });

        builder.setView(view);
        // Important: Prevent dialog from being dismissed by tapping outside or back button
        setCancelable(false);

        return builder.create();
    }
}

