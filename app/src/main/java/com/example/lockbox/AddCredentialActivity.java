package com.example.lockbox;

import android.content.Intent;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adds a new credential, or edits an existing one when launched with an "id" extra.
 */
public class AddCredentialActivity extends AppCompatActivity {

    private long editId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Session.isActive()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_add_credential);

        editId = getIntent().getLongExtra("id", -1);

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

        // Edit mode: load the existing record from the encrypted database
        if (editId != -1) {
            new Thread(() -> {
                try {
                    CredentialEntity existing = LockBoxDatabase
                            .getInstance(this, Session.getDbKey())
                            .credentialDao().getCredentialById(editId);
                    if (existing != null) {
                        runOnUiThread(() -> {
                            etSite.setText(existing.siteOrApp);
                            etUsername.setText(existing.username);
                            etPassword.setText(existing.encryptedPassword);
                            etNote.setText(existing.note);
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Could not load credential", Toast.LENGTH_SHORT).show());
                }
            }).start();
        }

        // Suggest strong password click listener
        btnSuggestPassword.setOnClickListener(v -> {
            String generatedPassword = generateSecurePassword(16);
            etPassword.setText(generatedPassword);
            etPassword.setSelection(generatedPassword.length()); // Move cursor to the end
            Toast.makeText(this, "Strong password generated", Toast.LENGTH_SHORT).show();
        });

        tvSave.setOnClickListener(v -> {
            String site = etSite.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String note = etNote.getText().toString().trim();

            if (site.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Site and Password are required", Toast.LENGTH_SHORT).show();
                return;
            }

            String dbKey = Session.getDbKey();
            if (dbKey == null || dbKey.isEmpty()) {
                Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            tvSave.setEnabled(false); // prevent double-tap saving duplicates

            new Thread(() -> {
                try {
                    CredentialEntity credential = new CredentialEntity();
                    credential.siteOrApp = site;
                    credential.username = username;
                    credential.encryptedPassword = password; // protected at rest by SQLCipher
                    credential.note = note;

                    LockBoxDatabase db = LockBoxDatabase.getInstance(this, dbKey);
                    if (editId != -1) {
                        credential.id = editId;
                        db.credentialDao().updateCredential(credential);
                    } else {
                        db.credentialDao().insertCredential(credential);
                    }

                    runOnUiThread(() -> {
                        Toast.makeText(this,
                                editId != -1 ? "Credential updated" : "Credential saved securely!",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        overridePendingTransition(0, R.anim.slide_out_bottom);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        tvSave.setEnabled(true);
                    });
                }
            }).start();
        });
    }

    // Guarantees at least one upper, lower, digit and symbol, then shuffles
    private String generateSecurePassword(int length) {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String symbols = "!@#$%^&*()_+-=[]{}|;:,.<>?";
        String all = upper + lower + digits + symbols;

        SecureRandom random = new SecureRandom();
        List<Character> chars = new ArrayList<>();
        chars.add(upper.charAt(random.nextInt(upper.length())));
        chars.add(lower.charAt(random.nextInt(lower.length())));
        chars.add(digits.charAt(random.nextInt(digits.length())));
        chars.add(symbols.charAt(random.nextInt(symbols.length())));
        while (chars.size() < length) {
            chars.add(all.charAt(random.nextInt(all.length())));
        }
        Collections.shuffle(chars, random);

        StringBuilder password = new StringBuilder();
        for (char c : chars) {
            password.append(c);
        }
        return password.toString();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.slide_out_bottom);
    }
}