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

    private TextView tvTotalQuizzes, tvEmptyState;
    private LinearLayout quizListContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        tvTotalQuizzes = view.findViewById(R.id.tv_total_quizzes);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        quizListContainer = view.findViewById(R.id.quiz_list_container);

        loadQuizzes();

        return view;
    }

    private void loadQuizzes() {
        if (getContext() == null) return;
        
        quizListContainer.removeAllViews();
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String quizzesJson = sharedPref.getString("QuizList", "[]");

        try {
            JSONArray quizArray = new JSONArray(quizzesJson);
            int count = quizArray.length();

            if (count == 0) {
                tvEmptyState.setVisibility(View.VISIBLE);
                tvTotalQuizzes.setText("0 TỔNG SỐ");
            } else {
                tvEmptyState.setVisibility(View.GONE);
                tvTotalQuizzes.setText(count + " TỔNG SỐ");

                for (int i = 0; i < count; i++) {
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
        
        TextView tvTitle = cardView.findViewById(R.id.item_title);
        TextView tvDate = cardView.findViewById(R.id.item_date);
        TextView tvCount = cardView.findViewById(R.id.item_count);

        tvTitle.setText(title);
        tvDate.setText("Đã tạo " + date);
        tvCount.setText(qCount);

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