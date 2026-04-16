package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        TextView welcomeText = view.findViewById(R.id.home_welcome_text);
        Button btnCreate = view.findViewById(R.id.btn_create_quiz);
        Button btnLibrary = view.findViewById(R.id.btn_library);

        // Load user info
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String name = sharedPref.getString("UserName", "John Doe");
        welcomeText.setText("Hello, " + name + "! 👋");

        btnCreate.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToTab(1); // Create tab
            }
        });
        
        if (btnLibrary != null) {
            btnLibrary.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToTab(2); // Library tab
                }
            });
        }

        return view;
    }
}