package com.winlator;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_splash);
        } catch (Exception e) {
            // if splash layout fails, go straight to main
            goMain();
            return;
        }

        new Handler(Looper.getMainLooper()).postDelayed(this::goMain, 1200);
    }

    private void goMain() {
        try {
            Intent i = new Intent(SplashActivity.this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ignored) {
        }
        try {
            finish();
        } catch (Exception ignored) {
        }
    }
}
