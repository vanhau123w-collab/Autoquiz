package com.example.myapplication.main.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import org.zwobble.mammoth.DocumentConverter;
import org.zwobble.mammoth.Result;
import java.io.InputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.example.myapplication.R;
import com.example.myapplication.BuildConfig;
import com.example.myapplication.data.AppDatabase;
import com.example.myapplication.data.Quiz;
import com.example.myapplication.main.MainActivity;

public class CreateFragment extends Fragment {

    private EditText etContent, etQuizTitle;
    private Button btnGenerateQuiz;
    private Button btnQ5, btnQ10, btnQ15, btnQ20;
    private Button btnEasy, btnMedium, btnHard;
    private String selectedQuantity = "10";
    private String selectedDifficulty = "Medium";
    
    private final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;

    private ActivityResultLauncher<String[]> getContent = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    extractTextFromDocx(uri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create, container, false);

        etContent = view.findViewById(R.id.et_content);
        etQuizTitle = view.findViewById(R.id.et_quiz_title);
        btnGenerateQuiz = view.findViewById(R.id.btn_generate_quiz);
        
        view.findViewById(R.id.btn_upload_docx).setOnClickListener(v -> {
            getContent.launch(new String[]{"application/vnd.openxmlformats-officedocument.wordprocessingml.document"});
        });

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
            String title = etQuizTitle.getText().toString().trim();
            if (title.isEmpty()) title = "Quiz (AI generated)";
            
            if (content.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng dán nội dung", Toast.LENGTH_SHORT).show();
                return;
            }
            final String finalTitle = title;
            callGeminiAPI(content, selectedQuantity, selectedDifficulty, finalTitle);
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

    private void callGeminiAPI(String sourceContent, String quantity, String difficulty, String finalTitle) {
        btnGenerateQuiz.setEnabled(false);
        btnGenerateQuiz.setText("AI đang soạn câu hỏi...");

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        if (sourceContent.length() < 100) {
            Toast.makeText(getContext(), "Nội dung quá ngắn để tạo câu hỏi chất lượng. Vui lòng nhập thêm thông tin!", Toast.LENGTH_SHORT).show();
            btnGenerateQuiz.setEnabled(true);
            btnGenerateQuiz.setText("Generate Quiz");
            return;
        }

        String prompt = "### SYSTEM INSTRUCTIONS ###\n" +
                       "You are a strict Quiz Extraction Tool. Your only source of truth is the [SOURCE TEXT] provided below.\n" +
                       "1. ONLY create questions based on the [SOURCE TEXT].\n" +
                       "2. DO NOT use external knowledge. If the text doesn't contain enough info to create " + quantity + " questions, generate only as many as possible.\n" +
                       "3. Each question must have exactly 4 options. Options must be plausible but only one is correct according to the text.\n" +
                       "4. Response language: Vietnamese.\n" +
                       "5. Difficulty: " + difficulty + ".\n" +
                       "\n### [SOURCE TEXT] ###\n" + sourceContent + "\n" +
                       "\n### OUTPUT FORMAT ###\n" +
                       "Return ONLY a raw JSON array of objects. No markdown, no explanations.\n" +
                       "Format: [{\"question\": \"...\", \"options\": [\"A\", \"B\", \"C\", \"D\"], \"answer\": 0-3}]";

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
            String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + GEMINI_API_KEY;

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

                            String cleanedAiText = "";
                            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[[\\s\\S]*\\]");
                            java.util.regex.Matcher matcher = pattern.matcher(aiText);
                            if (matcher.find()) {
                                cleanedAiText = matcher.group();
                            } else {
                                cleanedAiText = aiText.replace("```json", "").replace("```", "").trim();
                            }

                            final String finalAiText = cleanedAiText;
                            getActivity().runOnUiThread(() -> {
                                if (finalAiText.isEmpty() || !finalAiText.startsWith("[")) {
                                    Toast.makeText(getContext(), "AI trả về dữ liệu không đúng định dạng, hãy thử lại", Toast.LENGTH_SHORT).show();
                                    btnGenerateQuiz.setEnabled(true);
                                    btnGenerateQuiz.setText("Generate Quiz");
                                    return;
                                }
                                saveQuizToLibrary(finalTitle, finalAiText, quantity);
                                Toast.makeText(getContext(), "Tạo thành công!", Toast.LENGTH_LONG).show();
                                btnGenerateQuiz.setEnabled(true);
                                btnGenerateQuiz.setText("Generate Quiz");
                                etContent.setText("");
                                etQuizTitle.setText("");
                            });
                        } catch (Exception e) {
                            getActivity().runOnUiThread(() -> {
                                btnGenerateQuiz.setEnabled(true);
                                btnGenerateQuiz.setText("Generate Quiz");
                                Toast.makeText(getContext(), "AI trả về dữ liệu lạ, hãy thử lại", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        String finalResponseBody = responseBody;
                        Log.e("GeminiAPI", "Lỗi: " + response.code() + " - " + finalResponseBody);
                        getActivity().runOnUiThread(() -> {
                            btnGenerateQuiz.setEnabled(true);
                            btnGenerateQuiz.setText("Generate Quiz");
                            
                            String errorMsg = "Lỗi " + response.code();
                            try {
                                JSONObject jsonResponse = new JSONObject(finalResponseBody);
                                if (jsonResponse.has("error")) {
                                    JSONObject errorObj = jsonResponse.getJSONObject("error");
                                    errorMsg += ": " + errorObj.getString("message");
                                } else {
                                    errorMsg += ": " + finalResponseBody;
                                }
                            } catch (Exception e) {
                                errorMsg += ": " + finalResponseBody;
                            }
                            
                            Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void extractTextFromDocx(Uri uri) {
        try {
            if (getContext() == null) return;
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                DocumentConverter converter = new DocumentConverter();
                Result<String> result = converter.extractRawText(inputStream);
                String text = result.getValue();
                etContent.setText(text);
                inputStream.close();
                Toast.makeText(getContext(), "Đã tải văn bản từ file!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Lỗi khi đọc file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveQuizToLibrary(String title, String aiData, String count) {
        if(getActivity() == null) return;
        try {
            // Lưu vào Room Database
            Quiz newQuiz = new Quiz(title, new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date()), count, aiData);
            AppDatabase.getInstance(getContext()).quizDao().insert(newQuiz);
            
            // Auto switch to library tab
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToTab(2); // Library tab
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
