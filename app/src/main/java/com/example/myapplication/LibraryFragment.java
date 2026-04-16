package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.json.JSONArray;
import org.json.JSONObject;

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
        // Keep emptyState logic in mind if it's placed inside library_container or outside
        // Wait, in my fragment_library.xml, empty_state is inside library_container.
        // So I must re-add empty_state if count == 0.
        
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String quizzesJson = sharedPref.getString("QuizList", "[]");

        try {
            JSONArray quizArray = new JSONArray(quizzesJson);
            int count = quizArray.length();

            if (count == 0) {
                if (emptyState.getParent() != null) {
                    ((ViewGroup) emptyState.getParent()).removeView(emptyState);
                }
                quizListContainer.addView(emptyState);
                emptyState.setVisibility(View.VISIBLE);
                tvTotalQuizzes.setText("0 saved quizzes");
            } else {
                tvTotalQuizzes.setText(count + " saved quizzes");

                for (int i = quizArray.length() - 1; i >= 0; i--) {
                    JSONObject quizObj = quizArray.getJSONObject(i);
                    addQuizCard(quizObj.getString("title"), quizObj.getString("date"), quizObj.getString("count"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addQuizCard(String title, String date, String qCount) {
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.item_quiz, quizListContainer, false);
        
        TextView tvTitle = cardView.findViewById(R.id.tv_title);
        TextView tvDate = cardView.findViewById(R.id.tv_date);
        TextView tvCount = cardView.findViewById(R.id.tv_question_count);

        tvTitle.setText(title);
        tvDate.setText(date);
        tvCount.setText(qCount + " questions");

        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), QuizActivity.class);
            startActivity(intent);
        });

        quizListContainer.addView(cardView);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadQuizzes();
    }
}