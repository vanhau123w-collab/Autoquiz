package com.example.myapplication.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.example.myapplication.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText nameEditText = findViewById(R.id.nameEditText);
        EditText emailEditText = findViewById(R.id.emailEditText);
        EditText passwordEditText = findViewById(R.id.passwordEditText);
        Button registerButton = findViewById(R.id.registerButton);
        TextView loginTextView = findViewById(R.id.loginTextView);

        registerButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            // Chuyển sang màn hình xác thực OTP
            Intent intent = new Intent(RegisterActivity.this, OtpVerificationActivity.class);
            intent.putExtra(OtpVerificationActivity.EXTRA_ACTION, OtpVerificationActivity.ACTION_REGISTER);
            intent.putExtra(OtpVerificationActivity.EXTRA_NAME, name);
            intent.putExtra(OtpVerificationActivity.EXTRA_EMAIL, email);
            intent.putExtra(OtpVerificationActivity.EXTRA_PASSWORD, password);
            startActivity(intent);
        });

        loginTextView.setOnClickListener(v -> finish());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }
}
