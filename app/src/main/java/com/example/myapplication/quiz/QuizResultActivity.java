package com.example.myapplication.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.data.AppDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

public class QuizResultActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.example.myapplication.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);

        int correct = getIntent().getIntExtra("CORRECT_ANSWERS", 0);
        int total = getIntent().getIntExtra("TOTAL_QUESTIONS", 0);
        long timeMillis = getIntent().getLongExtra("TIME_SPENT", 0);
        int quizId = getIntent().getIntExtra("QUIZ_ID", -1);

        TextView tvEmoji = findViewById(R.id.tv_result_emoji);
        TextView tvScore = findViewById(R.id.tv_result_score);
        TextView tvTime = findViewById(R.id.tv_result_time);
        TextView tvAccuracy = findViewById(R.id.tv_result_accuracy);
        Button btnDone = findViewById(R.id.btn_done);

        saveResultToDatabase(quizId, correct, total, timeMillis);

        tvScore.setText(correct + "/" + total);

        // Detailed stats
        int easyCount = getIntent().getIntExtra("EASY_COUNT", 0);
        int medCount = getIntent().getIntExtra("MEDIUM_COUNT", 0);
        int hardCount = getIntent().getIntExtra("HARD_COUNT", 0);
        int easyCorrect = getIntent().getIntExtra("EASY_CORRECT", 0);
        int medCorrect = getIntent().getIntExtra("MEDIUM_CORRECT", 0);
        int hardCorrect = getIntent().getIntExtra("HARD_CORRECT", 0);

        ((TextView) findViewById(R.id.tv_stat_total)).setText(String.valueOf(total));
        ((TextView) findViewById(R.id.tv_stat_easy)).setText(easyCorrect + "/" + easyCount);
        ((TextView) findViewById(R.id.tv_stat_medium)).setText(medCorrect + "/" + medCount);
        ((TextView) findViewById(R.id.tv_stat_hard)).setText(hardCorrect + "/" + hardCount);

        // Understanding % calculation (Weighted score)
        int maxPoints = (easyCount * 1) + (medCount * 2) + (hardCount * 3);
        int earnedPoints = (easyCorrect * 1) + (medCorrect * 2) + (hardCorrect * 3);
        int understanding = (maxPoints > 0) ? (earnedPoints * 100 / maxPoints) : 0;

        TextView tvUnderstanding = findViewById(R.id.tv_result_understanding);
        tvUnderstanding.setText(understanding + "%");

        // Calculate accuracy
        int accuracy = (total > 0) ? (correct * 100 / total) : 0;
        tvAccuracy.setText(accuracy + "%");

        // Format time
        long seconds = timeMillis / 1000;
        long mins = seconds / 60;
        long secs = seconds % 60;
        tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));

        // Change emoji based on performance
        if (accuracy >= 80)
            tvEmoji.setText("🎉");
        else if (accuracy >= 50)
            tvEmoji.setText("👏");
        else
            tvEmoji.setText("💪");

        btnDone.setOnClickListener(v -> finish());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        String quizData = getIntent().getStringExtra("QUIZ_DATA");
        int[] userAnswers = getIntent().getIntArrayExtra("USER_ANSWERS");

        findViewById(R.id.btn_review).setOnClickListener(v -> {
            Intent intent = new Intent(this, ReviewActivity.class);
            intent.putExtra("QUIZ_ID", quizId);
            intent.putExtra("QUIZ_DATA", quizData);
            intent.putExtra("USER_ANSWERS", userAnswers);
            startActivity(intent);
        });

        findViewById(R.id.btn_retry_wrong).setOnClickListener(v -> {
            if (quizData == null || userAnswers == null)
                return;
            try {
                JSONArray original = new JSONArray(quizData);
                JSONArray filtered = new JSONArray();

                for (int i = 0; i < original.length(); i++) {
                    if (i < userAnswers.length) {
                        JSONObject obj = original.getJSONObject(i);
                        String type = obj.optString("type", "single");
                        boolean isCorrect = false;

                        if (type.equals("multiple")) {
                            isCorrect = false;
                        } else {
                            int correctAns = obj.has("answer") ? obj.getInt("answer")
                                    : (obj.has("answers") ? obj.getJSONArray("answers").getInt(0) : -1);
                            if (userAnswers[i] == correctAns) {
                                isCorrect = true;
                            }
                        }

                        if (!isCorrect) {
                            filtered.put(obj);
                        }
                    }
                }

                if (filtered.length() == 0) {
                    Toast.makeText(this, "Bạn đã đúng hết rồi!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(this, QuizActivity.class);
                intent.putExtra("QUIZ_DATA", filtered.toString());
                intent.putExtra("QUIZ_TITLE", "Làm lại câu sai");
                intent.putExtra("IS_RESUME", false);
                startActivity(intent);
                finish();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi lọc câu hỏi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveResultToDatabase(int quizId, int correct, int total, long timeSpent) {
        if (quizId == -1)
            return;

        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs",
                android.content.Context.MODE_PRIVATE);
        String email = prefs.getString("CurrentUserEmail", "");

        new Thread(() -> {
            try {
                com.example.myapplication.data.QuizResult result = new com.example.myapplication.data.QuizResult(
                        quizId, correct, total, timeSpent, System.currentTimeMillis(), email);
                AppDatabase.getInstance(this).quizDao().insertResult(result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
