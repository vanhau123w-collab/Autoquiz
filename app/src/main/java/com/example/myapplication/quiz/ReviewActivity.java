package com.example.myapplication.quiz;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.data.Quiz;
import com.example.myapplication.quiz.QuizActivity;
import com.example.myapplication.utils.AIModelManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class ReviewActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(com.example.myapplication.utils.LocaleHelper.onAttach(newBase));
    }

    private RecyclerView rvReview;
    private ImageButton btnClose;
    private List<ReviewItem> reviewList = new ArrayList<>();
    private AIModelManager aiModelManager;
    private int quizId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        rvReview = findViewById(R.id.rv_review);
        btnClose = findViewById(R.id.btn_close_review);

        quizId = getIntent().getIntExtra("QUIZ_ID", -1);
        String quizData = getIntent().getStringExtra("QUIZ_DATA");
        int[] userAnswers = getIntent().getIntArrayExtra("USER_ANSWERS");

        loadReviewData(quizData, userAnswers);
        
        findViewById(R.id.btn_retry_wrong_review).setOnClickListener(v -> {
            retryWrongQuestions(quizData, userAnswers);
        });

        rvReview.setLayoutManager(new LinearLayoutManager(this));
        aiModelManager = new AIModelManager(this);
        rvReview.setAdapter(new ReviewAdapter(this, reviewList, this::showAIExplanation));

        btnClose.setOnClickListener(v -> finish());
    }

    private void showAIExplanation(ReviewItem item) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_ai_explanation, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView tvExplainTitle = sheetView.findViewById(R.id.tv_explain_title);
        TextView tvExplainContent = sheetView.findViewById(R.id.tv_explain_content);
        ProgressBar progressBar = sheetView.findViewById(R.id.progress_bar_explain);
        View btnCloseSheet = sheetView.findViewById(R.id.btn_close_sheet);

        tvExplainTitle.setText("✨ Giải thích câu hỏi");
        btnCloseSheet.setOnClickListener(v -> bottomSheetDialog.dismiss());
        sheetView.findViewById(R.id.btn_got_it).setOnClickListener(v -> bottomSheetDialog.dismiss());
        bottomSheetDialog.show();

        // Nếu đã có giải thích thì dùng luôn
        if (item.aiExplanation != null && !item.aiExplanation.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            tvExplainContent.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
            tvExplainContent.setText(android.text.Html.fromHtml(formatExplanation(item.aiExplanation), android.text.Html.FROM_HTML_MODE_COMPACT));
            return;
        }

        tvExplainContent.setText("AI đang phân tích và tìm dẫn chứng...");
        progressBar.setVisibility(View.VISIBLE);

        callAIForExplanation(item, new AIExplanationCallback() {
            @Override
            public void onSuccess(String explanation) {
                item.aiExplanation = explanation; // Lưu lại kết quả
                saveExplanationToDb(item);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvExplainContent.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
                    tvExplainContent.setText(android.text.Html.fromHtml(formatExplanation(explanation), android.text.Html.FROM_HTML_MODE_COMPACT));
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvExplainContent.setText("Lỗi: " + error);
                });
            }
        });
    }

    private void saveExplanationToDb(ReviewItem item) {
        if (quizId == -1) return;
        new Thread(() -> {
            try {
                com.example.myapplication.data.AppDatabase db = com.example.myapplication.data.AppDatabase.getInstance(this);
                com.example.myapplication.data.Quiz quiz = db.quizDao().getQuizById(quizId);
                if (quiz != null) {
                    JSONArray array = new JSONArray(quiz.getJsonData());
                    if (item.originalIndex >= 0 && item.originalIndex < array.length()) {
                        JSONObject obj = array.getJSONObject(item.originalIndex);
                        obj.put("aiExplanation", item.aiExplanation);
                        quiz.setJsonData(array.toString());
                        db.quizDao().update(quiz);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String formatExplanation(String text) {
        // Simple markdown to HTML conversion for links and bold text
        return text.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>")
                   .replaceAll("\n", "<br>")
                   .replaceAll("(https?://[\\w\\d./?=#&%-]+)", "<a href=\"$1\">$1</a>");
    }

    private void callAIForExplanation(ReviewItem item, AIExplanationCallback callback) {
        String provider = aiModelManager.getSelectedProvider();
        String modelId = AIModelManager.toApiModelId(provider, aiModelManager.getSelectedModel());
        String apiKey = aiModelManager.nextKey(provider);

        if (apiKey == null || apiKey.isEmpty()) {
            callback.onFailure("Chưa có API Key cho " + provider);
            return;
        }

        String optionsStr = "";
        for (int i = 0; i < item.options.length; i++) {
            optionsStr += (char) ('A' + i) + ". " + item.options[i] + "\n";
        }

        String userAnsText = item.userIndex >= 0 ? item.options[item.userIndex] : "Không chọn";
        String correctAnsText = item.options[item.correctIndex];

        String prompt = "Hãy đóng vai là một chuyên gia giáo dục. Giải thích chi tiết tại sao đáp án đúng là chính xác và tại sao các lựa chọn khác (đặc biệt là lựa chọn của người dùng) chưa đúng.\n\n" +
                "Câu hỏi: " + item.question + "\n" +
                "Các lựa chọn:\n" + optionsStr +
                "Đáp án đúng: " + correctAnsText + "\n" +
                "Đáp án người dùng đã chọn: " + userAnsText + "\n\n" +
                "Yêu cầu:\n" +
                "1. Giải thích ngắn gọn, dễ hiểu, giọng văn sư phạm.\n" +
                "2. Cung cấp dẫn chứng kiến thức khoa học/thực tế.\n" +
                "3. TÌM VÀ CUNG CẤP ÍT NHẤT 1 ĐƯỜNG LINK (URL) từ nguồn tin cậy (Wikipedia, các trang giáo dục (.edu), hoặc báo chí uy tín) để người dùng có thể tự kiểm chứng.\n" +
                "4. Nếu người dùng chọn đúng, hãy chúc mừng và mở rộng thêm kiến thức liên quan.\n" +
                "5. Trả về kết quả bằng Tiếng Việt.";

        OkHttpClient client = new OkHttpClient();
        MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

        try {
            JSONObject jsonBody = new JSONObject();
            if (provider.equals(AIModelManager.PROVIDER_GEMINI)) {
                JSONArray contents = new JSONArray();
                JSONObject parts = new JSONObject().put("text", prompt);
                contents.put(new JSONObject().put("parts", new JSONArray().put(parts)));
                jsonBody.put("contents", contents);

                String url = "https://generativelanguage.googleapis.com/v1/models/" + modelId + ":generateContent?key=" + apiKey;
                Request request = new Request.Builder().url(url).post(RequestBody.create(jsonBody.toString(), JSON_TYPE)).build();

                client.newCall(request).enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) { callback.onFailure(e.getMessage()); }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body().string();
                        if (response.isSuccessful()) {
                            try {
                                String text = new JSONObject(body).getJSONArray("candidates").getJSONObject(0)
                                        .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
                                callback.onSuccess(text);
                            } catch (Exception e) { callback.onFailure("Parse error"); }
                        } else { callback.onFailure("HTTP " + response.code()); }
                    }
                });
            } else {
                // OpenAI Compatible (Groq, OpenRouter)
                jsonBody.put("model", modelId);
                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role", "user").put("content", prompt));
                jsonBody.put("messages", messages);

                Request.Builder builder = new Request.Builder()
                        .url(AIModelManager.getBaseUrl(provider))
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .post(RequestBody.create(jsonBody.toString(), JSON_TYPE));
                
                client.newCall(builder.build()).enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) { callback.onFailure(e.getMessage()); }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body().string();
                        if (response.isSuccessful()) {
                            try {
                                String text = new JSONObject(body).getJSONArray("choices").getJSONObject(0)
                                        .getJSONObject("message").getString("content");
                                callback.onSuccess(text);
                            } catch (Exception e) { callback.onFailure("Parse error"); }
                        } else { callback.onFailure("HTTP " + response.code()); }
                    }
                });
            }
        } catch (Exception e) { callback.onFailure(e.getMessage()); }
    }

    interface AIExplanationCallback {
        void onSuccess(String explanation);
        void onFailure(String error);
    }

    private void retryWrongQuestions(String quizData, int[] userAnswers) {
        if (quizData == null || userAnswers == null) return;
        try {
            JSONArray original = new JSONArray(quizData);
            JSONArray filtered = new JSONArray();
            
            for (int i = 0; i < original.length(); i++) {
                if (i < userAnswers.length) {
                    JSONObject obj = original.getJSONObject(i);
                    int correct = obj.has("answer") ? obj.getInt("answer") : 
                                 (obj.has("answers") ? obj.getJSONArray("answers").getInt(0) : -1);
                    
                    if (userAnswers[i] != correct) {
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
    }

    private void loadReviewData(String jsonData, int[] userAnswers) {
        if (jsonData == null || userAnswers == null) return;
        try {
            JSONArray array = new JSONArray(jsonData);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String question = obj.getString("question");
                JSONArray optionsArray = obj.getJSONArray("options");
                String[] options = new String[4];
                for (int j = 0; j < 4; j++) {
                    options[j] = optionsArray.getString(j);
                }
                
                int correct = obj.has("answer") ? obj.getInt("answer") : 
                             (obj.has("answers") ? obj.getJSONArray("answers").getInt(0) : 0);
                
                int userAns = (userAnswers != null && i < userAnswers.length) ? userAnswers[i] : -1;
                int difficulty = obj.optInt("difficulty", 2);
                ReviewItem item = new ReviewItem(question, options, correct, userAns, difficulty, i);
                item.aiExplanation = obj.optString("aiExplanation", null);
                reviewList.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ReviewItem {
        String question;
        String[] options;
        int correctIndex;
        int userIndex;
        int difficulty;
        String aiExplanation; // Lưu kết quả giải thích của AI
        int originalIndex;

        ReviewItem(String question, String[] options, int correctIndex, int userIndex, int difficulty, int originalIndex) {
            this.question = question;
            this.options = options;
            this.correctIndex = correctIndex;
            this.userIndex = userIndex;
            this.difficulty = difficulty;
            this.aiExplanation = null;
            this.originalIndex = originalIndex;
        }
    }

    private static class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {
        private Context context;
        private List<ReviewItem> items;
        private OnExplainClickListener explainClickListener;

        interface OnExplainClickListener {
            void onExplainClick(ReviewItem item);
        }

        ReviewAdapter(Context context, List<ReviewItem> items, OnExplainClickListener listener) {
            this.context = context;
            this.items = items;
            this.explainClickListener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_review_question, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ReviewItem item = items.get(position);
            holder.tvQuestion.setText((position + 1) + ". " + item.question);

            if (holder.tvDifficulty != null) {
                if (item.difficulty == 1) {
                    holder.tvDifficulty.setText("Dễ");
                    holder.tvDifficulty.setTextColor(ContextCompat.getColor(context, R.color.success));
                    holder.tvDifficulty.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.success_container)));
                } else if (item.difficulty == 3) {
                    holder.tvDifficulty.setText("Khó");
                    holder.tvDifficulty.setTextColor(ContextCompat.getColor(context, R.color.error));
                    holder.tvDifficulty.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.error_container)));
                } else {
                    holder.tvDifficulty.setText("Trung bình");
                    holder.tvDifficulty.setTextColor(ContextCompat.getColor(context, R.color.warning));
                    holder.tvDifficulty.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.warning_container)));
                }
            }

            TextView[] options = {holder.tvOpt0, holder.tvOpt1, holder.tvOpt2, holder.tvOpt3};

            for (int i = 0; i < 4; i++) {
                options[i].setText(item.options[i]);
                
                // Reset state
                options[i].setBackground(ContextCompat.getDrawable(context, R.drawable.bg_input_field));
                options[i].setTextColor(ContextCompat.getColor(context, R.color.on_surface));

                if (i == item.correctIndex) {
                    // Correct answer - always Green
                    options[i].setBackground(ContextCompat.getDrawable(context, R.drawable.bg_chip_success));
                    options[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.success_container)));
                    options[i].setTextColor(ContextCompat.getColor(context, R.color.success));
                } else if (i == item.userIndex && item.userIndex != item.correctIndex) {
                    // User was wrong - show Red
                    options[i].setBackground(ContextCompat.getDrawable(context, R.drawable.bg_chip_success)); // Using same drawable but different tint
                    options[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.error_container)));
                    options[i].setTextColor(ContextCompat.getColor(context, R.color.error));
                }
            }

            holder.btnExplain.setOnClickListener(v -> {
                if (explainClickListener != null) explainClickListener.onExplainClick(item);
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvQuestion, tvOpt0, tvOpt1, tvOpt2, tvOpt3, tvDifficulty;
            View btnExplain;

            ViewHolder(View itemView) {
                super(itemView);
                tvQuestion = itemView.findViewById(R.id.tv_review_question);
                tvOpt0 = itemView.findViewById(R.id.tv_review_option_0);
                tvOpt1 = itemView.findViewById(R.id.tv_review_option_1);
                tvOpt2 = itemView.findViewById(R.id.tv_review_option_2);
                tvOpt3 = itemView.findViewById(R.id.tv_review_option_3);
                tvDifficulty = itemView.findViewById(R.id.tv_difficulty);
                btnExplain = itemView.findViewById(R.id.btn_ai_explain);
            }
        }
    }
}
