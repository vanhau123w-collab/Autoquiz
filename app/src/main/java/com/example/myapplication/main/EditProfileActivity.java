package com.example.myapplication.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.data.AppDatabase;
import com.example.myapplication.data.User;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Button btnSave;
    private User currentUser;
    private String originalEmail;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(com.example.myapplication.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etName = findViewById(R.id.et_edit_name);
        etEmail = findViewById(R.id.et_edit_email);
        etPassword = findViewById(R.id.et_edit_password);
        btnSave = findViewById(R.id.btn_save_profile);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        loadUserData();

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadUserData() {
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        originalEmail = sharedPref.getString("CurrentUserEmail", "");

        currentUser = AppDatabase.getInstance(this).userDao().getUserByEmail(originalEmail);

        if (currentUser != null) {
            etName.setText(currentUser.getName());
            etEmail.setText(currentUser.getEmail());
            etPassword.setText(currentUser.getPassword());
        }
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser != null) {
            currentUser.setName(name);
            currentUser.setEmail(email);
            currentUser.setPassword(password);

            try {
                AppDatabase.getInstance(this).userDao().update(currentUser);
                
                // Update SharedPreferences in case email changed
                SharedPreferences sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                sharedPref.edit().putString("CurrentUserEmail", email).commit();

                Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "Email này đã được sử dụng", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
