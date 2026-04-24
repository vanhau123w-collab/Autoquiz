package com.example.myapplication.main.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.List;
import java.util.Locale;

import com.example.myapplication.R;
import com.example.myapplication.data.AppDatabase;
import com.example.myapplication.data.Quiz;
import com.example.myapplication.quiz.QuizActivity;

public class LibraryFragment extends Fragment {

    private TextView tvTotalQuizzes;
    private View emptyState;
    private LinearLayout quizListContainer;
    
    // Selection state
    private boolean isEditMode = false;
    private java.util.HashSet<Integer> selectedQuizIds = new java.util.HashSet<>();
    private View layoutSelectionActions;
    private TextView tvSelectedCount;
    private TextView tvMultiSelect;
    private TextView tvClearAll;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        tvTotalQuizzes = view.findViewById(R.id.tv_quiz_count);
        emptyState = view.findViewById(R.id.empty_state);
        quizListContainer = view.findViewById(R.id.library_container);
        
        layoutSelectionActions = view.findViewById(R.id.layout_selection_actions);
        tvSelectedCount = view.findViewById(R.id.tv_selected_count);
        tvMultiSelect = view.findViewById(R.id.tv_multi_select);
        tvClearAll = view.findViewById(R.id.tv_clear_all);

        tvClearAll.setOnClickListener(v -> showDeleteAllConfirm());
        tvMultiSelect.setOnClickListener(v -> toggleEditMode(true));
        
        view.findViewById(R.id.btn_cancel_selection).setOnClickListener(v -> toggleEditMode(false));
        view.findViewById(R.id.btn_delete_selected).setOnClickListener(v -> showDeleteSelectedConfirm());

        loadQuizzes();
        return view;
    }

    private void toggleEditMode(boolean enable) {
        isEditMode = enable;
        selectedQuizIds.clear();
        layoutSelectionActions.setVisibility(enable ? View.VISIBLE : View.GONE);
        tvMultiSelect.setVisibility(enable ? View.GONE : View.VISIBLE);
        tvClearAll.setVisibility(enable ? View.GONE : View.VISIBLE);
        updateSelectedCountText();
        loadQuizzes(); // Refresh UI to show checkboxes
    }

    private void updateSelectedCountText() {
        tvSelectedCount.setText("Đã chọn: " + selectedQuizIds.size());
    }

    private void showDeleteAllConfirm() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Xóa tất cả")
            .setMessage("Bạn có chắc chắn muốn xóa toàn bộ bộ quiz trong thư viện?")
            .setPositiveButton("Xóa hết", (dialog, which) -> deleteAllQuizzes())
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void showDeleteSelectedConfirm() {
        if (selectedQuizIds.isEmpty()) return;
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Xóa mục đã chọn")
            .setMessage("Bạn có chắc chắn muốn xóa " + selectedQuizIds.size() + " bộ quiz đã chọn?")
            .setPositiveButton("Xóa", (dialog, which) -> deleteSelectedQuizzes())
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void deleteAllQuizzes() {
        try {
            android.content.SharedPreferences sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String email = sharedPref.getString("CurrentUserEmail", "");
            AppDatabase.getInstance(getContext()).quizDao().deleteAllQuizzes(email);
            loadQuizzes();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteSelectedQuizzes() {
        try {
            for (Integer id : selectedQuizIds) {
                Quiz q = new Quiz("", "", "", "", "");
                q.setId(id);
                AppDatabase.getInstance(getContext()).quizDao().delete(q);
            }
            toggleEditMode(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadQuizzes() {
        if (getContext() == null || quizListContainer == null) return;
        quizListContainer.removeAllViews();
        
        try {
            android.content.SharedPreferences sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String email = sharedPref.getString("CurrentUserEmail", "");
            
            List<Quiz> quizList = AppDatabase.getInstance(getContext()).quizDao().getAllQuizzes(email);
            int count = quizList.size();

            if (count == 0) {
                if (emptyState.getParent() != null) {
                    ((ViewGroup) emptyState.getParent()).removeView(emptyState);
                }
                quizListContainer.addView(emptyState);
                emptyState.setVisibility(View.VISIBLE);
                tvTotalQuizzes.setText(getString(R.string.saved_quizzes_format, 0));
            } else {
                tvTotalQuizzes.setText(getString(R.string.saved_quizzes_format, count));
                emptyState.setVisibility(View.GONE);

                for (Quiz quiz : quizList) {
                    addQuizCard(quiz);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addQuizCard(Quiz quiz) {
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.item_quiz, quizListContainer, false);
        
        TextView tvTitle = cardView.findViewById(R.id.tv_title);
        TextView tvDate = cardView.findViewById(R.id.tv_date);
        TextView tvCount = cardView.findViewById(R.id.tv_question_count);
        View btnDelete = cardView.findViewById(R.id.btn_delete);

        tvTitle.setText(quiz.getTitle());
        tvDate.setText(quiz.getDate());
        
        int countInt = 0;
        try {
            countInt = Integer.parseInt(quiz.getCount());
        } catch (NumberFormatException e) {
            // Fallback for safety
        }
        tvCount.setText(getString(R.string.questions_count_format, countInt));
        
        // Cập nhật Best Score thực tế
        View layoutBestScore = cardView.findViewById(R.id.layout_best_score);
        TextView tvBestScore = cardView.findViewById(R.id.tv_best_score);
        
        Double bestScore = AppDatabase.getInstance(getContext()).quizDao().getBestScoreForQuiz(quiz.getId());
        if (bestScore != null) {
            layoutBestScore.setVisibility(View.VISIBLE);
            tvBestScore.setText(String.format(Locale.getDefault(), "Best Score: %.0f%%", bestScore));
        } else {
            layoutBestScore.setVisibility(View.GONE);
        }

        android.widget.CheckBox cbSelect = cardView.findViewById(R.id.cb_select);
        if (isEditMode) {
            cbSelect.setVisibility(View.VISIBLE);
            cbSelect.setChecked(selectedQuizIds.contains(quiz.getId()));
            btnDelete.setVisibility(View.GONE);
        } else {
            cbSelect.setVisibility(View.GONE);
            btnDelete.setVisibility(View.VISIBLE);
        }

        cbSelect.setOnClickListener(v -> {
            if (cbSelect.isChecked()) selectedQuizIds.add(quiz.getId());
            else selectedQuizIds.remove(quiz.getId());
            updateSelectedCountText();
        });

        cardView.setOnClickListener(v -> {
            if (isEditMode) {
                cbSelect.performClick();
            } else {
                Intent intent = new Intent(getActivity(), QuizActivity.class);
                intent.putExtra("QUIZ_ID", quiz.getId());
                intent.putExtra("QUIZ_DATA", quiz.getJsonData());
                intent.putExtra("QUIZ_TITLE", quiz.getTitle());
                startActivity(intent);
            }
        });
        
        cardView.setOnLongClickListener(v -> {
            if (!isEditMode) showRenameDialog(quiz);
            return true;
        });

        btnDelete.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_quiz))
                .setMessage(getString(R.string.delete_quiz_msg))
                .setPositiveButton(getString(R.string.delete_confirm), (dialog, which) -> deleteQuiz(quiz))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
        });

        quizListContainer.addView(cardView);
    }

    private void showRenameDialog(Quiz quiz) {
        android.widget.EditText editText = new android.widget.EditText(requireContext());
        editText.setText(quiz.getTitle());
        editText.setSelection(quiz.getTitle().length());
        
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(requireContext());
        container.addView(editText);
        editText.setPadding(padding, padding / 2, padding, padding / 2);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Đổi tên bộ Quiz")
            .setView(container)
            .setPositiveButton("Lưu", (dialog, which) -> {
                String newName = editText.getText().toString().trim();
                if (!newName.isEmpty()) {
                    quiz.setTitle(newName);
                    renameQuiz(quiz);
                }
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void renameQuiz(Quiz quiz) {
        try {
            AppDatabase.getInstance(getContext()).quizDao().update(quiz);
            loadQuizzes(); // Refresh list
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteQuiz(Quiz quiz) {
        try {
            AppDatabase.getInstance(getContext()).quizDao().delete(quiz);
            loadQuizzes(); // Refresh list
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadQuizzes();
    }
}
