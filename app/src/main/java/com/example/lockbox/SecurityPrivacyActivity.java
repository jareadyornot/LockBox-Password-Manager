package com.example.lockbox;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SecurityPrivacyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_privacy);

        // Back button
        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());

        // Bottom Navigation Tab Switching
        findViewById(R.id.navLock).setOnClickListener(v -> {
            startActivity(new Intent(SecurityPrivacyActivity.this, CredentialsActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        findViewById(R.id.navNotifications).setOnClickListener(v -> {
            startActivity(new Intent(SecurityPrivacyActivity.this, NotificationsActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        findViewById(R.id.navSettings).setOnClickListener(v -> {
            startActivity(new Intent(SecurityPrivacyActivity.this, SettingsActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }
}