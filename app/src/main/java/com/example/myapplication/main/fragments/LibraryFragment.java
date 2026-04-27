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
import com.example.myapplication.quiz.ReviewActivity;
import com.example.myapplication.utils.QuizExportManager;
import java.util.ArrayList;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.widget.Toast;
import android.widget.ProgressBar;
import androidx.core.content.ContextCompat;

public class LibraryFragment extends Fragment {

    private TextView tvTotalQuizzes;
    private View emptyState;
    private LinearLayout quizListContainer;
    
    // Selection state
    private boolean isEditMode = false;
    private java.util.HashSet<Integer> selectedQuizIds = new java.util.HashSet<>();
    private View layoutSelectionActions;
    private TextView tvSelectedCount;
    private android.widget.ImageButton btnMenu;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private com.google.android.material.tabs.TabLayout tabLayout;
    private int selectedTabIndex = 0;
    private android.widget.EditText etSearch;
    private String searchQuery = "";
    
    private Quiz quizToExport;
    
    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
        new ActivityResultContracts.CreateDocument("application/json"),
        uri -> {
            if (uri != null && quizToExport != null) {
                String json = QuizExportManager.exportToJSON(quizToExport);
                if (json != null) {
                    boolean success = QuizExportManager.writeToFile(requireContext(), uri, json);
                    if (success) Toast.makeText(getContext(), "Đã xuất bộ quiz thành công!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    );

    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
        new ActivityResultContracts.OpenDocument(),
        uri -> {
            if (uri != null) {
                String json = QuizExportManager.readFromFile(requireContext(), uri);
                if (json != null) {
                    android.content.SharedPreferences sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                    String email = sharedPref.getString("CurrentUserEmail", "");
                    Quiz importedQuiz = QuizExportManager.importFromJSON(json, email);
                    if (importedQuiz != null) {
                        String fileName = QuizExportManager.getFileNameFromUri(requireContext(), uri);
                        if (fileName != null && !fileName.isEmpty()) {
                            importedQuiz.setTitle(fileName);
                        }
                        new Thread(() -> {
                            int existing = AppDatabase.getInstance(getContext()).quizDao().getQuizCountByTitle(importedQuiz.getTitle(), email);
                            if (existing > 0) {
                                getActivity().runOnUiThread(() -> {
                                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                        .setTitle("Trùng tên Quiz")
                                        .setMessage("Bộ quiz \"" + importedQuiz.getTitle() + "\" đã tồn tại. Bạn muốn lưu với tên khác không?")
                                        .setPositiveButton("Lưu với tên mới", (d, w) -> {
                                            importedQuiz.setTitle(importedQuiz.getTitle() + " (" + (existing + 1) + ")");
                                            new Thread(() -> {
                                                AppDatabase.getInstance(getContext()).quizDao().insert(importedQuiz);
                                                getActivity().runOnUiThread(() -> {
                                                    loadQuizzes();
                                                    Toast.makeText(getContext(), "Đã nhập bộ quiz thành công!", Toast.LENGTH_SHORT).show();
                                                });
                                            }).start();
                                        })
                                        .setNegativeButton("Hủy", null)
                                        .show();
                                });
                            } else {
                                AppDatabase.getInstance(getContext()).quizDao().insert(importedQuiz);
                                getActivity().runOnUiThread(() -> {
                                    loadQuizzes();
                                    Toast.makeText(getContext(), "Đã nhập bộ quiz thành công!", Toast.LENGTH_SHORT).show();
                                });
                            }
                        }).start();
                    } else {
                        Toast.makeText(getContext(), "Tệp JSON không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        tvTotalQuizzes = view.findViewById(R.id.tv_quiz_count);
        emptyState = view.findViewById(R.id.empty_state);
        quizListContainer = view.findViewById(R.id.library_container);
        
        layoutSelectionActions = view.findViewById(R.id.layout_selection_actions);
        tvSelectedCount = view.findViewById(R.id.tv_selected_count);
        btnMenu = view.findViewById(R.id.btn_library_menu);

        btnMenu.setOnClickListener(v -> showLibraryPopupMenu(v));
        
        swipeRefreshLayout = view.findViewById(R.id.srl_library);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadQuizzes();
            swipeRefreshLayout.setRefreshing(false);
        });

        tabLayout = view.findViewById(R.id.tab_layout_library);
        tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                selectedTabIndex = tab.getPosition();
                filterAndDisplayQuizzes();
            }
            @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });

        view.findViewById(R.id.btn_cancel_selection).setOnClickListener(v -> toggleEditMode(false));
        view.findViewById(R.id.btn_delete_selected).setOnClickListener(v -> showDeleteSelectedConfirm());

        etSearch = view.findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase();
                filterAndDisplayQuizzes();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        loadQuizzes();
        return view;
    }

    private void toggleEditMode(boolean enable) {
        isEditMode = enable;
        selectedQuizIds.clear();
        layoutSelectionActions.setVisibility(enable ? View.VISIBLE : View.GONE);
        btnMenu.setVisibility(enable ? View.GONE : View.VISIBLE);
        updateSelectedCountText();
        loadQuizzes(); // Refresh UI to show checkboxes
    }

    private void showLibraryPopupMenu(View v) {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_quiz_options, null);
        sheet.setContentView(sheetView);

        ((TextView) sheetView.findViewById(R.id.tv_sheet_title)).setText("Tùy chọn thư viện");
        LinearLayout container = sheetView.findViewById(R.id.options_container);

        String[][] items = {
            {"📥", "Nhập Quiz từ file"},
            {"☑️", "Chọn nhiều"},
            {"🗑️", "Xóa tất cả"}
        };

        for (String[] item : items) {
            View row = LayoutInflater.from(getContext()).inflate(R.layout.item_sheet_option, container, false);
            ((TextView) row.findViewById(R.id.tv_option_icon)).setText(item[0]);
            ((TextView) row.findViewById(R.id.tv_option_text)).setText(item[1]);
            
            if (item[1].contains("Xóa")) {
                ((TextView) row.findViewById(R.id.tv_option_text)).setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.error));
            }

            row.setOnClickListener(rv -> {
                sheet.dismiss();
                if (item[1].contains("Nhập")) importLauncher.launch(new String[]{"application/json"});
                else if (item[1].contains("Chọn")) toggleEditMode(true);
                else if (item[1].contains("Xóa")) showDeleteAllConfirm();
            });
            container.addView(row);
        }
        sheet.show();
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

    private List<Quiz> cachedQuizzes = new ArrayList<>();

    private void loadQuizzes() {
        if (getContext() == null || quizListContainer == null) return;
        
        try {
            android.content.SharedPreferences sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String email = sharedPref.getString("CurrentUserEmail", "");
            
            cachedQuizzes = AppDatabase.getInstance(getContext()).quizDao().getAllQuizzes(email);
            filterAndDisplayQuizzes();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void filterAndDisplayQuizzes() {
        if (getContext() == null || quizListContainer == null) return;
        quizListContainer.removeAllViews();
        try {
            List<Quiz> filteredList = new ArrayList<>();
            for (Quiz q : cachedQuizzes) {
                boolean isCompleted = q.getCompletedResults() != null && !q.getCompletedResults().isEmpty() && !q.getCompletedResults().equals("[]");
                boolean matchesTab = (selectedTabIndex == 1) ? isCompleted : !isCompleted;
                boolean matchesSearch = searchQuery.isEmpty() || q.getTitle().toLowerCase().contains(searchQuery);
                if (matchesTab && matchesSearch) filteredList.add(q);
            }

            if (filteredList.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                quizListContainer.addView(emptyState);
                tvTotalQuizzes.setText("0 quizzes");
            } else {
                emptyState.setVisibility(View.GONE);
                tvTotalQuizzes.setText(filteredList.size() + " quizzes");
                for (Quiz quiz : filteredList) {
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
                showOptionsDialog(quiz);
            }
        });
        
        cardView.setOnLongClickListener(v -> {
            if (!isEditMode) {
                toggleEditMode(true);
                cbSelect.setChecked(true);
                selectedQuizIds.add(quiz.getId());
                updateSelectedCountText();
            }
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

    private void showOptionsDialog(Quiz quiz) {
        boolean hasResults = quiz.getCompletedResults() != null 
                && !quiz.getCompletedResults().isEmpty() 
                && !quiz.getCompletedResults().equals("[]");
        boolean hasAiEval = quiz.getAiEvaluation() != null && !quiz.getAiEvaluation().isEmpty();

        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
            new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_quiz_options, null);
        sheet.setContentView(sheetView);

        ((TextView) sheetView.findViewById(R.id.tv_sheet_title)).setText(quiz.getTitle());
        LinearLayout container = sheetView.findViewById(R.id.options_container);

        // Build options dynamically
        List<String[]> items = new ArrayList<>();
        items.add(new String[]{"📝", "Làm bài", "do_quiz"});
        if (hasResults) items.add(new String[]{"✨", "Xem lại bài làm gần nhất", "review"});
        if (hasAiEval) items.add(new String[]{"🤖", "Xem đánh giá AI", "view_ai"});
        if (hasResults && !hasAiEval) items.add(new String[]{"🤖", "Yêu cầu AI đánh giá", "request_ai"});
        items.add(new String[]{"✏️", "Đổi tên", "rename"});
        items.add(new String[]{"📤", "Chia sẻ (Xuất file JSON)", "export"});
        items.add(new String[]{"🗑️", "Xóa", "delete"});

        for (String[] item : items) {
            View row = LayoutInflater.from(getContext()).inflate(R.layout.item_sheet_option, container, false);
            ((TextView) row.findViewById(R.id.tv_option_icon)).setText(item[0]);
            ((TextView) row.findViewById(R.id.tv_option_text)).setText(item[1]);

            if (item[2].equals("delete")) {
                ((TextView) row.findViewById(R.id.tv_option_text)).setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.error));
            }

            row.setOnClickListener(rv -> {
                sheet.dismiss();
                switch (item[2]) {
                    case "do_quiz":
                        Intent intent = new Intent(getActivity(), QuizActivity.class);
                        intent.putExtra("QUIZ_ID", quiz.getId());
                        intent.putExtra("QUIZ_DATA", quiz.getJsonData());
                        intent.putExtra("QUIZ_TITLE", quiz.getTitle());
                        intent.putExtra("IS_RESUME", false);
                        startActivity(intent);
                        break;
                    case "review":
                        try {
                            org.json.JSONArray array = new org.json.JSONArray(quiz.getCompletedResults());
                            int[] answers = new int[array.length()];
                            for (int i = 0; i < array.length(); i++) answers[i] = array.getInt(i);
                            Intent reviewIntent = new Intent(getActivity(), ReviewActivity.class);
                            reviewIntent.putExtra("QUIZ_ID", quiz.getId());
                            reviewIntent.putExtra("QUIZ_DATA", quiz.getJsonData());
                            reviewIntent.putExtra("USER_ANSWERS", answers);
                            startActivity(reviewIntent);
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Không thể tải kết quả cũ", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "view_ai":
                        showAiEvaluationDialog(quiz);
                        break;
                    case "request_ai":
                        requestAiEvaluationForQuiz(quiz);
                        break;
                    case "rename":
                        showRenameDialog(quiz);
                        break;
                    case "export":
                        quizToExport = quiz;
                        exportLauncher.launch(quiz.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_") + ".json");
                        break;
                    case "delete":
                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.delete_quiz))
                            .setMessage(getString(R.string.delete_quiz_msg))
                            .setPositiveButton(getString(R.string.delete_confirm), (d, w) -> deleteQuiz(quiz))
                            .setNegativeButton(getString(R.string.cancel), null)
                            .show();
                        break;
                }
            });
            container.addView(row);
        }
        sheet.show();
    }

    private void requestAiEvaluationForQuiz(Quiz quiz) {
        try {
            org.json.JSONArray results = new org.json.JSONArray(quiz.getCompletedResults());
            int[] userAnswers = new int[results.length()];
            for (int i = 0; i < results.length(); i++) {
                userAnswers[i] = results.getInt(i);
            }
            com.example.myapplication.utils.AIEvaluationManager.evaluateQuiz(
                    getActivity(), quiz.getId(), quiz.getJsonData(), userAnswers, quiz.getTitle(),
                    (success, resultOrError) -> {
                        if (success) loadQuizzes();
                    }
                );
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Không thể phân tích kết quả cũ", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAiEvaluationDialog(Quiz quiz) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.layout_ai_explanation, null);
        bottomSheet.setContentView(sheetView);

        TextView tvTitle = sheetView.findViewById(R.id.tv_explain_title);
        TextView tvContent = sheetView.findViewById(R.id.tv_explain_content);
        ProgressBar progressBar = sheetView.findViewById(R.id.progress_bar_explain);

        tvTitle.setText("🤖 Đánh giá AI - " + quiz.getTitle());
        progressBar.setVisibility(View.GONE);

        String html = quiz.getAiEvaluation()
                .replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>")
                .replaceAll("\\n", "<br>");
        tvContent.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        tvContent.setText(android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_COMPACT));

        sheetView.findViewById(R.id.btn_close_sheet).setOnClickListener(v -> bottomSheet.dismiss());
        sheetView.findViewById(R.id.btn_got_it).setOnClickListener(v -> bottomSheet.dismiss());
        bottomSheet.show();
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
