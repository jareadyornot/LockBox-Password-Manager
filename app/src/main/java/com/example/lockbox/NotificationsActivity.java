package com.example.lockbox;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

public class NotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Credentials navigation
        FrameLayout navLock = findViewById(R.id.navLock);

        navLock.setOnClickListener(v -> {
            Intent intent = new Intent(
                    NotificationsActivity.this,
                    CredentialsActivity.class
            );
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        // Settings navigation
        FrameLayout navSettings = findViewById(R.id.navSettings);

        navSettings.setOnClickListener(v -> {
            Intent intent = new Intent(
                    NotificationsActivity.this,
                    SettingsActivity.class
            );
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });
    }
}