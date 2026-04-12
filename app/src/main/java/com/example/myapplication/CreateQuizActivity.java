package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateQuizActivity extends AppCompatActivity {

    private EditText etQuizTitle, etContent;
    private Spinner spinnerQuantity;
    private Button btnGenerateQuiz;
    
    // API Key của bạn
    private final String GEMINI_API_KEY = "AIzaSyAYiq07tEJCOH4SviWhAIXS4XFNcgJhfVY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_quiz);

        etQuizTitle = findViewById(R.id.et_quiz_title);
        etContent = findViewById(R.id.et_content);
        spinnerQuantity = findViewById(R.id.spinner_quantity);
        btnGenerateQuiz = findViewById(R.id.btn_generate_quiz);

        String[] quantities = {"5 Câu hỏi", "10 Câu hỏi", "15 Câu hỏi", "20 Câu hỏi"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, quantities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerQuantity.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        btnGenerateQuiz.setOnClickListener(v -> {
            String title = etQuizTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();
            String quantity = spinnerQuantity.getSelectedItem().toString().split(" ")[0];

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ tên và nội dung", Toast.LENGTH_SHORT).show();
                return;
            }

            callGeminiAPI(title, content, quantity);
        });
    }

    private void callGeminiAPI(String title, String sourceContent, String quantity) {
        btnGenerateQuiz.setEnabled(false);
        btnGenerateQuiz.setText("AI đang soạn câu hỏi...");

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        String prompt = "Dựa vào nội dung sau: " + sourceContent + 
                       "\nHãy tạo " + quantity + " câu hỏi trắc nghiệm tiếng Việt." +
                       "\nĐịnh dạng trả về duy nhất là JSON array, mỗi phần tử có: 'question' (String), 'options' (mảng 4 xâu), 'answer' (số từ 0-3).";

        try {
            JSONObject jsonBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject parts = new JSONObject();
            parts.put("text", prompt);
            JSONObject contentObj = new JSONObject();
            contentObj.put("parts", new JSONArray().put(parts));
            contents.put(contentObj);
            jsonBody.put("contents", contents);

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
            
            // QUAY LẠI v1beta ĐỂ TRÁNH LỖI 404
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY;

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        btnGenerateQuiz.setEnabled(true);
                        btnGenerateQuiz.setText("Thử lại");
                        Toast.makeText(CreateQuizActivity.this, "Lỗi mạng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String aiText = jsonResponse.getJSONArray("candidates")
                                    .getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text");

                            // Loại bỏ markdown nếu có
                            aiText = aiText.replace("```json", "").replace("```", "").trim();

                            String finalAiText = aiText;
                            runOnUiThread(() -> {
                                saveQuizToLibrary(title, finalAiText, quantity);
                                Toast.makeText(CreateQuizActivity.this, "Tạo thành công bộ câu hỏi!", Toast.LENGTH_LONG).show();
                                finish();
                            });
                        } catch (Exception e) {
                            Log.e("GeminiError", "Parse error: " + responseBody);
                            runOnUiThread(() -> {
                                btnGenerateQuiz.setEnabled(true);
                                Toast.makeText(CreateQuizActivity.this, "AI trả về dữ liệu lạ, hãy thử lại", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        // LOG CHI TIẾT ĐỂ BIẾT TẠI SAO BỊ TỪ CHỐI
                        Log.e("GeminiError", "HTTP Code: " + response.code() + " | Body: " + responseBody);
                        runOnUiThread(() -> {
                            btnGenerateQuiz.setEnabled(true);
                            btnGenerateQuiz.setText("Thử lại");
                            if (response.code() == 403) {
                                Toast.makeText(CreateQuizActivity.this, "Lỗi 403: API Key chưa hợp lệ hoặc Vùng không hỗ trợ", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(CreateQuizActivity.this, "Lỗi AI (" + response.code() + "). Hãy kiểm tra Logcat!", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveQuizToLibrary(String title, String aiData, String count) {
        try {
            SharedPreferences sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String quizzesJson = sharedPref.getString("QuizList", "[]");
            JSONArray quizArray = new JSONArray(quizzesJson);

            JSONObject newQuiz = new JSONObject();
            newQuiz.put("title", title);
            newQuiz.put("date", new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date()));
            newQuiz.put("count", count);
            newQuiz.put("raw_data", aiData);
            
            quizArray.put(newQuiz);
            sharedPref.edit().putString("QuizList", quizArray.toString()).apply();
            sharedPref.edit().putBoolean("HasNewQuiz", true).apply();
        } catch (Exception e) { e.printStackTrace(); }
    }
}