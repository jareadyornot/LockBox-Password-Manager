package com.example.lockbox;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lockbox.data.LockBoxDatabase;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    private LinearLayout rowDataStorage;
    private LinearLayout rowResetData; // Add this if you have a reset row in your XML

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        rowDataStorage = findViewById(R.id.rowDataStorage);

        // Optional: If you add a reset row to activity_settings.xml with ID rowResetData
        // rowResetData = findViewById(R.id.rowResetData);
        // if (rowResetData != null) {
        //     rowResetData.setOnClickListener(v -> showResetConfirmationDialog());
        // }

        // Open Data and Storage sub-screen
        rowDataStorage.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, DataStorageActivity.class));
            overridePendingTransition(R.anim.slide_in_bottom, 0);
        });

        // Bottom Navigation Listeners
        findViewById(R.id.navLock).setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, CredentialsActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        findViewById(R.id.navNotifications).setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, NotificationsActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }

    private void showResetConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reset App Data")
                .setMessage("This will permanently delete your local database and Master PIN, returning the app to its initial setup state. Your external backup files will not be affected. Are you sure?")
                .setPositiveButton("Reset App", (dialog, which) -> performAppDataReset())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performAppDataReset() {
        new Thread(() -> {
            try {
                // 1. Close and destroy the database instance
                LockBoxDatabase.destroyInstance();

                // 2. Delete the local SQLite database file and WAL/SHM temp files
                File dbFile = getDatabasePath("lockbox_secure.db");
                if (dbFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    dbFile.delete();
                }
                File walFile = new File(dbFile.getPath() + "-wal");
                if (walFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    walFile.delete();
                }
                File shmFile = new File(dbFile.getPath() + "-shm");
                if (shmFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    shmFile.delete();
                }

                // 3. Clear SharedPreferences (preserve backup folder URI preference if desired)
                SharedPreferences prefs = getSharedPreferences("LockBoxPrefs", MODE_PRIVATE);
                String savedBackupUri = prefs.getString("backup_folder_uri", "");
                prefs.edit()
                        .clear()
                        .putString("backup_folder_uri", savedBackupUri)
                        .apply();

                // 4. Clear active memory session
                Session.clear();

                // 5. Restart app back to MainActivity (Setup / Restore screen)
                runOnUiThread(() -> {
                    Toast.makeText(this, "App data reset successfully.", Toast.LENGTH_LONG).show();
                    Intent i = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    if (i != null) {
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                    }
                    finishAffinity();

                    new Handler(Looper.getMainLooper()).postDelayed(
                            () -> android.os.Process.killProcess(android.os.Process.myPid()), 500);
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Reset failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}