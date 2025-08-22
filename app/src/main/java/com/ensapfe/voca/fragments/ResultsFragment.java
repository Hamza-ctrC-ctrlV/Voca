package com.ensapfe.voca.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.ensapfe.voca.R;

public class ResultsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_results, container, false);

        // Get results from arguments
        Bundle args = getArguments();
        int score = args != null ? args.getInt("score", 0) : 0;
        int total = args != null ? args.getInt("total", 0) : 0;

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

        btnDone.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        btnRetry.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new QuizFragment())
                    .commit();
        });

        return view;
    }
}