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
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        TextView settingsName = view.findViewById(R.id.settingsName);
        TextView settingsEmail = view.findViewById(R.id.settingsEmail);
        MaterialSwitch darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        MaterialButton logoutButton = view.findViewById(R.id.logoutButton);

        // Load user info
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String name = sharedPref.getString("UserName", "Chưa có tên");
        String email = sharedPref.getString("UserEmail", "chua@co.email");
        settingsName.setText(name);
        settingsEmail.setText(email);

        // Đảm bảo Switch hiển thị đúng trạng thái Dark Mode
        boolean isDarkMode = false;
        try {
            isDarkMode = sharedPref.getBoolean("DarkMode", false);
        } catch (Exception e) {
            sharedPref.edit().remove("DarkMode").apply();
        }
        darkModeSwitch.setChecked(isDarkMode);
        
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("DarkMode", isChecked);
            editor.apply();

            // Áp dụng ngay lập tức
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // Logout Logic
        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }
}