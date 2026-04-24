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
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;


import com.example.myapplication.R;
import com.example.myapplication.BuildConfig;
import com.example.myapplication.data.AppDatabase;
import com.example.myapplication.data.Quiz;
import com.example.myapplication.main.AISettingsActivity;
import com.example.myapplication.main.MainActivity;
import com.example.myapplication.utils.AIModelManager;

public class CreateFragment extends Fragment {

    private EditText etContent, etQuizTitle, etCustomPrompt, etCustomQuantity;
    private Button btnGenerateQuiz;
    private Button btnQ5, btnQ10, btnQ15;
    private String selectedQuantity = "10";
    private AIModelManager aiModelManager;

    private final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;

    private ActivityResultLauncher<String[]> getDocx = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) extractTextFromDocx(uri); }
    );

    private ActivityResultLauncher<String[]> getPdf = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) extractTextFromPdf(uri); }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create, container, false);

        etContent        = view.findViewById(R.id.et_content);
        etQuizTitle      = view.findViewById(R.id.et_quiz_title);
        etCustomPrompt   = view.findViewById(R.id.et_custom_prompt);
        etCustomQuantity = view.findViewById(R.id.et_custom_quantity);
        btnGenerateQuiz  = view.findViewById(R.id.btn_generate_quiz);
        aiModelManager   = new AIModelManager(requireContext());

        // Collapsible custom prompt
        android.widget.LinearLayout llPromptContainer = view.findViewById(R.id.ll_prompt_container);
        android.widget.TextView tvArrow = view.findViewById(R.id.tv_prompt_arrow);
        view.findViewById(R.id.btn_toggle_prompt).setOnClickListener(v -> {
            boolean visible = llPromptContainer.getVisibility() == View.VISIBLE;
            llPromptContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
            tvArrow.setText(visible ? "▼" : "▲");
        });

        view.findViewById(R.id.btn_ai_settings).setOnClickListener(v ->
            startActivity(new android.content.Intent(getContext(), AISettingsActivity.class)));

        view.findViewById(R.id.btn_upload_docx).setOnClickListener(v ->
            getDocx.launch(new String[]{"application/vnd.openxmlformats-officedocument.wordprocessingml.document"}));

        view.findViewById(R.id.btn_upload_pdf).setOnClickListener(v ->
            getPdf.launch(new String[]{"application/pdf"}));

        // PDFBoxConfig.init removed as iTextG doesn't need static init

        btnQ5  = view.findViewById(R.id.btn_q5);
        btnQ10 = view.findViewById(R.id.btn_q10);
        btnQ15 = view.findViewById(R.id.btn_q15);

        setupSelectionButtons();

        btnGenerateQuiz.setOnClickListener(v -> {
            String content = etContent.getText().toString().trim();
            String title   = etQuizTitle.getText().toString().trim();
            if (title.isEmpty()) title = "Quiz (AI generated)";

            if (content.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng dán nội dung", Toast.LENGTH_SHORT).show();
                return;
            }

            // Ưu tiên ô tự nhập, giới hạn tối đa 20
            String customQty = etCustomQuantity.getText().toString().trim();
            if (!customQty.isEmpty()) {
                int n = Integer.parseInt(customQty);
                if (n < 1) n = 1;
                if (n > 20) { n = 20; etCustomQuantity.setText("20"); }
                selectedQuantity = String.valueOf(n);
                // Bỏ chọn các chip
                btnQ5.setSelected(false); btnQ10.setSelected(false); btnQ15.setSelected(false);
            }

            final String finalTitle = title;
            callAI(content, selectedQuantity, finalTitle);
        });

        return view;
    }

    private void setupSelectionButtons() {
        View.OnClickListener quantityListener = v -> {
            btnQ5.setSelected(false); btnQ10.setSelected(false); btnQ15.setSelected(false);
            v.setSelected(true);
            selectedQuantity = ((Button) v).getText().toString();
            etCustomQuantity.setText(""); // xóa ô tự nhập khi chọn chip
        };
        btnQ5.setOnClickListener(quantityListener);
        btnQ10.setOnClickListener(quantityListener);
        btnQ15.setOnClickListener(quantityListener);
        btnQ10.setSelected(true);
    }

    private void callAI(String content, String quantity, String title) {
        String provider    = aiModelManager.getSelectedProvider();
        String displayName = aiModelManager.getSelectedModel();
        String modelId     = AIModelManager.toApiModelId(provider, displayName);
        String baseUrl     = AIModelManager.getBaseUrl(provider);
        String fullUrl     = provider.equals(AIModelManager.PROVIDER_GEMINI)
                ? "https://generativelanguage.googleapis.com/v1/models/" + modelId + ":generateContent"
                : baseUrl;
        Log.d("GeminiAPI", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Log.d("GeminiAPI", "Provider : " + provider);
        Log.d("GeminiAPI", "Model ID : " + modelId);
        Log.d("GeminiAPI", "URL      : " + fullUrl);
        Log.d("GeminiAPI", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        switch (provider) {
            case AIModelManager.PROVIDER_GROQ:
            case AIModelManager.PROVIDER_OPENROUTER:
                callOpenAICompatible(content, quantity, title, provider, modelId);
                break;
            default:
                callGeminiAPI(content, quantity, title, modelId);
                break;
        }
    }

    private void callGeminiAPI(String sourceContent, String quantity, String finalTitle, String modelId) {
        btnGenerateQuiz.setEnabled(false);
        btnGenerateQuiz.setText("AI đang soạn câu hỏi...");

        // Lấy key: ưu tiên key từ manager, fallback về BuildConfig
        String apiKey = aiModelManager.nextKey(AIModelManager.PROVIDER_GEMINI);
        if (apiKey == null || apiKey.isEmpty()) apiKey = BuildConfig.GEMINI_API_KEY;
        final String finalApiKey = apiKey;

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");



        String prompt = buildPrompt(sourceContent, quantity);
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
            String url = "https://generativelanguage.googleapis.com/v1/models/" + modelId + ":generateContent?key=" + finalApiKey;

            Request request = new Request.Builder().url(url).post(body).build();
            Log.d("GeminiAPI", "Gửi request tới: " + url);
            Log.d("GeminiAPI", "Prompt length: " + prompt.length() + " chars");

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("GeminiAPI", "Network failure", e);
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
                    Log.d("GeminiAPI", "Response code: " + response.code());
                    if (response.isSuccessful()) {
                        Log.d("GeminiAPI", "Response body: " + responseBody);
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String aiText = jsonResponse.getJSONArray("candidates")
                                     .getJSONObject(0)
                                     .getJSONObject("content")
                                     .getJSONArray("parts")
                                     .getJSONObject(0)
                                     .getString("text");
                            Log.d("GeminiAPI", "AI raw text: " + aiText);
                            handleAIText(aiText, finalTitle, quantity);
                        } catch (Exception e) {
                            Log.e("GeminiAPI", "Parse error", e);
                            Log.e("GeminiAPI", "Raw response: " + responseBody);
                            getActivity().runOnUiThread(() -> {
                                resetBtn();
                                Toast.makeText(getContext(), "AI trả về dữ liệu lạ, hãy thử lại", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        String finalResponseBody = responseBody;
                        Log.e("GeminiAPI", "HTTP Error " + response.code() + " - " + finalResponseBody);
                        getActivity().runOnUiThread(() -> {
                            resetBtn();
                            String errorMsg = "Lỗi " + response.code();
                            try {
                                JSONObject jsonResponse = new JSONObject(finalResponseBody);
                                if (jsonResponse.has("error")) {
                                    errorMsg += ": " + jsonResponse.getJSONObject("error").getString("message");
                                }
                            } catch (Exception e) { errorMsg += ": " + finalResponseBody; }
                            Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void callOpenAICompatible(String sourceContent, String quantity,
                                       String finalTitle, String provider, String model) {
        String apiKey = aiModelManager.nextKey(provider);
        if (apiKey == null || apiKey.isEmpty()) {
            Toast.makeText(getContext(), "Chưa có API key cho " + provider + ". Vào AI Settings để thêm.", Toast.LENGTH_LONG).show();
            return;
        }
        btnGenerateQuiz.setEnabled(false);
        btnGenerateQuiz.setText("AI đang soạn câu hỏi...");

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        String prompt = buildPrompt(sourceContent, quantity);
        try {
            org.json.JSONObject body = new org.json.JSONObject();
            body.put("model", model);
            org.json.JSONArray messages = new org.json.JSONArray();
            org.json.JSONObject msg = new org.json.JSONObject();
            msg.put("role", "user");
            msg.put("content", prompt);
            messages.put(msg);
            body.put("messages", messages);

            okhttp3.RequestBody reqBody = okhttp3.RequestBody.create(body.toString(),
                    okhttp3.MediaType.get("application/json; charset=utf-8"));
            okhttp3.Request.Builder reqBuilder = new okhttp3.Request.Builder()
                    .url(AIModelManager.getBaseUrl(provider))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(reqBody);
            if (provider.equals(AIModelManager.PROVIDER_OPENROUTER)) {
                reqBuilder.addHeader("HTTP-Referer", "https://autoquiz.app");
            }

            Log.d("GeminiAPI", provider + " request → model: " + model);
            client.newCall(reqBuilder.build()).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    Log.e("GeminiAPI", provider + " network failure", e);
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> { resetBtn(); Toast.makeText(getContext(), "Lỗi mạng: " + e.getMessage(), Toast.LENGTH_LONG).show(); });
                }
                @Override public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                    if (getActivity() == null) return;
                    String rb = response.body() != null ? response.body().string() : "";
                    Log.d("GeminiAPI", provider + " response " + response.code() + ": " + rb);

                    if (response.code() == 429) {
                        // Parse message chi tiết từ provider
                        String userMsg = "Rate limit! Đổi model khác hoặc thử lại sau.";
                        try {
                            org.json.JSONObject err = new org.json.JSONObject(rb)
                                    .getJSONObject("error");
                            // OpenRouter trả message trong metadata.raw
                            if (err.has("metadata")) {
                                String raw = err.getJSONObject("metadata").optString("raw", "");
                                if (!raw.isEmpty()) userMsg = raw;
                            } else {
                                userMsg = err.optString("message", userMsg);
                            }
                        } catch (Exception ignored) {}
                        Log.e("GeminiAPI", provider + " 429: " + userMsg);
                        final String finalMsg = userMsg;
                        getActivity().runOnUiThread(() -> {
                            resetBtn();
                            Toast.makeText(getContext(), "⚠️ " + finalMsg, Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    if (response.isSuccessful()) {
                        try {
                            org.json.JSONObject respJson = new org.json.JSONObject(rb);
                            // Server echo: xác nhận model thực tế đã chạy
                            String serverModel = respJson.optString("model", "unknown");
                            Log.d("GeminiAPI", "✅ Server confirmed model: " + serverModel);
                            String aiText = respJson
                                    .getJSONArray("choices").getJSONObject(0)
                                    .getJSONObject("message").getString("content");
                            handleAIText(aiText, finalTitle, quantity);
                        } catch (Exception e) {
                            Log.e("GeminiAPI", provider + " parse error", e);
                            getActivity().runOnUiThread(() -> { resetBtn(); Toast.makeText(getContext(), "Parse lỗi", Toast.LENGTH_SHORT).show(); });
                        }
                    } else {
                        Log.e("GeminiAPI", provider + " HTTP " + response.code() + " - " + rb);
                        getActivity().runOnUiThread(() -> { resetBtn(); Toast.makeText(getContext(), "Lỗi " + response.code(), Toast.LENGTH_LONG).show(); });
                    }
                }
            });
        } catch (Exception e) { e.printStackTrace(); resetBtn(); }
    }

    private String buildPrompt(String sourceContent, String quantity) {
        String custom = etCustomPrompt != null ? etCustomPrompt.getText().toString().trim() : "";
        if (!custom.isEmpty()) {
            return custom + "\n\n### [SOURCE TEXT] ###\n" + sourceContent +
                   "\n\n### OUTPUT FORMAT ###\n" +
                   "Return ONLY a raw JSON array. No markdown.\n" +
                   "Format: [{\"question\": \"...\", \"options\": [\"A\", \"B\", \"C\", \"D\"], \"type\": \"single|multiple\", \"answers\": [0,1,2,3], \"difficulty\": 1-3}]";
        }
        return "Tạo " + quantity + " câu hỏi trắc nghiệm từ văn bản sau. Yêu cầu:\n" +
               "1. CHỈ dựa vào nội dung văn bản, KHÔNG dùng kiến thức bên ngoài\n" +
               "2. Phải chia bộ câu hỏi thành 3 mức độ: Dễ (1), Trung bình (2), Khó (3). Tỷ lệ khoảng 30% dễ, 40% trung bình, 30% khó.\n" +
               "3. Mỗi câu có đúng 4 đáp án\n" +
               "4. Có 2 loại: \"single\" (1 đáp án đúng) và \"multiple\" (nhiều đáp án đúng)\n" +
               "5. Khoảng 70% câu single, 30% câu multiple\n" +
               "6. Trả lời bằng tiếng Việt\n" +
               "7. Trả về JSON array thuần, KHÔNG có markdown, không giải thích\n\n" +
               "Văn bản:\n" + sourceContent + "\n\n" +
               "Format JSON: [{\"question\":\"...\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"type\":\"single\",\"answers\":[0],\"difficulty\":1}]\n" +
               "Lưu ý: \"difficulty\" phải là số 1, 2 hoặc 3. \"answers\" luôn là mảng các số (index đáp án từ 0-3).";
    }

    private void handleAIText(String aiText, String finalTitle, String quantity) {
        if (getActivity() == null) return;
        String cleaned = "";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\[[\\s\\S]*\\]").matcher(aiText);
        if (matcher.find()) cleaned = matcher.group();
        else cleaned = aiText.replace("```json", "").replace("```", "").trim();

        Log.d("GeminiAPI", "Cleaned JSON: " + cleaned);
        final String finalCleaned = cleaned;
        getActivity().runOnUiThread(() -> {
            if (finalCleaned.isEmpty() || !finalCleaned.startsWith("[")) {
                Log.e("GeminiAPI", "JSON không hợp lệ: " + finalCleaned);
                Toast.makeText(getContext(), "AI trả về dữ liệu không đúng định dạng", Toast.LENGTH_SHORT).show();
                resetBtn(); return;
            }
            
            // Shuffle questions randomly
            String shuffledJson = shuffleQuestions(finalCleaned);
            saveQuizToLibrary(finalTitle, shuffledJson, quantity);
            Toast.makeText(getContext(), "Tạo thành công!", Toast.LENGTH_LONG).show();
            resetBtn();
            etContent.setText("");
            etQuizTitle.setText("");
        });
    }

    private String shuffleQuestions(String jsonData) {
        try {
            JSONArray array = new JSONArray(jsonData);
            java.util.List<JSONObject> list = new java.util.ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getJSONObject(i));
            }
            java.util.Collections.shuffle(list);
            JSONArray shuffled = new JSONArray();
            for (JSONObject obj : list) {
                shuffled.put(obj);
            }
            return shuffled.toString();
        } catch (Exception e) {
            Log.e("GeminiAPI", "Shuffle error, returning original", e);
            return jsonData;
        }
    }

    private void resetBtn() {
        btnGenerateQuiz.setEnabled(true);
        btnGenerateQuiz.setText("Generate Quiz");
    }

    private void extractTextFromPdf(Uri uri) {
        try {
            if (getContext() == null) return;
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                PdfReader reader = new PdfReader(inputStream);
                StringBuilder builder = new StringBuilder();
                int numPages = reader.getNumberOfPages();
                
                for (int i = 1; i <= numPages; i++) {
                    builder.append(PdfTextExtractor.getTextFromPage(reader, i)).append("\n");
                }
                
                etContent.setText(builder.toString());
                reader.close();
                inputStream.close();
                Toast.makeText(getContext(), "Đã tải văn bản từ file PDF (iTextG)!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Lỗi khi đọc file PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
            SharedPreferences sharedPref = getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String email = sharedPref.getString("CurrentUserEmail", "");
            
            Quiz newQuiz = new Quiz(title, new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date()), count, aiData, email);
            AppDatabase.getInstance(getContext()).quizDao().insert(newQuiz);
            
            // Auto switch to library tab
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToTab(2); // Library tab
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
