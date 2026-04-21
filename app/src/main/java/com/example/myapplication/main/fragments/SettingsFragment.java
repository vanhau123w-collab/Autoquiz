package com.example.myapplication.main.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.Button;
import android.graphics.Color;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.auth.LoginActivity;
import com.example.myapplication.data.AppDatabase;
import com.example.myapplication.data.User;

public class SettingsFragment extends Fragment {

    private TextView tvName, tvEmail, tvStreakCount;
    private TextView[] tvSettingsDays = new TextView[7];
    private Switch switchNotifications, switchReminder;
    private View btnLogout, btnEditProfile;
    private Button btnLangEn, btnLangVn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        tvName = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        switchNotifications = view.findViewById(R.id.switch_notifications);
        switchReminder = view.findViewById(R.id.switch_reminder);
        btnLangEn = view.findViewById(R.id.btn_lang_en);
        btnLangVn = view.findViewById(R.id.btn_lang_vn);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        tvStreakCount = view.findViewById(R.id.tv_streak_count_settings);

        // Initialize day indicators
        tvSettingsDays[0] = view.findViewById(R.id.tv_settings_day_1);
        tvSettingsDays[1] = view.findViewById(R.id.tv_settings_day_2);
        tvSettingsDays[2] = view.findViewById(R.id.tv_settings_day_3);
        tvSettingsDays[3] = view.findViewById(R.id.tv_settings_day_4);
        tvSettingsDays[4] = view.findViewById(R.id.tv_settings_day_5);
        tvSettingsDays[5] = view.findViewById(R.id.tv_settings_day_6);
        tvSettingsDays[6] = view.findViewById(R.id.tv_settings_day_7);

        loadUserData();
        setupListeners();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }

    private void loadUserData() {
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String email = sharedPref.getString("CurrentUserEmail", "");
        
        User user = AppDatabase.getInstance(getContext()).userDao().getUserByEmail(email);
        
        if (user != null) {
            tvName.setText(user.getName());
            tvEmail.setText(user.getEmail());
        } else {
            tvName.setText("Guest User");
            tvEmail.setText("guest@autoquiz.app");
        }

        // Load Streak Data
        try {
            com.example.myapplication.data.QuizDao quizDao = AppDatabase.getInstance(getContext()).quizDao();
            java.util.List<com.example.myapplication.data.QuizResult> results = quizDao.getAllResults(email);

            int streak = com.example.myapplication.utils.StreakUtils.calculateCurrentStreak(results);
            tvStreakCount.setText(String.valueOf(streak));

            boolean[] weeklyStatus = com.example.myapplication.utils.StreakUtils.getWeeklyStatus(results);
            for (int i = 0; i < 7; i++) {
                if (tvSettingsDays[i] != null) {
                    if (weeklyStatus[i]) {
                        tvSettingsDays[i].setBackgroundResource(R.drawable.bg_streak_circle);
                        tvSettingsDays[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                        tvSettingsDays[i].setText("✓");
                    } else {
                        tvSettingsDays[i].setBackgroundResource(0);
                        tvSettingsDays[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_secondary));
                        tvSettingsDays[i].setText("");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Load switch states
        switchNotifications.setChecked(sharedPref.getBoolean("NotifEnabled", true));
        switchReminder.setChecked(sharedPref.getBoolean("ReminderEnabled", true));

        // Update language button visual state
        updateLanguageButtons();
    }

    private void updateLanguageButtons() {
        String currentLang = com.example.myapplication.utils.LocaleHelper.getLanguage(requireContext());
        boolean isEn = "en".equals(currentLang);

        // Highlight EN button
        btnLangEn.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), 
                isEn ? R.color.primary : R.color.surface));
        btnLangEn.setTextColor(ContextCompat.getColor(requireContext(), 
                isEn ? R.color.white : R.color.on_surface));

        // Highlight VN button
        btnLangVn.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), 
                !isEn ? R.color.primary : R.color.surface));
        btnLangVn.setTextColor(ContextCompat.getColor(requireContext(), 
                !isEn ? R.color.white : R.color.on_surface));
    }

    private void setupListeners() {
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("NotifEnabled", isChecked).apply();
            Toast.makeText(getContext(), isChecked ? "Notifications enabled" : "Notifications disabled", Toast.LENGTH_SHORT).show();
        });

        switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("ReminderEnabled", isChecked).apply();
        });

        btnLangEn.setOnClickListener(v -> {
            editor.putBoolean("is_switching_language", true).apply();
            com.example.myapplication.utils.LocaleHelper.setLocale(requireContext(), "en");
            requireActivity().recreate();
        });

        btnLangVn.setOnClickListener(v -> {
            editor.putBoolean("is_switching_language", true).apply();
            com.example.myapplication.utils.LocaleHelper.setLocale(requireContext(), "vi");
            requireActivity().recreate();
        });

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), com.example.myapplication.main.EditProfileActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            // Clear current user session
            editor.remove("CurrentUserEmail");
            editor.apply();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
