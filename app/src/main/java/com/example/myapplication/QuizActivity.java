package com.example.myapplication;

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

import java.util.ArrayList;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

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
    private int selectedOptionIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        initViews();
        
        String quizData = getIntent().getStringExtra("QUIZ_DATA");
        String quizTitle = getIntent().getStringExtra("QUIZ_TITLE");
        
        if (quizData != null && !quizData.isEmpty()) {
            loadAiQuestions(quizData, quizTitle);
        } else {
            loadMockQuestions();
        }
        
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
            if (currentQuestionIndex < questionList.size() - 1) {
                currentQuestionIndex++;
                selectedOptionIndex = -1;
                updateUI();
            } else {
                Toast.makeText(this, "Bạn đã hoàn thành bài trắc nghiệm!", Toast.LENGTH_LONG).show();
                finish();
            }
        });

        btnBack.setOnClickListener(v -> {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--;
                selectedOptionIndex = -1;
                updateUI();
            }
        });

        btnClose.setOnClickListener(v -> finish());
    }

    private void selectOption(int index) {
        selectedOptionIndex = index;
        for (int i = 0; i < 4; i++) {
            if (i == index) {
                optionCards[i].setStrokeWidth(4);
                optionCards[i].setStrokeColor(ContextCompat.getColor(this, R.color.primary));
                optionCards[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary_fixed));
                indicators[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary));
                indicatorTexts[i].setTextColor(Color.WHITE);
            } else {
                optionCards[i].setStrokeWidth(0);
                optionCards[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface_container_lowest));
                indicators[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface_container_low));
                indicatorTexts[i].setTextColor(ContextCompat.getColor(this, R.color.primary));
            }
        }
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
        tvQuestionCounter.setText("Câu hỏi " + (currentQuestionIndex + 1) + " trên " + size);

        // Reset options visual state
        for (int i = 0; i < 4; i++) {
            optionCards[i].setStrokeWidth(0);
            optionCards[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface_container_lowest));
            indicators[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface_container_low));
            indicatorTexts[i].setTextColor(ContextCompat.getColor(this, R.color.primary));
        }

        btnBack.setVisibility(currentQuestionIndex == 0 ? View.INVISIBLE : View.VISIBLE);
        btnNext.setText(currentQuestionIndex == size - 1 ? "Hoàn thành" : "Câu tiếp theo");
    }

    private void loadMockQuestions() {
        questionList.add(new Question("Hệ thống Ô tô", "Bộ phận nào chịu trách nhiệm chính trong việc chuyển hóa hóa năng của nhiên liệu thành cơ năng?", 
                new String[]{"Kim phun nhiên liệu", "Cụm Piston và Xi-lanh", "Hệ thống Máy phát điện", "Vỏ bộ tăng áp"}));
        
        questionList.add(new Question("Công nghệ", "AI là viết tắt của cụm từ nào sau đây?", 
                new String[]{"Artificial Intelligence", "Advanced Integration", "Automated Interface", "Active Interaction"}));

        questionList.add(new Question("Lịch sử", "Thành phố nào là thủ đô của Việt Nam?", 
                new String[]{"Đà Nẵng", "Hồ Chí Minh", "Hà Nội", "Huế"}));
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
                // Note: The AI returns 'answer' as 0-3, but our current Question class 
                // doesn't store the correct answer yet. We'll add it for logic later.
                questionList.add(new Question(title != null ? title : "AI Quiz", question, options));
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

        Question(String category, String question, String[] options) {
            this.category = category;
            this.question = question;
            this.options = options;
        }
    }
}