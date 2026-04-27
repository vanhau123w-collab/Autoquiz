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
import com.example.myapplication.data.AppDatabase;
import com.example.myapplication.data.Quiz;
import org.json.JSONArray;
import org.json.JSONObject;

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
    private android.widget.CheckBox[] checkBoxes = new android.widget.CheckBox[4];
    private TextView tvSelectLabel;
    
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
        
        boolean isResume = getIntent().getBooleanExtra("IS_RESUME", true);
        if (isResume) {
            restoreProgress();
        } else if (quizId != -1) {
            new Thread(() -> {
                try {
                    Quiz quiz = AppDatabase.getInstance(this).quizDao().getQuizById(quizId);
                    if (quiz != null) {
                        quiz.setLastPosition(0);
                        quiz.setLastProgress(null);
                        AppDatabase.getInstance(this).quizDao().update(quiz);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }).start();
        }
        
        updateUI();

        setupListeners();
        
        loadDailyStreak();
    }
    
    private void loadDailyStreak() {
        new Thread(() -> {
            try {
                android.content.SharedPreferences sharedPref = getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
                String email = sharedPref.getString("CurrentUserEmail", "");
                java.util.List<com.example.myapplication.data.QuizResult> results = com.example.myapplication.data.AppDatabase.getInstance(this).quizDao().getAllResults(email);
                int streak = com.example.myapplication.utils.StreakUtils.calculateCurrentStreak(results);
                runOnUiThread(() -> {
                    if (tvStreak != null) {
                        tvStreak.setText("🔥 " + streak);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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

        checkBoxes[0] = findViewById(R.id.cb_option_a);
        checkBoxes[1] = findViewById(R.id.cb_option_b);
        checkBoxes[2] = findViewById(R.id.cb_option_c);
        checkBoxes[3] = findViewById(R.id.cb_option_d);

        tvSelectLabel = findViewById(R.id.tv_select_label);

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
                saveProgress();
                currentQuestionIndex++;
                updateUI();
            } else {
                finishQuiz();
            }
        });

        btnBack.setOnClickListener(v -> {
            if (currentQuestionIndex > 0) {
                saveProgress();
                currentQuestionIndex--;
                updateUI();
            }
        });

        btnClose.setOnClickListener(v -> {
            saveProgress();
            finish();
        });
    }

    private void saveProgress() {
        if (quizId == -1) return;
        new Thread(() -> {
            try {
                Quiz quiz = AppDatabase.getInstance(this).quizDao().getQuizById(quizId);
                if (quiz != null) {
                    JSONObject progress = new JSONObject();
                    JSONArray singleAns = new JSONArray();
                    for (int ans : userAnswers) singleAns.put(ans);
                    progress.put("single", singleAns);

                    JSONArray multiAns = new JSONArray();
                    for (List<Integer> list : userMultiAnswers) {
                        JSONArray item = new JSONArray();
                        for (int val : list) item.put(val);
                        multiAns.put(item);
                    }
                    progress.put("multi", multiAns);

                    quiz.setLastPosition(currentQuestionIndex);
                    quiz.setLastProgress(progress.toString());
                    AppDatabase.getInstance(this).quizDao().update(quiz);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void restoreProgress() {
        if (quizId == -1) return;
        try {
            Quiz quiz = AppDatabase.getInstance(this).quizDao().getQuizById(quizId);
            if (quiz != null && quiz.getLastProgress() != null) {
                JSONObject progress = new JSONObject(quiz.getLastProgress());
                
                JSONArray singleAns = progress.getJSONArray("single");
                for (int i = 0; i < singleAns.length() && i < userAnswers.length; i++) {
                    userAnswers[i] = singleAns.getInt(i);
                    if (userAnswers[i] != -1) isQuestionAnswered[i] = true;
                }

                JSONArray multiAns = progress.getJSONArray("multi");
                for (int i = 0; i < multiAns.length() && i < userMultiAnswers.length; i++) {
                    JSONArray item = multiAns.getJSONArray(i);
                    userMultiAnswers[i].clear();
                    for (int j = 0; j < item.length(); j++) {
                        userMultiAnswers[i].add(item.getInt(j));
                        isQuestionAnswered[i] = true;
                    }
                }
                currentQuestionIndex = quiz.getLastPosition();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }



    private void finishQuiz() {
        if (quizId != -1) {
            new Thread(() -> {
                try {
                    Quiz quiz = AppDatabase.getInstance(this).quizDao().getQuizById(quizId);
                    if (quiz != null) {
                        JSONArray results = new JSONArray();
                        for (int ans : userAnswers) results.put(ans);
                        quiz.setCompletedResults(results.toString());
                        quiz.setLastPosition(0);
                        quiz.setLastProgress(null);
                        AppDatabase.getInstance(this).quizDao().update(quiz);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }).start();
        }

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

        final int fCorrectCount = correctCount;
        final int fEasyCount = easyCount, fMedCount = mediumCount, fHardCount = hardCount;
        final int fEasyCorrect = easyCorrect, fMedCorrect = mediumCorrect, fHardCorrect = hardCorrect;
        final long fTimeSpent = timeSpent;
        
        String title = getIntent().getStringExtra("QUIZ_TITLE");
        if (title == null) title = "Quiz";
        final String fTitle = title;

        new Thread(() -> {
            boolean needsEvaluation = false;
            try {
                if (quizId != -1) {
                    Quiz quiz = AppDatabase.getInstance(this).quizDao().getQuizById(quizId);
                    if (quiz != null && (quiz.getAiEvaluation() == null || quiz.getAiEvaluation().isEmpty())) {
                        needsEvaluation = true;
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }

            final boolean doEval = needsEvaluation;
            runOnUiThread(() -> {
                if (doEval) {
                    com.example.myapplication.utils.AIEvaluationManager.evaluateQuiz(
                            this, quizId, currentQuizJson, userAnswers, fTitle,
                            (success, resultOrError) -> navigateToResult(fCorrectCount, questionList.size(),
                                    fEasyCount, fMedCount, fHardCount,
                                    fEasyCorrect, fMedCorrect, fHardCorrect, fTimeSpent)
                    );
                } else {
                    navigateToResult(fCorrectCount, questionList.size(),
                            fEasyCount, fMedCount, fHardCount,
                            fEasyCorrect, fMedCorrect, fHardCorrect, fTimeSpent);
                }
            });
        }).start();
    }



    private void navigateToResult(int correctCount, int total,
            int easyCount, int medCount, int hardCount,
            int easyCorrect, int medCorrect, int hardCorrect, long timeSpent) {
        Intent intent = new Intent(this, QuizResultActivity.class);
        intent.putExtra("CORRECT_ANSWERS", correctCount);
        intent.putExtra("TOTAL_QUESTIONS", total);
        intent.putExtra("EASY_COUNT", easyCount);
        intent.putExtra("MEDIUM_COUNT", medCount);
        intent.putExtra("HARD_COUNT", hardCount);
        intent.putExtra("EASY_CORRECT", easyCorrect);
        intent.putExtra("MEDIUM_CORRECT", medCorrect);
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
            // Update checkbox state
            for (int i = 0; i < 4; i++) {
                checkBoxes[i].setChecked(selected.contains(i));
            }
            highlightSelectedOption(index);
        } else {
            // Single-select mode
            if (isQuestionAnswered[currentQuestionIndex]) return;
            userAnswers[currentQuestionIndex] = index;
            isQuestionAnswered[currentQuestionIndex] = true;
            // Streak logic removed to use daily streak
            
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
        
        // Display difficulty badge with distinct colors
        if (q.difficulty == 1) {
            tvCategory.setText("DỄ");
            tvCategory.setTextColor(Color.parseColor("#16A34A")); // green
            tvCategory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F0FDF4"))); // green light
        } else if (q.difficulty == 3) {
            tvCategory.setText("KHÓ");
            tvCategory.setTextColor(Color.parseColor("#DC2626")); // red
            tvCategory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF2F2"))); // red light
        } else {
            tvCategory.setText("TRUNG BÌNH");
            tvCategory.setTextColor(Color.parseColor("#EA580C")); // orange
            tvCategory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF7ED"))); // orange light
        }
        
        // Display question type & checkbox visibility
        if (q.type.equals("multiple")) {
            tvQuestionType.setText("CHỌN NHIỀU ĐÁP ÁN");
            tvQuestionType.setVisibility(View.VISIBLE);
            tvSelectLabel.setText("CHỌN NHIỀU ĐÁP ÁN");
            for (int i = 0; i < 4; i++) {
                checkBoxes[i].setVisibility(View.VISIBLE);
                checkBoxes[i].setChecked(userMultiAnswers[currentQuestionIndex].contains(i));
            }
        } else {
            tvQuestionType.setVisibility(View.GONE);
            tvSelectLabel.setText("SELECT ONE ANSWER");
            for (int i = 0; i < 4; i++) {
                checkBoxes[i].setVisibility(View.GONE);
                checkBoxes[i].setChecked(false);
            }
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
