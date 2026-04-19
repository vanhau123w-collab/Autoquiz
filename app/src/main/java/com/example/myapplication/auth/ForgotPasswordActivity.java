package com.example.myapplication.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.data.AppDatabase;
import com.example.myapplication.data.User;

public class ForgotPasswordActivity extends AppCompatActivity {
    
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.example.myapplication.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        EditText emailEditText = findViewById(R.id.et_forgot_email);
        EditText newPasswordEditText = findViewById(R.id.et_new_password);
        EditText confirmPasswordEditText = findViewById(R.id.et_confirm_password);
        Button resetBtn = findViewById(R.id.btn_reset_password);
        TextView backToLoginTv = findViewById(R.id.tv_back_to_login);

        resetBtn.setOnClickListener(v -> {
            String emailInput = emailEditText.getText().toString().trim();
            String newPassword = newPasswordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            if (emailInput.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
                return;
            }

            // Kiểm tra email trong Room Database
            User user = AppDatabase.getInstance(this).userDao().getUserByEmail(emailInput);

            if (user != null) {
                // Email khớp, chuyển sang xác thực OTP
                Intent intent = new Intent(ForgotPasswordActivity.this, OtpVerificationActivity.class);
                intent.putExtra(OtpVerificationActivity.EXTRA_ACTION, OtpVerificationActivity.ACTION_FORGOT_PASSWORD);
                intent.putExtra(OtpVerificationActivity.EXTRA_EMAIL, emailInput);
                intent.putExtra(OtpVerificationActivity.EXTRA_PASSWORD, newPassword); // Gửi password mới để lưu sau khi verify
                startActivity(intent);
            } else {
                // Email not found
                Toast.makeText(this, "Email không chính xác hoặc chưa được đăng ký", Toast.LENGTH_SHORT).show();
            }
        });

        backToLoginTv.setOnClickListener(v -> finish());
    }
}
