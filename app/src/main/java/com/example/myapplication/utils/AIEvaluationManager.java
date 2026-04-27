package com.example.myapplication.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.data.AppDatabase;
import com.example.myapplication.data.Quiz;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIEvaluationManager {

    public interface EvaluationCallback {
        void onCompleted(boolean success, String resultOrError);
    }

    public static void evaluateQuiz(Activity activity, int quizId, String quizJson, int[] userAnswers, String title, EvaluationCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_ai_evaluating, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();

        TextView tvStatus = dialogView.findViewById(R.id.tv_eval_status);
        TextView tvEvalContent = dialogView.findViewById(R.id.tv_eval_content);
        Button btnContinue = dialogView.findViewById(R.id.btn_continue_eval);
        android.widget.ProgressBar pbLoading = dialogView.findViewById(R.id.pb_eval_loading);

        AIModelManager aiModelManager = new AIModelManager(activity);
        String provider = aiModelManager.getSelectedProvider();
        String modelId = AIModelManager.toApiModelId(provider, aiModelManager.getSelectedModel());
        String apiKey = aiModelManager.nextKey(provider);

        if (apiKey == null || apiKey.isEmpty()) {
            pbLoading.setVisibility(View.GONE);
            tvStatus.setText("⚠️ Chưa có API Key");
            tvEvalContent.setText("Vui lòng cài đặt AI Model trong Settings.");
            tvEvalContent.setVisibility(View.VISIBLE);
            btnContinue.setVisibility(View.VISIBLE);
            btnContinue.setText("Đóng / Bỏ qua");
            btnContinue.setOnClickListener(v -> {
                dialog.dismiss();
                if (callback != null) callback.onCompleted(false, "No API Key");
            });
            return;
        }

        StringBuilder summary = new StringBuilder();
        int correct = 0;
        int total = 0;
        try {
            JSONArray questions = new JSONArray(quizJson);
            total = questions.length();
            for (int i = 0; i < questions.length() && i < userAnswers.length; i++) {
                JSONObject q = questions.getJSONObject(i);
                int userAns = userAnswers[i];
                JSONArray options = q.getJSONArray("options");
                int difficulty = q.optInt("difficulty", 2);
                String diffText = difficulty == 1 ? "Dễ" : (difficulty == 3 ? "Khó" : "TB");

                int correctIdx = q.has("answer") ? q.getInt("answer") :
                        (q.has("answers") ? q.getJSONArray("answers").getInt(0) : -1);

                boolean isCorrect = (userAns == correctIdx);
                if (isCorrect) correct++;

                String userAnsText = (userAns >= 0 && userAns < options.length()) ? options.getString(userAns) : "Không chọn";
                String correctAnsText = (correctIdx >= 0 && correctIdx < options.length()) ? options.getString(correctIdx) : "N/A";

                summary.append("Câu ").append(i + 1).append(" [").append(diffText).append("]: ")
                        .append(q.getString("question")).append("\n");
                summary.append("  → Trả lời: ").append(userAnsText);
                summary.append(isCorrect ? " ✓" : " ✗ (Đáp án đúng: " + correctAnsText + ")");
                summary.append("\n\n");
            }
            summary.insert(0, "Kết quả bộ quiz: " + title + "\nĐạt: " + correct + "/" + total + " câu đúng.\n\n");
        } catch (Exception e) { e.printStackTrace(); }

        String prompt = "Bạn là một giáo viên chuyên nghiệp. Dựa trên kết quả bài quiz dưới đây, hãy đánh giá người học:\n\n" +
                summary.toString() + "\n\n" +
                "Hãy viết đánh giá NGẮN GỌN (tối đa 200 từ) theo format sau:\n" +
                "📊 ĐÁNH GIÁ TỔNG QUAN: (1-2 câu nhận xét chung)\n" +
                "✅ ĐIỂM MẠNH: (liệt kê các chủ đề/dạng câu mà người học nắm tốt)\n" +
                "⚠️ ĐIỂM YẾU: (liệt kê các chủ đề/dạng câu cần cải thiện)\n" +
                "📚 GỢI Ý ÔN TẬP: (hướng dẫn cụ thể nên ôn tập gì)\n" +
                "Viết bằng tiếng Việt, thân thiện và khích lệ.";

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

        try {
            JSONObject jsonBody = new JSONObject();
            Request request;

            if (provider.equals(AIModelManager.PROVIDER_GEMINI)) {
                JSONArray contents = new JSONArray();
                JSONObject parts = new JSONObject().put("text", prompt);
                contents.put(new JSONObject().put("parts", new JSONArray().put(parts)));
                jsonBody.put("contents", contents);
                String url = "https://generativelanguage.googleapis.com/v1/models/" + modelId + ":generateContent?key=" + apiKey;
                request = new Request.Builder().url(url)
                        .post(RequestBody.create(jsonBody.toString(), JSON_TYPE)).build();
            } else {
                jsonBody.put("model", modelId);
                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role", "user").put("content", prompt));
                jsonBody.put("messages", messages);
                request = new Request.Builder()
                        .url(AIModelManager.getBaseUrl(provider))
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .post(RequestBody.create(jsonBody.toString(), JSON_TYPE)).build();
            }

            final String providerFinal = provider;
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    activity.runOnUiThread(() -> {
                        pbLoading.setVisibility(View.GONE);
                        tvStatus.setText("❌ Lỗi kết nối");
                        tvEvalContent.setText(e.getMessage());
                        tvEvalContent.setVisibility(View.VISIBLE);
                        btnContinue.setVisibility(View.VISIBLE);
                        btnContinue.setText("Đóng / Bỏ qua");
                        btnContinue.setOnClickListener(v -> {
                            dialog.dismiss();
                            if (callback != null) callback.onCompleted(false, e.getMessage());
                        });
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body().string();
                    String evalText = null;
                    if (response.isSuccessful()) {
                        try {
                            if (providerFinal.equals(AIModelManager.PROVIDER_GEMINI)) {
                                evalText = new JSONObject(body).getJSONArray("candidates").getJSONObject(0)
                                        .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
                            } else {
                                evalText = new JSONObject(body).getJSONArray("choices").getJSONObject(0)
                                        .getJSONObject("message").getString("content");
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }

                    if (evalText != null) {
                        final String finalEval = evalText;
                        // Save to DB if quizId is valid
                        if (quizId != -1) {
                            new Thread(() -> {
                                Quiz updatedQuiz = AppDatabase.getInstance(activity).quizDao().getQuizById(quizId);
                                if (updatedQuiz != null) {
                                    updatedQuiz.setAiEvaluation(finalEval);
                                    AppDatabase.getInstance(activity).quizDao().update(updatedQuiz);
                                }
                            }).start();
                        }

                        activity.runOnUiThread(() -> {
                            pbLoading.setVisibility(View.GONE);
                            tvStatus.setText("✅ Phân tích hoàn tất!");
                            String html = finalEval
                                    .replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>")
                                    .replaceAll("\\n", "<br>");
                            tvEvalContent.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT));
                            tvEvalContent.setVisibility(View.VISIBLE);
                            btnContinue.setVisibility(View.VISIBLE);
                            btnContinue.setText(quizId != -1 && callback != null ? "Tiếp tục" : "Đóng");
                            btnContinue.setOnClickListener(v -> {
                                dialog.dismiss();
                                if (callback != null) callback.onCompleted(true, finalEval);
                            });
                        });
                    } else {
                        activity.runOnUiThread(() -> {
                            pbLoading.setVisibility(View.GONE);
                            tvStatus.setText("❌ Không thể đánh giá");
                            tvEvalContent.setVisibility(View.VISIBLE);
                            tvEvalContent.setText("HTTP " + response.code() + "\n" + body);
                            btnContinue.setVisibility(View.VISIBLE);
                            btnContinue.setText("Đóng / Bỏ qua");
                            btnContinue.setOnClickListener(v -> {
                                dialog.dismiss();
                                if (callback != null) callback.onCompleted(false, "Error " + response.code());
                            });
                        });
                    }
                }
            });
        } catch (Exception e) {
            dialog.dismiss();
            Toast.makeText(activity, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            if (callback != null) callback.onCompleted(false, e.getMessage());
        }
    }
}
