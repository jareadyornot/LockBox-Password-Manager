package com.example.lockbox;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lockbox.data.CredentialEntity;
import com.example.lockbox.data.LockBoxDatabase;
import com.google.android.material.button.MaterialButton;

import java.security.SecureRandom;

public class AddCredentialActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_credential);

        ImageView ivClose = findViewById(R.id.ivClose);
        ivClose.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.slide_out_bottom);
        });

        EditText etSite = findViewById(R.id.etSite);
        EditText etUsername = findViewById(R.id.etUsername);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etNote = findViewById(R.id.etNote);
        TextView tvSave = findViewById(R.id.tvSave);
        MaterialButton btnSuggestPassword = findViewById(R.id.btnSuggestPassword);

        // Suggest strong password click listener
        btnSuggestPassword.setOnClickListener(v -> {
            String generatedPassword = generateSecurePassword(16);
            etPassword.setText(generatedPassword);
            etPassword.setSelection(generatedPassword.length()); // Move cursor to the end
            Toast.makeText(this, "Strong password generated", Toast.LENGTH_SHORT).show();
        });

        // Inside AddCredentialActivity.java save click listener:
        tvSave.setOnClickListener(v -> {
            String site = etSite.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String note = etNote.getText().toString().trim();

            if (site.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Site and Password are required", Toast.LENGTH_SHORT).show();
                return;
            }

            String currentPin = Session.getMasterPin();
            if (currentPin == null || currentPin.isEmpty()) {
                Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            new Thread(() -> {
                try {
                    CredentialEntity credential = new CredentialEntity();
                    credential.siteOrApp = site;
                    credential.username = username;
                    credential.encryptedPassword = password;
                    credential.note = note;

                    LockBoxDatabase db = LockBoxDatabase.getInstance(this, currentPin);
                    db.credentialDao().insertCredential(credential);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Credential saved securely!", Toast.LENGTH_SHORT).show();
                        finish();
                        overridePendingTransition(0, R.anim.slide_out_bottom);
                    });
                } catch (Exception e) {
                    // Destroy the cached instance so a failed write doesn't lock up the app
                    LockBoxDatabase.destroyInstance();

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        });
    }

    private String generateSecurePassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|;:,.<>?";
        StringBuilder password = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            password.append(chars.charAt(index));
        }
        return password.toString();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.slide_out_bottom);
    }
}