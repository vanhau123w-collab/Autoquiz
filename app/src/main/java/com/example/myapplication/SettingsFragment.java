package com.example.myapplication;

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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.Button;
import android.graphics.Color;
import androidx.core.content.ContextCompat;

public class SettingsFragment extends Fragment {

    private TextView tvName, tvEmail;
    private Switch switchNotifications, switchReminder;
    private View btnLogout;
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

        loadUserData();
        setupListeners();

        return view;
    }

    private void loadUserData() {
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String name = sharedPref.getString("UserName", "Guest User");
        String email = sharedPref.getString("UserEmail", "guest@autoquiz.app");
        
        tvName.setText(name);
        tvEmail.setText(email);

        // Load switch states
        switchNotifications.setChecked(sharedPref.getBoolean("NotifEnabled", true));
        switchReminder.setChecked(sharedPref.getBoolean("ReminderEnabled", true));
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
            // Logic for English - visual feedback
            btnLangEn.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.primary));
            btnLangEn.setTextColor(Color.WHITE);
            btnLangVn.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.surface));
            btnLangVn.setTextColor(ContextCompat.getColor(getContext(), R.color.on_surface));
            Toast.makeText(getContext(), "Language set to English", Toast.LENGTH_SHORT).show();
        });

        btnLangVn.setOnClickListener(v -> {
            // Logic for Vietnamese - visual feedback
            btnLangVn.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.primary));
            btnLangVn.setTextColor(Color.WHITE);
            btnLangEn.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.surface));
            btnLangEn.setTextColor(ContextCompat.getColor(getContext(), R.color.on_surface));
            Toast.makeText(getContext(), "Đã chuyển sang tiếng Việt", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            // Clear session and go to Login
            editor.remove("UserName");
            editor.remove("UserEmail");
            editor.apply();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}