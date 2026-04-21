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

    private TextView tvQuestionCounter, tvProgressPercent, tvQuestionText, tvCategory, tvQuestionType, tvStreak;
    private LinearProgressIndicator progressBar;
    private MaterialCardView[] optionCards = new MaterialCardView[4];
    private TextView[] optionTexts = new TextView[4];
    private MaterialCardView[] indicators = new MaterialCardView[4];
    private TextView[] indicatorTexts = new TextView[4];
    private ImageView[] checkIcons = new ImageView[4]; 
    
    private MaterialButton btnNext, btnBack;
    private ImageView btnClose;

    private List<Question> questionList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int currentStreak = 0;
    private String currentQuizJson = "";
    private int[] userAnswers; 
    private List<Integer>[] userMultiAnswers; 
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
        userMultiAnswers = new List[questionList.size()];
        isQuestionAnswered = new boolean[questionList.size()];
        for(int i=0; i<userAnswers.length; i++) {
            userAnswers[i] = -1;
            userMultiAnswers[i] = new ArrayList<>();
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
        tvQuestionType = findViewById(R.id.tv_question_type);
        tvStreak = findViewById(R.id.streak_count);
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

        checkIcons[0] = findViewById(R.id.check_icon_a);
        checkIcons[1] = findViewById(R.id.check_icon_b);
        checkIcons[2] = findViewById(R.id.check_icon_c);
        checkIcons[3] = findViewById(R.id.check_icon_d);

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
            Question q = questionList.get(currentQuestionIndex);
            if (q.type.equals("multiple")) {
                if (userMultiAnswers[currentQuestionIndex].isEmpty()) {
                    Toast.makeText(this, "Vui lòng chọn ít nhất 1 đáp án", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Submit multi-answer
                if (!isQuestionAnswered[currentQuestionIndex]) {
                    isQuestionAnswered[currentQuestionIndex] = true;
                    // Streak logic for multi-select
                    List<Integer> userAns = userMultiAnswers[currentQuestionIndex];
                    if (userAns.size() == q.correctAnswers.length) {
                        boolean allCorrect = true;
                        for (int ans : q.correctAnswers) {
                            if (!userAns.contains(ans)) {
                                allCorrect = false;
                                break;
                            }
                        }
                        if (allCorrect) currentStreak++;
                        else currentStreak = 0;
                    } else {
                        currentStreak = 0;
                    }
                    highlightSelectedOption(-1);
                    return; // Wait for next click to proceed
                }
            } else {
                if (userAnswers[currentQuestionIndex] == -1) {
                    Toast.makeText(this, getString(R.string.please_select_answer), Toast.LENGTH_SHORT).show();
                    return;
                }
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
        int easyCount = 0, mediumCount = 0, hardCount = 0;
        int easyCorrect = 0, mediumCorrect = 0, hardCorrect = 0;

        for (int i = 0; i < questionList.size(); i++) {
            Question q = questionList.get(i);
            boolean isCorrect = false;

            // Count total per difficulty
            if (q.difficulty == 1) easyCount++;
            else if (q.difficulty == 3) hardCount++;
            else mediumCount++;

            if (q.type.equals("multiple")) {
                List<Integer> userAns = userMultiAnswers[i];
                if (userAns.size() == q.correctAnswers.length) {
                    boolean allCorrect = true;
                    for (int ans : q.correctAnswers) {
                        if (!userAns.contains(ans)) {
                            allCorrect = false;
                            break;
                        }
                    }
                    if (allCorrect) isCorrect = true;
                }
            } else {
                if (userAnswers[i] == q.correctAnswerIndex) {
                    isCorrect = true;
                }
            }

            if (isCorrect) {
                correctCount++;
                if (q.difficulty == 1) easyCorrect++;
                else if (q.difficulty == 3) hardCorrect++;
                else mediumCorrect++;
            }
        }

        long timeSpent = System.currentTimeMillis() - startTime;

        // Start QuizResultActivity
        Intent intent = new Intent(this, QuizResultActivity.class);
        intent.putExtra("CORRECT_ANSWERS", correctCount);
        intent.putExtra("TOTAL_QUESTIONS", questionList.size());
        
        // Passing detailed stats
        intent.putExtra("EASY_COUNT", easyCount);
        intent.putExtra("MEDIUM_COUNT", mediumCount);
        intent.putExtra("HARD_COUNT", hardCount);
        intent.putExtra("EASY_CORRECT", easyCorrect);
        intent.putExtra("MEDIUM_CORRECT", mediumCorrect);
        intent.putExtra("HARD_CORRECT", hardCorrect);

        intent.putExtra("TIME_SPENT", timeSpent);
        intent.putExtra("QUIZ_DATA", currentQuizJson); 
        intent.putExtra("USER_ANSWERS", userAnswers);
        intent.putExtra("QUIZ_ID", quizId);
        startActivity(intent);
        
        finish();
    }

    private void selectOption(int index) {
        Question q = questionList.get(currentQuestionIndex);
        
        if (q.type.equals("multiple")) {
            // Multi-select mode
            List<Integer> selected = userMultiAnswers[currentQuestionIndex];
            if (selected.contains(index)) {
                selected.remove(Integer.valueOf(index));
            } else {
                selected.add(index);
            }
            highlightSelectedOption(index);
        } else {
            // Single-select mode
            if (isQuestionAnswered[currentQuestionIndex]) return;
            userAnswers[currentQuestionIndex] = index;
            isQuestionAnswered[currentQuestionIndex] = true;
            
            // Streak logic for single-select
            if (index == q.correctAnswerIndex) {
                currentStreak++;
            } else {
                currentStreak = 0;
            }
            
            highlightSelectedOption(index);
        }
    }

    private void highlightSelectedOption(int index) {
        Question q = questionList.get(currentQuestionIndex);
        boolean answered = isQuestionAnswered[currentQuestionIndex];

        if (q.type.equals("multiple")) {
            // Multi-select highlighting
            List<Integer> selected = userMultiAnswers[currentQuestionIndex];
            for (int i = 0; i < 4; i++) {
                final int currentIndex = i;
                boolean isCorrect = false;
                for (int ans : q.correctAnswers) {
                    if (ans == currentIndex) {
                        isCorrect = true;
                        break;
                    }
                }
                boolean isSelected = selected.contains(currentIndex);
                
                if (answered) {
                    // Show results
                    if (isCorrect) {
                        optionCards[i].setStrokeWidth(dpToPx(2));
                        optionCards[i].setStrokeColor(ContextCompat.getColor(this, R.color.success));
                        optionCards[i].setCardBackgroundColor(Color.parseColor("#F0FDF4")); // success light
                        indicators[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.success));
                        indicatorTexts[i].setTextColor(Color.WHITE);
                        checkIcons[i].setVisibility(View.VISIBLE);
                    } else if (isSelected) {
                        optionCards[i].setStrokeWidth(dpToPx(2));
                        optionCards[i].setStrokeColor(ContextCompat.getColor(this, R.color.error));
                        optionCards[i].setCardBackgroundColor(Color.parseColor("#FEF2F2")); // error light
                        indicators[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.error));
                        indicatorTexts[i].setTextColor(Color.WHITE);
                        checkIcons[i].setVisibility(View.VISIBLE);
                    } else {
                        resetOptionCard(i);
                        checkIcons[i].setVisibility(View.GONE);
                    }
                } else {
                    // Show selection
                    if (isSelected) {
                        optionCards[i].setStrokeWidth(dpToPx(2));
                        optionCards[i].setStrokeColor(ContextCompat.getColor(this, R.color.primary));
                        optionCards[i].setCardBackgroundColor(Color.parseColor("#EFF6FF")); // primary light
                        indicators[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary));
                        indicatorTexts[i].setTextColor(Color.WHITE);
                        checkIcons[i].setVisibility(View.VISIBLE);
                    } else {
                        resetOptionCard(i);
                        checkIcons[i].setVisibility(View.GONE);
                    }
                }
            }
        } else {
            // Single-select highlighting
            int correctIndex = q.correctAnswerIndex;
            for (int i = 0; i < 4; i++) {
                checkIcons[i].setVisibility(View.GONE);
                if (answered) {
                    if (i == correctIndex) {
                        optionCards[i].setStrokeWidth(dpToPx(2));
                        optionCards[i].setStrokeColor(ContextCompat.getColor(this, R.color.success));
                        optionCards[i].setCardBackgroundColor(Color.parseColor("#F0FDF4"));
                        indicators[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.success));
                        indicatorTexts[i].setTextColor(Color.WHITE);
                    } else if (i == index && index != correctIndex) {
                        optionCards[i].setStrokeWidth(dpToPx(2));
                        optionCards[i].setStrokeColor(ContextCompat.getColor(this, R.color.error));
                        optionCards[i].setCardBackgroundColor(Color.parseColor("#FEF2F2"));
                        indicators[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.error));
                        indicatorTexts[i].setTextColor(Color.WHITE);
                    } else {
                        resetOptionCard(i);
                    }
                } else {
                    if (i == index) {
                        optionCards[i].setStrokeWidth(dpToPx(2));
                        optionCards[i].setStrokeColor(ContextCompat.getColor(this, R.color.primary));
                        optionCards[i].setCardBackgroundColor(Color.parseColor("#EFF6FF"));
                        indicators[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary));
                        indicatorTexts[i].setTextColor(Color.WHITE);
                    } else {
                        resetOptionCard(i);
                    }
                }
            }
        }
    }

    private void resetOptionCard(int i) {
        optionCards[i].setStrokeWidth(dpToPx(1));
        optionCards[i].setStrokeColor(Color.parseColor("#F1F5F9"));
        optionCards[i].setCardBackgroundColor(Color.WHITE);
        indicators[i].setCardBackgroundColor(Color.parseColor("#F8FAFC"));
        indicatorTexts[i].setTextColor(Color.parseColor("#64748B"));
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void updateUI() {
        if (questionList.isEmpty()) {
            Toast.makeText(this, "Bộ câu hỏi này không có dữ liệu!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Question q = questionList.get(currentQuestionIndex);
        tvQuestionText.setText(q.question);
        
        // Display difficulty badge
        String difficultyText = q.difficulty == 1 ? "DỄ" : (q.difficulty == 3 ? "KHÓ" : "TRUNG BÌNH");
        tvCategory.setText(difficultyText);
        
        // Display question type
        if (q.type.equals("multiple")) {
            tvQuestionType.setText("CHỌN NHIỀU ĐÁP ÁN");
            tvQuestionType.setVisibility(View.VISIBLE);
        } else {
            tvQuestionType.setVisibility(View.GONE);
        }
        
        optionTexts[0].setText(q.options[0]);
        optionTexts[1].setText(q.options[1]);
        optionTexts[2].setText(q.options[2]);
        optionTexts[3].setText(q.options[3]);

        int size = questionList.size();
        int progress = (int) (((float) (currentQuestionIndex + 1) / size) * 100);
        progressBar.setProgress(progress);
        tvProgressPercent.setText(progress + "%");
        
        tvQuestionCounter.setText("Question " + (currentQuestionIndex + 1) + " of " + size);
        tvStreak.setText("🔥 " + currentStreak);

        // Restore user answer if exists
        highlightSelectedOption(userAnswers[currentQuestionIndex]);

        btnBack.setVisibility(currentQuestionIndex == 0 ? View.INVISIBLE : View.VISIBLE);
        btnNext.setText(currentQuestionIndex == size - 1 ? "Finish" : "Next");
    }

    private void loadMockQuestions() {
        questionList.add(new Question("Hệ thống Ô tô", "Bộ phận nào chịu trách nhiệm chính trong việc chuyển hóa hóa năng của nhiên liệu thành cơ năng?", 
                new String[]{"Kim phun nhiên liệu", "Cụm Piston và Xi-lanh", "Hệ thống Máy phát điện", "Vỏ bộ tăng áp"}, 1, 2));
        
        questionList.add(new Question("Công nghệ", "AI là viết tắt của cụm từ nào sau đây?", 
                new String[]{"Artificial Intelligence", "Advanced Integration", "Automated Interface", "Active Interaction"}, 0, 1));

        questionList.add(new Question("Lịch sử", "Thành phố nào là thủ đô của Việt Nam?", 
                new String[]{"Đà Nẵng", "Hồ Chí Minh", "Hà Nội", "Huế"}, 2, 1));
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
                int difficulty = obj.optInt("difficulty", 2);
                String type = obj.optString("type", "single");
                
                if (type.equals("multiple")) {
                    // Parse multiple answers
                    org.json.JSONArray answersArray = obj.getJSONArray("answers");
                    int[] correctAnswers = new int[answersArray.length()];
                    for (int j = 0; j < answersArray.length(); j++) {
                        correctAnswers[j] = answersArray.getInt(j);
                    }
                    questionList.add(new Question(title != null ? title : "AI Quiz", question, options, correctAnswers, type, difficulty));
                } else {
                    // Single answer (backward compatible)
                    int correctAnswer = obj.has("answer") ? obj.getInt("answer") : obj.getJSONArray("answers").getInt(0);
                    questionList.add(new Question(title != null ? title : "AI Quiz", question, options, correctAnswer, difficulty));
                }
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
        int correctAnswerIndex; // For backward compatibility (single answer)
        int[] correctAnswers; // For multiple answers
        String type; // "single" or "multiple"
        int difficulty; // 1=easy, 2=medium, 3=hard

        // Constructor for single answer (backward compatible)
        Question(String category, String question, String[] options, int correctAnswerIndex, int difficulty) {
            this.category = category;
            this.question = question;
            this.options = options;
            this.correctAnswerIndex = correctAnswerIndex;
            this.correctAnswers = new int[]{correctAnswerIndex};
            this.type = "single";
            this.difficulty = difficulty;
        }

        // Constructor for multiple answers
        Question(String category, String question, String[] options, int[] correctAnswers, String type, int difficulty) {
            this.category = category;
            this.question = question;
            this.options = options;
            this.correctAnswers = correctAnswers;
            this.correctAnswerIndex = correctAnswers.length > 0 ? correctAnswers[0] : 0;
            this.type = type;
            this.difficulty = difficulty;
        }
    }
}
