package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        animateDots();

        // Wait for 3 seconds then transition to Login
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }, 3000);
    }

    private void animateDots() {
        android.view.View dot1 = findViewById(R.id.dot1);
        android.view.View dot2 = findViewById(R.id.dot2);
        android.view.View dot3 = findViewById(R.id.dot3);

        startBounceAnimation(dot1, 0);
        startBounceAnimation(dot2, 200);
        startBounceAnimation(dot3, 400);
    }

    private void startBounceAnimation(android.view.View view, long delay) {
        view.setScaleX(0.5f);
        view.setScaleY(0.5f);
        view.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(delay)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(0.5f)
                            .scaleY(0.5f)
                            .alpha(0.5f)
                            .setDuration(600)
                            .withEndAction(() -> startBounceAnimation(view, 0))
                            .start();
                })
                .start();
    }
}
