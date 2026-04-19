package com.example.myapplication.quiz;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.example.myapplication.utils.LocaleHelper.onAttach(newBase));
    }

    private TextView tvQuestionCounter, tvProgressPercent, tvQuestionText, tvCategory;
    private LinearProgressIndicator progressBar;
    private MaterialCardView[] optionCards = new MaterialCardView[4];
    private TextView[] optionTexts = new TextView[4];
    private MaterialCardView[] indicators = new MaterialCardView[4];
    private TextView[] indicatorTexts = new TextView[4];
    
    private MaterialButton btnNext, btnBack;
    private ImageView btnClose;

    private List<Question> questionList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private String currentQuizJson = "";
    private int[] userAnswers;
    private boolean[] isQuestionAnswered;
    private long startTime;
    private int quizId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        initViews();
        
        quizId = getIntent().getIntExtra("QUIZ_ID", -1);
        String quizData = getIntent().getStringExtra("QUIZ_DATA");
        String quizTitle = getIntent().getStringExtra("QUIZ_TITLE");
        
        if (quizData != null && !quizData.isEmpty()) {
            this.currentQuizJson = quizData;
            loadAiQuestions(quizData, quizTitle);
        } else {
            loadMockQuestions();
        }
        
        userAnswers = new int[questionList.size()];
        isQuestionAnswered = new boolean[questionList.size()];
        for(int i=0; i<userAnswers.length; i++) {
            userAnswers[i] = -1;
            isQuestionAnswered[i] = false;
        }
        
        startTime = System.currentTimeMillis();
        updateUI();

        setupListeners();
    }

    private void initViews() {
        tvQuestionCounter = findViewById(R.id.question_counter);
        tvProgressPercent = findViewById(R.id.tv_progress_percent);
        tvQuestionText = findViewById(R.id.tv_question_text);
        tvCategory = findViewById(R.id.tv_category);
        progressBar = findViewById(R.id.quiz_progress_bar);
        
        optionCards[0] = findViewById(R.id.option_a);
        optionCards[1] = findViewById(R.id.option_b);
        optionCards[2] = findViewById(R.id.option_c);
        optionCards[3] = findViewById(R.id.option_d);

        optionTexts[0] = findViewById(R.id.tv_option_a);
        optionTexts[1] = findViewById(R.id.tv_option_b);
        optionTexts[2] = findViewById(R.id.tv_option_c);
        optionTexts[3] = findViewById(R.id.tv_option_d);

        indicators[0] = findViewById(R.id.indicator_a);
        indicators[1] = findViewById(R.id.indicator_b);
        indicators[2] = findViewById(R.id.indicator_c);
        indicators[3] = findViewById(R.id.indicator_d);

        indicatorTexts[0] = findViewById(R.id.tv_indicator_a);
        indicatorTexts[1] = findViewById(R.id.tv_indicator_b);
        indicatorTexts[2] = findViewById(R.id.tv_indicator_c);
        indicatorTexts[3] = findViewById(R.id.tv_indicator_d);

        btnNext = findViewById(R.id.btn_next_quiz);
        btnBack = findViewById(R.id.btn_back_quiz);
        btnClose = findViewById(R.id.close_quiz);
    }

    private void setupListeners() {
        for (int i = 0; i < 4; i++) {
            final int index = i;
            optionCards[i].setOnClickListener(v -> selectOption(index));
        }

        btnNext.setOnClickListener(v -> {
            if (userAnswers[currentQuestionIndex] == -1) {
                Toast.makeText(this, getString(R.string.please_select_answer), Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentQuestionIndex < questionList.size() - 1) {
                currentQuestionIndex++;
                updateUI();
            } else {
                finishQuiz();
            }
        });

        btnBack.setOnClickListener(v -> {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--;
                updateUI();
            }
        });

        btnClose.setOnClickListener(v -> finish());
    }

    private void finishQuiz() {
        int correctCount = 0;
        for (int i = 0; i < questionList.size(); i++) {
            if (userAnswers[i] == questionList.get(i).correctAnswerIndex) {
                correctCount++;
            }
        }

        long timeSpent = System.currentTimeMillis() - startTime;
        
        // Save to Database only if it's a real quiz (not mock)
        if (quizId != -1) {
            com.example.myapplication.data.QuizResult result = new com.example.myapplication.data.QuizResult(
                    quizId, correctCount, questionList.size(), timeSpent, System.currentTimeMillis()
            );
            com.example.myapplication.data.AppDatabase.getInstance(this).quizDao().insertResult(result);
        }

        // Start QuizResultActivity
        Intent intent = new Intent(this, QuizResultActivity.class);
        intent.putExtra("CORRECT_ANSWERS", correctCount);
        intent.putExtra("TOTAL_QUESTIONS", questionList.size());
        intent.putExtra("TIME_SPENT", timeSpent);
        intent.putExtra("QUIZ_DATA", currentQuizJson); 
        intent.putExtra("USER_ANSWERS", userAnswers);
        intent.putExtra("QUIZ_ID", quizId);
        startActivity(intent);
        
        finish();
    }

    private void selectOption(int index) {
        if (isQuestionAnswered[currentQuestionIndex]) return;

        userAnswers[currentQuestionIndex] = index;
        isQuestionAnswered[currentQuestionIndex] = true;
        highlightSelectedOption(index);
    }

    private void highlightSelectedOption(int index) {
        int correctIndex = questionList.get(currentQuestionIndex).correctAnswerIndex;
        boolean answered = isQuestionAnswered[currentQuestionIndex];

        for (int i = 0; i < 4; i++) {
            if (answered) {
                // Show result coloring
                if (i == correctIndex) {
                    // Correct answer - always Green
                    optionCards[i].setStrokeWidth(4);
                    optionCards[i].setStrokeColor(ContextCompat.getColor(this, R.color.success));
                    optionCards[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.success_container));
                    indicators[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.success));
                    indicatorTexts[i].setTextColor(Color.WHITE);
                } else if (i == index && index != correctIndex) {
                    // User was wrong - show Red
                    optionCards[i].setStrokeWidth(4);
                    optionCards[i].setStrokeColor(ContextCompat.getColor(this, R.color.error));
                    optionCards[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.error_container));
                    indicators[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.error));
                    indicatorTexts[i].setTextColor(Color.WHITE);
                } else {
                    // Other options
                    resetOptionCard(i);
                }
            } else {
                // Not answered yet - show selection or default
                if (i == index) {
                    optionCards[i].setStrokeWidth(4);
                    optionCards[i].setStrokeColor(ContextCompat.getColor(this, R.color.primary));
                    optionCards[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary_fixed));
                    indicators[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary));
                    indicatorTexts[i].setTextColor(Color.WHITE);
                } else {
                    resetOptionCard(i);
                }
            }
        }
    }

    private void resetOptionCard(int i) {
        optionCards[i].setStrokeWidth(0);
        optionCards[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface_container_lowest));
        indicators[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface_container_low));
        indicatorTexts[i].setTextColor(ContextCompat.getColor(this, R.color.primary));
    }

    private void updateUI() {
        if (questionList.isEmpty()) {
            Toast.makeText(this, "Bộ câu hỏi này không có dữ liệu!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Question q = questionList.get(currentQuestionIndex);
        tvQuestionText.setText(q.question);
        tvCategory.setText(q.category);
        
        optionTexts[0].setText(q.options[0]);
        optionTexts[1].setText(q.options[1]);
        optionTexts[2].setText(q.options[2]);
        optionTexts[3].setText(q.options[3]);

        int size = questionList.size();
        int progress = (int) (((float) (currentQuestionIndex + 1) / size) * 100);
        progressBar.setProgress(progress);
        tvProgressPercent.setText(progress + "%");
        
        tvQuestionCounter.setText(getString(R.string.question_counter_placeholder, currentQuestionIndex + 1, size));

        // Restore user answer if exists
        highlightSelectedOption(userAnswers[currentQuestionIndex]);

        btnBack.setVisibility(currentQuestionIndex == 0 ? View.INVISIBLE : View.VISIBLE);
        btnNext.setText(currentQuestionIndex == size - 1 ? getString(R.string.btn_finish) : getString(R.string.btn_next));
    }

    private void loadMockQuestions() {
        questionList.add(new Question("Hệ thống Ô tô", "Bộ phận nào chịu trách nhiệm chính trong việc chuyển hóa hóa năng của nhiên liệu thành cơ năng?", 
                new String[]{"Kim phun nhiên liệu", "Cụm Piston và Xi-lanh", "Hệ thống Máy phát điện", "Vỏ bộ tăng áp"}, 1));
        
        questionList.add(new Question("Công nghệ", "AI là viết tắt của cụm từ nào sau đây?", 
                new String[]{"Artificial Intelligence", "Advanced Integration", "Automated Interface", "Active Interaction"}, 0));

        questionList.add(new Question("Lịch sử", "Thành phố nào là thủ đô của Việt Nam?", 
                new String[]{"Đà Nẵng", "Hồ Chí Minh", "Hà Nội", "Huế"}, 2));
    }

    private void loadAiQuestions(String jsonData, String title) {
        try {
            org.json.JSONArray array = new org.json.JSONArray(jsonData);
            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject obj = array.getJSONObject(i);
                String question = obj.getString("question");
                org.json.JSONArray optionsArray = obj.getJSONArray("options");
                String[] options = new String[4];
                for (int j = 0; j < 4; j++) {
                    options[j] = optionsArray.getString(j);
                }
                int correctAnswer = obj.getInt("answer");
                questionList.add(new Question(title != null ? title : "AI Quiz", question, options, correctAnswer));
            }
        } catch (Exception e) {
            e.printStackTrace();
            loadMockQuestions();
        }
    }

    private static class Question {
        String category;
        String question;
        String[] options;
        int correctAnswerIndex;

        Question(String category, String question, String[] options, int correctAnswerIndex) {
            this.category = category;
            this.question = question;
            this.options = options;
            this.correctAnswerIndex = correctAnswerIndex;
        }
    }
}
