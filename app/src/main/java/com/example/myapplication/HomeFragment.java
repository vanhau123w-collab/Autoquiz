package com.example.myapplication;

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
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends Fragment {
    
    private TextView welcomeText, tvAccuracy, tvQuizCount, tvAvgTime, tvStreak;
    private Button btnCreate, btnLibrary;
    private LinearLayout recentlyAddedContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        welcomeText = view.findViewById(R.id.home_welcome_text);
        btnCreate = view.findViewById(R.id.btn_create_quiz);
        btnLibrary = view.findViewById(R.id.btn_library);
        
        tvAccuracy = view.findViewById(R.id.tv_accuracy);
        tvQuizCount = view.findViewById(R.id.tv_total_quizzes_stat);
        tvAvgTime = view.findViewById(R.id.tv_avg_time);
        tvStreak = view.findViewById(R.id.streak_count);
        recentlyAddedContainer = view.findViewById(R.id.recently_added_container);

        loadData();
        setupListeners();

        return view;
    }

    private void loadData() {
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        
        // 1. Welcome name
        String name = sharedPref.getString("UserName", "User");
        welcomeText.setText("Hello, " + name + "! 👋");

        // 2. Load Quiz Data
        String quizzesJson = sharedPref.getString("QuizList", "[]");
        try {
            JSONArray quizArray = new JSONArray(quizzesJson);
            int count = quizArray.length();
            
            // Update Stats
            tvQuizCount.setText(String.valueOf(count));
            
            // Calculate Streak
            int streak = calculateStreak(quizArray);
            tvStreak.setText(String.valueOf(streak));

            // Load Recently Added (3 most recent)
            loadRecentlyAddedQuizzes(quizArray);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Keep simulated stats for accuracy and time for now
        tvAccuracy.setText("92%");
        tvAvgTime.setText("3m 45s");
    }

    private int calculateStreak(JSONArray quizArray) {
        if (quizArray.length() == 0) return 0;
        
        Set<String> activeDates = new HashSet<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        
        try {
            for (int i = 0; i < quizArray.length(); i++) {
                activeDates.add(quizArray.getJSONObject(i).getString("date"));
            }
        } catch (Exception e) {
            return 0;
        }

        int streak = 0;
        long oneDayMs = 24 * 60 * 60 * 1000;
        Date today = new Date();
        
        // Check backwards from today
        while (true) {
            String checkDate = dateFormat.format(new Date(today.getTime() - (streak * oneDayMs)));
            if (activeDates.contains(checkDate)) {
                streak++;
            } else {
                // If we didn't find today, check if yesterday exists. If not, streak is 0.
                if (streak == 0) {
                    // Check yesterday
                    String yesterday = dateFormat.format(new Date(today.getTime() - oneDayMs));
                    if (!activeDates.contains(yesterday)) {
                        return 0;
                    }
                    // If yesterday exists, streak calculation will catch it in next loop or we can just let it continue
                } else {
                    break;
                }
                
                // Safety break for today not being in yet
                if (streak == 0) {
                    String todayStr = dateFormat.format(today);
                    if (!activeDates.contains(todayStr)) {
                        // Check if we started yesterday
                        String yesterdayStr = dateFormat.format(new Date(today.getTime() - oneDayMs));
                        if (activeDates.contains(yesterdayStr)) {
                            // Continue from yesterday
                            today = new Date(today.getTime() - oneDayMs);
                            continue;
                        } else {
                            return 0;
                        }
                    }
                }
                break;
            }
            if (streak > 365) break; // Safety
        }
        
        return streak;
    }

    private void loadRecentlyAddedQuizzes(JSONArray quizArray) {
        if (recentlyAddedContainer == null || getContext() == null) return;
        recentlyAddedContainer.removeAllViews();

        int maxLimit = Math.min(3, quizArray.length());
        try {
            for (int i = quizArray.length() - 1; i >= quizArray.length() - maxLimit; i--) {
                JSONObject quizObj = quizArray.getJSONObject(i);
                addRecentQuizCard(quizObj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addRecentQuizCard(JSONObject quiz) throws Exception {
        View card = LayoutInflater.from(getContext()).inflate(R.layout.item_home_recent_quiz, recentlyAddedContainer, false);
        
        TextView title = card.findViewById(R.id.tv_title);
        TextView count = card.findViewById(R.id.tv_questions_count);
        ProgressBar progress = card.findViewById(R.id.progress_bar);
        TextView percent = card.findViewById(R.id.tv_progress_percent);
        
        title.setText(quiz.getString("title"));
        count.setText(quiz.getString("count") + " questions");
        
        final String rawData = quiz.getString("raw_data");
        final String quizTitle = quiz.getString("title");
        card.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), QuizActivity.class);
            intent.putExtra("QUIZ_DATA", rawData);
            intent.putExtra("QUIZ_TITLE", quizTitle);
            startActivity(intent);
        });

        // Simulating some progress if not available
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
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }
}