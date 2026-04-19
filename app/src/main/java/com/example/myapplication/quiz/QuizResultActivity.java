package com.example.myapplication.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
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

        TextView tvEmoji = findViewById(R.id.tv_result_emoji);
        TextView tvScore = findViewById(R.id.tv_result_score);
        TextView tvTime = findViewById(R.id.tv_result_time);
        TextView tvAccuracy = findViewById(R.id.tv_result_accuracy);
        Button btnDone = findViewById(R.id.btn_done);

        int quizId = getIntent().getIntExtra("QUIZ_ID", -1);
        
        saveResultToDatabase(quizId, correct, total, timeMillis);

        tvScore.setText(correct + "/" + total);
        
        // Calculate accuracy
        int accuracy = (total > 0) ? (correct * 100 / total) : 0;
        tvAccuracy.setText(accuracy + "%");

        // Format time
        long seconds = timeMillis / 1000;
        long mins = seconds / 60;
        long secs = seconds % 60;
        tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));

        // Change emoji based on performance
        if (accuracy >= 80) tvEmoji.setText("🎉");
        else if (accuracy >= 50) tvEmoji.setText("👏");
        else tvEmoji.setText("💪");

        btnDone.setOnClickListener(v -> finish());

        String quizData = getIntent().getStringExtra("QUIZ_DATA");
        int[] userAnswers = getIntent().getIntArrayExtra("USER_ANSWERS");

        findViewById(R.id.btn_review).setOnClickListener(v -> {
            Intent intent = new Intent(this, ReviewActivity.class);
            intent.putExtra("QUIZ_DATA", quizData);
            intent.putExtra("USER_ANSWERS", userAnswers);
            startActivity(intent);
        });

        findViewById(R.id.btn_retry_wrong).setOnClickListener(v -> {
            if (quizData == null || userAnswers == null) return;
            try {
                org.json.JSONArray original = new org.json.JSONArray(quizData);
                org.json.JSONArray filtered = new org.json.JSONArray();
                
                for (int i = 0; i < original.length(); i++) {
                    // Safety check: Ensure we don't go out of bounds if arrays are misaligned
                    if (userAnswers != null && i < userAnswers.length) {
                        org.json.JSONObject obj = original.getJSONObject(i);
                        int correctAns = obj.getInt("answer");
                        if (userAnswers[i] != correctAns) {
                            filtered.put(obj);
                        }
                    }
                }

                if (filtered.length() == 0) {
                    android.widget.Toast.makeText(this, "Bạn đã đúng hết rồi, không có gì để làm lại!", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(this, QuizActivity.class);
                intent.putExtra("QUIZ_DATA", filtered.toString());
                intent.putExtra("QUIZ_TITLE", "Retry Wrong Questions");
                intent.putExtra("QUIZ_ID", getIntent().getIntExtra("QUIZ_ID", -1));
                startActivity(intent);
                finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void saveResultToDatabase(int quizId, int correct, int total, long timeSpent) {
        if (quizId == -1) return;
        
        new Thread(() -> {
            try {
                com.example.myapplication.data.QuizResult result = new com.example.myapplication.data.QuizResult(
                    quizId, correct, total, timeSpent, System.currentTimeMillis()
                );
                com.example.myapplication.data.AppDatabase.getInstance(this).quizDao().insertResult(result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
