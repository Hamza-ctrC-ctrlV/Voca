package com.ensapfe.voca.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.ensapfe.voca.R;
import com.ensapfe.voca.models.Question;
import com.ensapfe.voca.utils.QuizData;
import java.util.List;
import com.ensapfe.voca.models.Category;

public class QuizFragment extends Fragment {

    private TextView questionText;
    private RadioGroup optionsGroup;
    private Button nextButton;
    private ProgressBar progressBar;
    private TextView progressText, scoreText;

    private List<Question> questions;
    private int currentIndex = 0;
    private int score = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quiz, container, false);

        questionText = view.findViewById(R.id.question_text);
        optionsGroup = view.findViewById(R.id.options_group);
        nextButton = view.findViewById(R.id.next_button);
        progressBar = view.findViewById(R.id.progress_bar);
        progressText = view.findViewById(R.id.progress_text);
        scoreText = view.findViewById(R.id.score_text);

        if (getArguments() != null) {
            String categoryId = getArguments().getString("categoryId");
            questions = getQuestionsForCategory(categoryId);
        }

        if (questions == null || questions.isEmpty()) {
            Toast.makeText(getContext(), "No questions found for this category", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return view;
        }

        updateQuestion();

        nextButton.setOnClickListener(v -> {
            if (optionsGroup.getCheckedRadioButtonId() == -1) {
                Toast.makeText(getContext(), "Please select an answer", Toast.LENGTH_SHORT).show();
                return;
            }
            checkAnswer();
        });

        return view;
    }

    private List<Question> getQuestionsForCategory(String categoryId) {
        for (Category category : QuizData.getAllCategoriesWithQuestions()) {
            if (category.getId().equals(categoryId)) {
                return category.getQuestions();
            }
        }
        return null;
    }

    private void updateQuestion() {
        if (currentIndex < questions.size()) {
            Question current = questions.get(currentIndex);
            questionText.setText(current.getQuestion());

            optionsGroup.removeAllViews();
            for (String option : current.getOptions()) {
                RadioButton radioButton = new RadioButton(getContext());
                radioButton.setText(option);
                radioButton.setId(View.generateViewId());
                optionsGroup.addView(radioButton);
            }

            progressText.setText((currentIndex + 1) + "/" + questions.size());
            progressBar.setMax(questions.size());
            progressBar.setProgress(currentIndex + 1);
            scoreText.setText("Score: " + score);
        } else {
            showResults();
        }
    }

    private void checkAnswer() {
        int selectedId = optionsGroup.getCheckedRadioButtonId();
        RadioButton selected = optionsGroup.findViewById(selectedId);
        int selectedIndex = optionsGroup.indexOfChild(selected);

        Question currentQuestion = questions.get(currentIndex);
        if (selectedIndex == currentQuestion.getAnswerIndex()) {
            score++;
        }

        currentIndex++;
        updateQuestion();
    }

    private void showResults() {
        ResultsDialogFragment dialog = ResultsDialogFragment.newInstance(score, questions.size());
        dialog.show(getParentFragmentManager(), "ResultsDialog");
    }
}