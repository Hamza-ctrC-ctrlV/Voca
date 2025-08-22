package com.ensapfe.voca.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.ensapfe.voca.R;

public class ResultsDialogFragment extends DialogFragment {

    private static final String ARG_SCORE = "score";
    private static final String ARG_TOTAL = "total";

    public static ResultsDialogFragment newInstance(int score, int total) {
        ResultsDialogFragment fragment = new ResultsDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SCORE, score);
        args.putInt(ARG_TOTAL, total);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_results, null);

        Bundle args = getArguments();
        int score = args != null ? args.getInt(ARG_SCORE, 0) : 0;
        int total = args != null ? args.getInt(ARG_TOTAL, 0) : 0;

        TextView scoreText = view.findViewById(R.id.result_score);
        TextView messageText = view.findViewById(R.id.result_message);
        Button btnDone = view.findViewById(R.id.btn_done);
        Button btnRetry = view.findViewById(R.id.btn_retry);

        scoreText.setText(String.format("%d/%d", score, total));

        double percentage = (double) score / total * 100;
        if (percentage >= 80) {
            messageText.setText("Excellent! You're a master!");
        } else if (percentage >= 60) {
            messageText.setText("Good job! You're getting better!");
        } else {
            messageText.setText("Keep practicing to improve!");
        }

        builder.setView(view);
        AlertDialog dialog = builder.create();

        btnDone.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
            dismiss();
        });

        btnRetry.setOnClickListener(v -> {
            QuizFragment quizFragment = new QuizFragment();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, quizFragment)
                    .commit();
            dismiss();
        });

        return dialog;
    }
}