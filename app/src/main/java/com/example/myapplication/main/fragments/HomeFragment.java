package com.example.myapplication.main.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.example.myapplication.R;
import com.example.myapplication.data.AppDatabase;
import com.example.myapplication.data.Quiz;
import com.example.myapplication.data.User;
import com.example.myapplication.main.MainActivity;
import com.example.myapplication.quiz.QuizActivity;

public class HomeFragment extends Fragment {
    
    private TextView welcomeText, tvAccuracy, tvQuizCount, tvAvgTime, tvStreak, tvSeeAll;
    private TextView[] tvHomeDays = new TextView[7];
    private Button btnCreate, btnLibrary;
    private LinearLayout recentlyAddedContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        welcomeText = view.findViewById(R.id.home_welcome_text);
        btnCreate = view.findViewById(R.id.btn_create_quiz);
        btnLibrary = view.findViewById(R.id.btn_library);
        tvSeeAll = view.findViewById(R.id.tv_see_all_library);
        
        tvAccuracy = view.findViewById(R.id.tv_accuracy);
        tvQuizCount = view.findViewById(R.id.tv_total_quizzes_stat);
        tvAvgTime = view.findViewById(R.id.tv_avg_time);
        tvStreak = view.findViewById(R.id.streak_count);
        recentlyAddedContainer = view.findViewById(R.id.recently_added_container);

        // Initialize day indicators
        tvHomeDays[0] = view.findViewById(R.id.tv_home_day_1);
        tvHomeDays[1] = view.findViewById(R.id.tv_home_day_2);
        tvHomeDays[2] = view.findViewById(R.id.tv_home_day_3);
        tvHomeDays[3] = view.findViewById(R.id.tv_home_day_4);
        tvHomeDays[4] = view.findViewById(R.id.tv_home_day_5);
        tvHomeDays[5] = view.findViewById(R.id.tv_home_day_6);
        tvHomeDays[6] = view.findViewById(R.id.tv_home_day_7);

        loadData();
        setupListeners();

        return view;
    }

    private void loadData() {
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        
        // 1. Welcome name
        String email = sharedPref.getString("CurrentUserEmail", "");
        User user = AppDatabase.getInstance(getContext()).userDao().getUserByEmail(email);
        String name = (user != null) ? user.getName() : "User";
        welcomeText.setText("Hello, " + name + "! 👋");

        // 2. Load Quiz Data and Stats from Room
        try {
            com.example.myapplication.data.QuizDao quizDao = AppDatabase.getInstance(getContext()).quizDao();
            List<Quiz> quizList = quizDao.getAllQuizzes(email);
            List<com.example.myapplication.data.QuizResult> results = quizDao.getAllResults(email);
            
            // Stats from QuizResult
            int quizLibraryCount = quizList.size();
            double avgAccuracy = quizDao.getAverageAccuracy(email);
            long avgTimeMillis = quizDao.getAverageTimeSpent(email);

            tvQuizCount.setText(String.valueOf(quizLibraryCount));
            tvAccuracy.setText(String.format(Locale.getDefault(), "%.0f%%", avgAccuracy));
            
            // Format time: mm:ss
            long totalSeconds = avgTimeMillis / 1000;
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            tvAvgTime.setText(String.format(Locale.getDefault(), "%dm %ds", minutes, seconds));
            
            // 3. Calculate Streak using StreakUtils
            int streak = com.example.myapplication.utils.StreakUtils.calculateCurrentStreak(results);
            tvStreak.setText(String.valueOf(streak));

            // 4. Update Weekly Status indicators
            boolean[] weeklyStatus = com.example.myapplication.utils.StreakUtils.getWeeklyStatus(results);
            for (int i = 0; i < 7; i++) {
                if (tvHomeDays[i] != null) {
                    if (weeklyStatus[i]) {
                        tvHomeDays[i].setBackgroundResource(R.drawable.bg_streak_circle);
                        tvHomeDays[i].setTextColor(getResources().getColor(R.color.white));
                        tvHomeDays[i].setText("✓");
                    } else {
                        tvHomeDays[i].setBackgroundResource(0); // No circle
                        tvHomeDays[i].setTextColor(getResources().getColor(R.color.on_surface_secondary));
                        tvHomeDays[i].setText(""); // Or maybe the day letter? Layout has the letter below.
                    }
                }
            }

            // 5. Load Recently Added (3 most recent)
            loadRecentlyAddedQuizzes(quizList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRecentlyAddedQuizzes(List<Quiz> quizList) {
        if (recentlyAddedContainer == null || getContext() == null) return;
        recentlyAddedContainer.removeAllViews();

        int maxLimit = Math.min(3, quizList.size());
        try {
            for (int i = 0; i < maxLimit; i++) {
                addRecentQuizCard(quizList.get(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addRecentQuizCard(Quiz quiz) {
        View card = LayoutInflater.from(getContext()).inflate(R.layout.item_home_recent_quiz, recentlyAddedContainer, false);
        
        TextView title = card.findViewById(R.id.tv_title);
        TextView count = card.findViewById(R.id.tv_questions_count);
        ProgressBar progress = card.findViewById(R.id.progress_bar);
        TextView percent = card.findViewById(R.id.tv_progress_percent);
        
        title.setText(quiz.getTitle());
        
        int countInt = 0;
        try {
            countInt = Integer.parseInt(quiz.getCount());
        } catch (NumberFormatException e) {
            // Fallback
        }
        count.setText(getString(R.string.questions_count_format, countInt));
        
        card.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), QuizActivity.class);
            intent.putExtra("QUIZ_ID", quiz.getId());
            intent.putExtra("QUIZ_DATA", quiz.getJsonData());
            intent.putExtra("QUIZ_TITLE", quiz.getTitle());
            startActivity(intent);
        });

        int p = (int) (Math.random() * 100);
        progress.setProgress(p);
        percent.setText(p + "%");

        recentlyAddedContainer.addView(card);
    }

    private void setupListeners() {
        btnCreate.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToTab(1); // Create tab
            }
        });
        
        btnLibrary.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToTab(2); // Library tab
            }
        });

        tvSeeAll.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToTab(2); // Library tab
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }
}
