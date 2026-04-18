package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button loginButton = findViewById(R.id.btn_login);
        TextView registerTextView = findViewById(R.id.tv_register);
        EditText emailEditText = findViewById(R.id.et_email);
        EditText passwordEditText = findViewById(R.id.et_password);

        loginButton.setOnClickListener(v -> {
            String emailInput = emailEditText.getText().toString().trim();
            String passwordInput = passwordEditText.getText().toString().trim();

            if (emailInput.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String savedEmail = sharedPref.getString("UserEmail", null);
            String savedPassword = sharedPref.getString("UserPassword", null);

            if (emailInput.equals(savedEmail) && passwordInput.equals(savedPassword)) {
                // Success
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                // Failure
                Toast.makeText(this, "Email hoặc mật khẩu không chính xác", Toast.LENGTH_SHORT).show();
            }
        });

        registerTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Password Visibility Toggle Logic
        passwordEditText.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2; // Right drawable index
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (passwordEditText.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {
                    if (event.getRawX() >= (passwordEditText.getRight() - passwordEditText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - passwordEditText.getPaddingEnd())) {
                        togglePasswordVisibility(passwordEditText);
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void togglePasswordVisibility(EditText passwordEditText) {
        if (isPasswordVisible) {
            // Hide Password
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordEditText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_outline, 0, R.drawable.ic_visibility_off, 0);
        } else {
            // Show Password
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            passwordEditText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_outline, 0, R.drawable.ic_visibility, 0);
        }
        isPasswordVisible = !isPasswordVisible;
        passwordEditText.setSelection(passwordEditText.getText().length()); // Move cursor to end
    }
}