package com.example.myapplication.quiz;

import android.content.Context;
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
import org.json.JSONArray;
import org.json.JSONObject;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        rvReview = findViewById(R.id.rv_review);
        btnClose = findViewById(R.id.btn_close_review);

        String quizData = getIntent().getStringExtra("QUIZ_DATA");
        int[] userAnswers = getIntent().getIntArrayExtra("USER_ANSWERS");

        loadReviewData(quizData, userAnswers);

        rvReview.setLayoutManager(new LinearLayoutManager(this));
        rvReview.setAdapter(new ReviewAdapter(this, reviewList));

        btnClose.setOnClickListener(v -> finish());
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
                int correct = obj.getInt("answer");
                int userAns = (userAnswers != null && i < userAnswers.length) ? userAnswers[i] : -1;
                reviewList.add(new ReviewItem(question, options, correct, userAns));
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

        ReviewItem(String question, String[] options, int correctIndex, int userIndex) {
            this.question = question;
            this.options = options;
            this.correctIndex = correctIndex;
            this.userIndex = userIndex;
        }
    }

    private static class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {
        private Context context;
        private List<ReviewItem> items;

        ReviewAdapter(Context context, List<ReviewItem> items) {
            this.context = context;
            this.items = items;
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
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvQuestion, tvOpt0, tvOpt1, tvOpt2, tvOpt3;

            ViewHolder(View itemView) {
                super(itemView);
                tvQuestion = itemView.findViewById(R.id.tv_review_question);
                tvOpt0 = itemView.findViewById(R.id.tv_review_option_0);
                tvOpt1 = itemView.findViewById(R.id.tv_review_option_1);
                tvOpt2 = itemView.findViewById(R.id.tv_review_option_2);
                tvOpt3 = itemView.findViewById(R.id.tv_review_option_3);
            }
        }
    }
}
