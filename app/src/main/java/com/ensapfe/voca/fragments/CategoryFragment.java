package com.ensapfe.voca.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ensapfe.voca.R;
import com.ensapfe.voca.adapters.CategoryAdapter;
import com.ensapfe.voca.models.Category;
import com.ensapfe.voca.utils.QuizData;
import java.util.List;

public class CategoryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        RecyclerView categoriesRecyclerView = view.findViewById(R.id.categories_recycler);
        categoriesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        List<Category> categories = QuizData.getAllCategoriesWithQuestions();

        CategoryAdapter adapter = new CategoryAdapter(categories, category -> {
            QuizFragment quizFragment = new QuizFragment();
            Bundle args = new Bundle();
            args.putString("categoryId", category.getId());
            quizFragment.setArguments(args);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, quizFragment)
                    .addToBackStack(null)
                    .commit();
        });

        categoriesRecyclerView.setAdapter(adapter);
        return view;
    }
}