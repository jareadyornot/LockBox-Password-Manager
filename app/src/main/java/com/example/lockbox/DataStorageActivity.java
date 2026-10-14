package com.example.lockbox;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DataStorageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_storage);

        // Back button
        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());

        // Backup action triggers
        findViewById(R.id.btnCreateBackup).setOnClickListener(v ->
                Toast.makeText(this, "Backup created successfully", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.btnRestoreBackup).setOnClickListener(v ->
                Toast.makeText(this, "Select backup file to restore", Toast.LENGTH_SHORT).show()
        );

        // Reset data trigger
        findViewById(R.id.rowResetData).setOnClickListener(v ->
                Toast.makeText(this, "Reset option selected", Toast.LENGTH_SHORT).show()
        );

        // Bottom Navigation Tab Switching
        findViewById(R.id.navLock).setOnClickListener(v -> {
            startActivity(new Intent(DataStorageActivity.this, CredentialsActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        findViewById(R.id.navNotifications).setOnClickListener(v -> {
            startActivity(new Intent(DataStorageActivity.this, NotificationsActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        findViewById(R.id.navSettings).setOnClickListener(v -> {
            startActivity(new Intent(DataStorageActivity.this, SettingsActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }
}