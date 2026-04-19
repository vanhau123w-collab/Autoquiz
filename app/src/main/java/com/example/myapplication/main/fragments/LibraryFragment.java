package com.example.myapplication.main.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.List;

import com.example.myapplication.R;
import com.example.myapplication.data.AppDatabase;
import com.example.myapplication.data.Quiz;
import com.example.myapplication.quiz.QuizActivity;

public class LibraryFragment extends Fragment {

    private TextView tvTotalQuizzes;
    private View emptyState;
    private LinearLayout quizListContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        tvTotalQuizzes = view.findViewById(R.id.tv_quiz_count);
        emptyState = view.findViewById(R.id.empty_state);
        quizListContainer = view.findViewById(R.id.library_container);

        loadQuizzes();

        return view;
    }

    private void loadQuizzes() {
        if (getContext() == null || quizListContainer == null) return;
        
        quizListContainer.removeAllViews();
        
        try {
            List<Quiz> quizList = AppDatabase.getInstance(getContext()).quizDao().getAllQuizzes();
            int count = quizList.size();

            if (count == 0) {
                if (emptyState.getParent() != null) {
                    ((ViewGroup) emptyState.getParent()).removeView(emptyState);
                }
                quizListContainer.addView(emptyState);
                emptyState.setVisibility(View.VISIBLE);
                tvTotalQuizzes.setText(getString(R.string.saved_quizzes_format, 0));
            } else {
                tvTotalQuizzes.setText(getString(R.string.saved_quizzes_format, count));
                emptyState.setVisibility(View.GONE);

                for (Quiz quiz : quizList) {
                    addQuizCard(quiz);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addQuizCard(Quiz quiz) {
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.item_quiz, quizListContainer, false);
        
        TextView tvTitle = cardView.findViewById(R.id.tv_title);
        TextView tvDate = cardView.findViewById(R.id.tv_date);
        TextView tvCount = cardView.findViewById(R.id.tv_question_count);
        View btnDelete = cardView.findViewById(R.id.btn_delete);

        tvTitle.setText(quiz.getTitle());
        tvDate.setText(quiz.getDate());
        
        int countInt = 0;
        try {
            countInt = Integer.parseInt(quiz.getCount());
        } catch (NumberFormatException e) {
            // Fallback for safety
        }
        tvCount.setText(getString(R.string.questions_count_format, countInt));

        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), QuizActivity.class);
            intent.putExtra("QUIZ_ID", quiz.getId());
            intent.putExtra("QUIZ_DATA", quiz.getJsonData());
            intent.putExtra("QUIZ_TITLE", quiz.getTitle());
            startActivity(intent);
        });
        
        btnDelete.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_quiz))
                .setMessage(getString(R.string.delete_quiz_msg))
                .setPositiveButton(getString(R.string.delete_confirm), (dialog, which) -> deleteQuiz(quiz))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
        });

        quizListContainer.addView(cardView);
    }

    private void deleteQuiz(Quiz quiz) {
        try {
            AppDatabase.getInstance(getContext()).quizDao().delete(quiz);
            loadQuizzes(); // Refresh list
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadQuizzes();
    }
}
