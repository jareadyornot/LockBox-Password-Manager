package com.example.lockbox;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Credentials
        FrameLayout navLock = findViewById(R.id.navLock);

        navLock.setOnClickListener(v -> {
            Intent intent = new Intent(
                    SettingsActivity.this,
                    CredentialsActivity.class
            );
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        // Notifications
        FrameLayout navNotifications = findViewById(R.id.navNotifications);

        navNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(
                    SettingsActivity.this,
                    NotificationsActivity.class
            );
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        // Appearance row navigation
        findViewById(R.id.rowAppearance).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, AppearanceActivity.class);
            startActivity(intent);
        });

        // Data and storage row navigation
        findViewById(R.id.rowDataStorage).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, DataStorageActivity.class);
            startActivity(intent);
        });

        // Security and privacy row navigation
        findViewById(R.id.rowSecurityPrivacy).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, SecurityPrivacyActivity.class);
            startActivity(intent);
        });

    }
}