package com.example.myapplication.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.myapplication.R;
import com.example.myapplication.utils.AIModelManager;
import java.util.ArrayList;
import java.util.List;

public class AISettingsActivity extends AppCompatActivity {

    private AIModelManager manager;
    private Spinner spinnerProvider, spinnerModel;
    private LinearLayout llKeysContainer;
    private TextView tvGetKeyLink;
    private String currentProvider;
    private boolean isInitializing = false;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.example.myapplication.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_settings);

        manager         = new AIModelManager(this);
        spinnerProvider = findViewById(R.id.spinner_provider);
        spinnerModel    = findViewById(R.id.spinner_model);
        llKeysContainer = findViewById(R.id.ll_keys_container);
        tvGetKeyLink    = findViewById(R.id.tv_get_key_link);

        // Provider spinner
        String[] providers = AIModelManager.allProviders();
        ArrayAdapter<String> providerAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, providers);
        providerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerProvider.setAdapter(providerAdapter);

        currentProvider = manager.getSelectedProvider();
        for (int i = 0; i < providers.length; i++) {
            if (providers[i].equals(currentProvider)) { spinnerProvider.setSelection(i); break; }
        }

        spinnerProvider.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (isInitializing) return;
                currentProvider = providers[pos];
                manager.setSelectedProvider(currentProvider); // auto-save
                refreshModelSpinner(currentProvider);
                refreshKeys(currentProvider);
                tvGetKeyLink.setTag(AIModelManager.getKeyUrl(currentProvider));
                showSavedHint();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (isInitializing) return;
                String selected = (String) spinnerModel.getSelectedItem();
                if (selected != null) {
                    manager.setSelectedModel(selected); // auto-save
                    showSavedHint();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        tvGetKeyLink.setOnClickListener(v -> {
            String url = (String) tvGetKeyLink.getTag();
            if (url != null) startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });

        findViewById(R.id.btn_add_key).setOnClickListener(v -> addKeyRow("", true, true));
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Initial load
        isInitializing = true;
        refreshModelSpinner(currentProvider);
        refreshKeys(currentProvider);
        tvGetKeyLink.setTag(AIModelManager.getKeyUrl(currentProvider));
        isInitializing = false;
    }

    private void refreshModelSpinner(String provider) {
        String[] models = AIModelManager.modelsForProvider(provider);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, models);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerModel.setAdapter(adapter);

        String saved = manager.getSelectedModel();
        for (int i = 0; i < models.length; i++) {
            if (models[i].equals(saved)) { spinnerModel.setSelection(i); return; }
        }
        spinnerModel.setSelection(0);
    }

    private void refreshKeys(String provider) {
        llKeysContainer.removeAllViews();
        // Chỉ Gemini mới cần nhiều key xoay tua
        boolean isGemini = AIModelManager.PROVIDER_GEMINI.equals(provider);
        findViewById(R.id.btn_add_key).setVisibility(isGemini ? View.VISIBLE : View.GONE);

        List<String> keys = manager.getKeys(provider);
        if (keys.isEmpty()) addKeyRow("", false, isGemini);
        else for (String k : keys) addKeyRow(k, false, isGemini);
    }

    private void addKeyRow(String value, boolean focused, boolean showDeleteBtn) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(56));
        cardParams.setMargins(0, 0, 0, dpToPx(8));
        card.setLayoutParams(cardParams);
        card.setRadius(dpToPx(12));
        card.setCardElevation(dpToPx(2));
        card.setCardBackgroundColor(getResources().getColor(R.color.surface, getTheme()));

        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int ph = dpToPx(16);
        row.setPadding(ph, 0, ph, 0);

        EditText et = new EditText(this);
        et.setId(View.generateViewId());
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        et.setLayoutParams(etParams);
        et.setBackground(null);
        et.setHint("API Key");
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        et.setText(value);
        et.setTextSize(13f);

        // Lưu reference EditText vào tag của card để tìm lại dễ dàng
        card.setTag(et);

        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isInitializing) saveKeysNow(); // auto-save, không toast
            }
        });

        // Toggle visibility button
        ImageButton btnToggle = new ImageButton(this);
        btnToggle.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)));
        btnToggle.setBackground(null);
        btnToggle.setImageResource(android.R.drawable.ic_menu_view);
        btnToggle.setOnClickListener(v -> {
            int cur = et.getInputType();
            // Check if currently visible (InputType for password visibility includes 0x90)
            boolean isVisible = (cur == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD));
            
            isInitializing = true;
            if (isVisible) {
                // Hide password
                et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnToggle.setImageResource(android.R.drawable.ic_menu_view);
            } else {
                // Show password
                et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnToggle.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            }
            et.setSelection(et.getText().length());
            
            // Re-enable after a short delay
            et.postDelayed(() -> isInitializing = false, 100);
        });

        // Clear key button (x)
        ImageButton btnClear = new ImageButton(this);
        btnClear.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)));
        btnClear.setBackground(null);
        btnClear.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnClear.setColorFilter(0xFFE53935, android.graphics.PorterDuff.Mode.SRC_IN);
        btnClear.setOnClickListener(v -> {
            et.setText("");
            saveKeysNow();
        });

        row.addView(et);
        row.addView(btnToggle);
        row.addView(btnClear);

        // Nút xóa chỉ hiện với Gemini (multi-key)
        if (showDeleteBtn) {
            ImageButton btnDelete = new ImageButton(this);
            btnDelete.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)));
            btnDelete.setBackground(null);
            btnDelete.setImageResource(android.R.drawable.ic_delete);
            btnDelete.setOnClickListener(v -> {
                if (llKeysContainer.getChildCount() > 1) {
                    llKeysContainer.removeView(card);
                    saveKeysNow();
                }
            });
            row.addView(btnDelete);
        }

        card.addView(row);
        llKeysContainer.addView(card);
        if (focused) et.requestFocus();
    }

    private void saveKeysNow() {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < llKeysContainer.getChildCount(); i++) {
            View child = llKeysContainer.getChildAt(i);
            // Tag của card chính là EditText reference
            if (child.getTag() instanceof EditText) {
                String k = ((EditText) child.getTag()).getText().toString().trim();
                if (!k.isEmpty()) keys.add(k);
            }
        }
        manager.setKeys(currentProvider, keys);
    }

    private void showSavedHint() {
        Toast.makeText(this, "✓ Đã lưu", Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
