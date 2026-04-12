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
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;

public class HomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        TextView welcomeText = view.findViewById(R.id.home_welcome_text);
        MaterialButton btnCreate = view.findViewById(R.id.btn_continue_learning);

        // Load user info
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String name = sharedPref.getString("UserName", "Bạn");
        welcomeText.setText("Chào mừng trở lại, " + name + "!");

        // Open Create Quiz Activity
        btnCreate.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreateQuizActivity.class);
            startActivity(intent);
        });

        return view;
    }
}