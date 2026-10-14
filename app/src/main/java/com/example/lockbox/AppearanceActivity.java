package com.example.lockbox;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AppearanceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appearance);

        // Back button
        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());

        // Theme selection pills
        Button btnSystem = findViewById(R.id.btnThemeSystem);
        Button btnLight = findViewById(R.id.btnThemeLight);
        Button btnDark = findViewById(R.id.btnThemeDark);

        btnSystem.setOnClickListener(v -> {
            btnSystem.setText("✓ System");
            btnSystem.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E3440")));
            btnSystem.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));

            btnLight.setText("Light");
            btnLight.setBackgroundTintList(null);
            btnLight.setTextColor(android.graphics.Color.parseColor("#AAAAAA"));

            btnDark.setText("Dark");
            btnDark.setBackgroundTintList(null);
            btnDark.setTextColor(android.graphics.Color.parseColor("#AAAAAA"));
        });

        btnLight.setOnClickListener(v -> {
            btnLight.setText("✓ Light");
            btnLight.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E3440")));
            btnLight.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));

            btnSystem.setText("System");
            btnSystem.setBackgroundTintList(null);
            btnSystem.setTextColor(android.graphics.Color.parseColor("#AAAAAA"));

            btnDark.setText("Dark");
            btnDark.setBackgroundTintList(null);
            btnDark.setTextColor(android.graphics.Color.parseColor("#AAAAAA"));
        });

        btnDark.setOnClickListener(v -> {
            btnDark.setText("✓ Dark");
            btnDark.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E3440")));
            btnDark.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));

            btnSystem.setText("System");
            btnSystem.setBackgroundTintList(null);
            btnSystem.setTextColor(android.graphics.Color.parseColor("#AAAAAA"));

            btnLight.setText("Light");
            btnLight.setBackgroundTintList(null);
            btnLight.setTextColor(android.graphics.Color.parseColor("#AAAAAA"));
        });

        // Bottom Navigation Tab Switching
        findViewById(R.id.navLock).setOnClickListener(v -> {
            startActivity(new Intent(AppearanceActivity.this, CredentialsActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        findViewById(R.id.navNotifications).setOnClickListener(v -> {
            startActivity(new Intent(AppearanceActivity.this, NotificationsActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        findViewById(R.id.navSettings).setOnClickListener(v -> {
            startActivity(new Intent(AppearanceActivity.this, SettingsActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }
}