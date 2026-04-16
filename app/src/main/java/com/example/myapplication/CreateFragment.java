package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

public class CreateFragment extends Fragment {

    private EditText etContent;
    private Button btnGenerateQuiz;
    private Button btnQ5, btnQ10, btnQ15, btnQ20;
    private Button btnEasy, btnMedium, btnHard;
    private String selectedQuantity = "10";
    private String selectedDifficulty = "Medium";
    
    private final String GEMINI_API_KEY = "AIzaSyAYiq07tEJCOH4SviWhAIXS4XFNcgJhfVY";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create, container, false);

        etContent = view.findViewById(R.id.et_content);
        btnGenerateQuiz = view.findViewById(R.id.btn_generate_quiz);

        btnQ5 = view.findViewById(R.id.btn_q5);
        btnQ10 = view.findViewById(R.id.btn_q10);
        btnQ15 = view.findViewById(R.id.btn_q15);
        btnQ20 = view.findViewById(R.id.btn_q20);

        btnEasy = view.findViewById(R.id.btn_easy);
        btnMedium = view.findViewById(R.id.btn_medium);
        btnHard = view.findViewById(R.id.btn_hard);

        setupSelectionButtons();

        btnGenerateQuiz.setOnClickListener(v -> {
            String content = etContent.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng dán nội dung", Toast.LENGTH_SHORT).show();
                return;
            }
            callGeminiAPI(content, selectedQuantity, selectedDifficulty);
        });

        return view;
    }

    private void setupSelectionButtons() {
        View.OnClickListener quantityListener = v -> {
            btnQ5.setSelected(false); btnQ10.setSelected(false);
            btnQ15.setSelected(false); btnQ20.setSelected(false);
            v.setSelected(true);
            selectedQuantity = ((Button)v).getText().toString();
        };
        btnQ5.setOnClickListener(quantityListener);
        btnQ10.setOnClickListener(quantityListener);
        btnQ15.setOnClickListener(quantityListener);
        btnQ20.setOnClickListener(quantityListener);
        btnQ10.setSelected(true);

        View.OnClickListener diffListener = v -> {
            btnEasy.setSelected(false); btnMedium.setSelected(false); btnHard.setSelected(false);
            v.setSelected(true);
            selectedDifficulty = ((Button)v).getText().toString();
        };
        btnEasy.setOnClickListener(diffListener);
        btnMedium.setOnClickListener(diffListener);
        btnHard.setOnClickListener(diffListener);
        btnMedium.setSelected(true);
    }

    private void callGeminiAPI(String sourceContent, String quantity, String difficulty) {
        btnGenerateQuiz.setEnabled(false);
        btnGenerateQuiz.setText("AI đang soạn câu hỏi...");

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        String prompt = "Dựa vào nội dung sau: " + sourceContent + 
                       "\nHãy tạo " + quantity + " câu hỏi trắc nghiệm tiếng Việt với mức độ: " + difficulty + "." +
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
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY;

            Request request = new Request.Builder().url(url).post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if(getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        btnGenerateQuiz.setEnabled(true);
                        btnGenerateQuiz.setText("Generate Quiz");
                        Toast.makeText(getContext(), "Lỗi mạng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if(getActivity() == null) return;
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

                            aiText = aiText.replace("```json", "").replace("```", "").trim();

                            String finalAiText = aiText;
                            getActivity().runOnUiThread(() -> {
                                saveQuizToLibrary("Quiz (AI generated)", finalAiText, quantity);
                                Toast.makeText(getContext(), "Tạo thành công!", Toast.LENGTH_LONG).show();
                                btnGenerateQuiz.setEnabled(true);
                                btnGenerateQuiz.setText("Generate Quiz");
                                etContent.setText("");
                            });
                        } catch (Exception e) {
                            getActivity().runOnUiThread(() -> {
                                btnGenerateQuiz.setEnabled(true);
                                btnGenerateQuiz.setText("Generate Quiz");
                                Toast.makeText(getContext(), "AI trả về dữ liệu lạ, hãy thử lại", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        getActivity().runOnUiThread(() -> {
                            btnGenerateQuiz.setEnabled(true);
                            btnGenerateQuiz.setText("Generate Quiz");
                            Toast.makeText(getContext(), "Lỗi AI (" + response.code() + ")", Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveQuizToLibrary(String title, String aiData, String count) {
        if(getActivity() == null) return;
        try {
            SharedPreferences sharedPref = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
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
            
            // Auto switch to library tab
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToTab(2); // Library tab
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
