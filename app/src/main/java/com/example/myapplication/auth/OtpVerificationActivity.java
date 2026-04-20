package com.example.myapplication.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.data.AppDatabase;
import com.example.myapplication.data.User;
import com.example.myapplication.utils.EmailSender;

import java.util.Random;

public class OtpVerificationActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.example.myapplication.utils.LocaleHelper.onAttach(newBase));
    }

    public static final String EXTRA_ACTION = "extra_action";
    public static final String ACTION_REGISTER = "register";
    public static final String ACTION_FORGOT_PASSWORD = "forgot_password";

    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_EMAIL = "extra_email";
    public static final String EXTRA_PASSWORD = "extra_password";

    private EditText[] otpEdits;
    private String currentOtp;
    private String userEmail, userName, userPassword, action;
    private TextView tvCountdown, tvResend, tvInstruction;
    private CountDownTimer countDownTimer;
    private EmailSender emailSender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        emailSender = new EmailSender();
        
        // Get data from intent
        action = getIntent().getStringExtra(EXTRA_ACTION);
        userEmail = getIntent().getStringExtra(EXTRA_EMAIL);
        userName = getIntent().getStringExtra(EXTRA_NAME);
        userPassword = getIntent().getStringExtra(EXTRA_PASSWORD);

        tvInstruction = findViewById(R.id.tv_otp_instruction);
        tvInstruction.setText("Please enter the 6-digit code sent to\n" + userEmail);

        otpEdits = new EditText[]{
                findViewById(R.id.otp_edit_1), findViewById(R.id.otp_edit_2),
                findViewById(R.id.otp_edit_3), findViewById(R.id.otp_edit_4),
                findViewById(R.id.otp_edit_5), findViewById(R.id.otp_edit_6)
        };

        setupOtpInputs();

        tvCountdown = findViewById(R.id.tv_countdown);
        tvResend = findViewById(R.id.tv_resend_otp);
        Button btnVerify = findViewById(R.id.btn_verify_otp);

        startCountdown();
        sendOtpToEmail();

        btnVerify.setOnClickListener(v -> verifyOtp());
        tvResend.setOnClickListener(v -> {
            startCountdown();
            sendOtpToEmail();
            tvResend.setVisibility(View.GONE);
            tvCountdown.setVisibility(View.VISIBLE);
        });
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void setupOtpInputs() {
        for (int i = 0; i < 6; i++) {
            final int index = i;
            otpEdits[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < 5) {
                        otpEdits[index + 1].requestFocus();
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            otpEdits[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (otpEdits[index].getText().toString().isEmpty() && index > 0) {
                        otpEdits[index - 1].requestFocus();
                        otpEdits[index - 1].setText("");
                    }
                }
                return false;
            });
        }
    }

    private void sendOtpToEmail() {
        currentOtp = String.format("%06d", new Random().nextInt(1000000));
        Toast.makeText(this, "Đang gửi mã OTP...", Toast.LENGTH_SHORT).show();
        
        emailSender.sendOtpEmail(userEmail, currentOtp, new EmailSender.EmailCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(OtpVerificationActivity.this, "Mã đã được gửi đến email!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(OtpVerificationActivity.this, "Gửi mail thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startCountdown() {
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvCountdown.setText("Resend code in " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                tvCountdown.setVisibility(View.GONE);
                tvResend.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    private void verifyOtp() {
        StringBuilder enteredOtp = new StringBuilder();
        for (EditText et : otpEdits) enteredOtp.append(et.getText().toString());

        if (enteredOtp.length() < 6) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ 6 số", Toast.LENGTH_SHORT).show();
            return;
        }

        if (enteredOtp.toString().equals(currentOtp)) {
            handleSuccess();
        } else {
            Toast.makeText(this, "Mã OTP không chính xác", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSuccess() {
        Toast.makeText(this, "Xác thực thành công!", Toast.LENGTH_SHORT).show();
        
        if (ACTION_REGISTER.equals(action)) {
            // Lưu vào Room Database
            User newUser = new User(userName, userEmail, userPassword);
            AppDatabase.getInstance(this).userDao().insert(newUser);

            Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        } else if (ACTION_FORGOT_PASSWORD.equals(action)) {
            // Cập nhật mật khẩu mới trong Room
            AppDatabase.getInstance(this).userDao().updatePassword(userEmail, userPassword);

            Toast.makeText(this, "Cập nhật mật khẩu thành công!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
