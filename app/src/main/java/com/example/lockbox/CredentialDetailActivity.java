package com.example.lockbox;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lockbox.data.CredentialEntity;
import com.example.lockbox.data.LockBoxDatabase;

public class CredentialDetailActivity extends AppCompatActivity {

    private static final String MASKED = "••••••••••••";

    private boolean isPasswordVisible = false;
    private long credentialId = -1;
    private String username = "";
    private String password = "";

    private TextView tvSite, tvUsername, tvPassword, tvNote;
    private ImageView btnTogglePassword;
    private CharSequence defaultNoteText;
    private int defaultNoteColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credential_detail);

        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());

        tvSite = findViewById(R.id.tvDetailSite);
        tvUsername = findViewById(R.id.tvDetailUsername);
        tvPassword = findViewById(R.id.tvDetailPassword);
        tvNote = findViewById(R.id.tvDetailNote);
        defaultNoteText = tvNote.getText();
        defaultNoteColor = tvNote.getCurrentTextColor();

        ImageView btnCopyUsername = findViewById(R.id.btnCopyUsername);
        ImageView btnCopyPassword = findViewById(R.id.btnCopyPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        Button btnDelete = findViewById(R.id.btnDeleteCredential);
        Button btnEdit = findViewById(R.id.btnEditCredential);

        // Only the id is passed in; the data is read from the encrypted database
        credentialId = getIntent().getLongExtra("id", -1);

        tvPassword.setText(MASKED);

        btnTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                tvPassword.setText(password);
                btnTogglePassword.setImageResource(android.R.drawable.ic_menu_view);
            } else {
                tvPassword.setText(MASKED);
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
            }
        });

        btnCopyUsername.setOnClickListener(v -> {
            copyToClipboard("Username", username, false);
            Toast.makeText(this, "Username copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        btnCopyPassword.setOnClickListener(v -> {
            copyToClipboard("Password", password, true);
            Toast.makeText(this, "Password copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        btnDelete.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Delete credential?")
                        .setMessage("This cannot be undone.")
                        .setPositiveButton("Delete", (d, w) -> deleteCredential())
                        .setNegativeButton("Cancel", null)
                        .show()
        );

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddCredentialActivity.class);
            intent.putExtra("id", credentialId);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_bottom, 0);
        });
    }

    // Reload on every resume so edits made in AddCredentialActivity show up immediately
    @Override
    protected void onResume() {
        super.onResume();

        if (!Session.isActive()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        loadCredential();
    }

    private void loadCredential() {
        new Thread(() -> {
            try {
                CredentialEntity c = LockBoxDatabase.getInstance(this, Session.getDbKey())
                        .credentialDao().getCredentialById(credentialId);
                runOnUiThread(() -> {
                    if (c == null) {
                        finish();
                        return;
                    }
                    bind(c);
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Could not load credential", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void bind(CredentialEntity c) {
        username = c.username == null ? "" : c.username;
        password = c.encryptedPassword == null ? "" : c.encryptedPassword;

        tvSite.setText(c.siteOrApp);
        tvUsername.setText(username);
        tvPassword.setText(isPasswordVisible ? password : MASKED);

        if (c.note != null && !c.note.isEmpty()) {
            tvNote.setText(c.note);
            tvNote.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
        } else {
            tvNote.setText(defaultNoteText);
            tvNote.setTextColor(defaultNoteColor);
        }
    }

    private void deleteCredential() {
        new Thread(() -> {
            try {
                LockBoxDatabase db = LockBoxDatabase.getInstance(this, Session.getDbKey());
                CredentialEntity entity = new CredentialEntity();
                entity.id = credentialId;
                db.credentialDao().deleteCredential(entity);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Credential deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void copyToClipboard(String label, String text, boolean sensitive) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        if (sensitive) {
            // Asks Android to hide the copied value from clipboard previews
            PersistableBundle extras = new PersistableBundle();
            extras.putBoolean("android.content.extra.IS_SENSITIVE", true);
            clip.getDescription().setExtras(extras);
        }
        clipboard.setPrimaryClip(clip);
    }
}