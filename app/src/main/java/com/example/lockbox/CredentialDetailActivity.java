package com.example.lockbox;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lockbox.data.CredentialEntity;
import com.example.lockbox.data.LockBoxDatabase;

public class CredentialDetailActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credential_detail);

        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());

        TextView tvSite = findViewById(R.id.tvDetailSite);
        TextView tvUsername = findViewById(R.id.tvDetailUsername);
        TextView tvPassword = findViewById(R.id.tvDetailPassword);
        TextView tvNote = findViewById(R.id.tvDetailNote);

        ImageView btnCopyUsername = findViewById(R.id.btnCopyUsername);
        ImageView btnCopyPassword = findViewById(R.id.btnCopyPassword);
        ImageView btnTogglePassword = findViewById(R.id.btnTogglePassword);
        Button btnDelete = findViewById(R.id.btnDeleteCredential);
        Button btnEdit = findViewById(R.id.btnEditCredential);

        // Get data passed from adapter
        String site = getIntent().getStringExtra("site");
        String username = getIntent().getStringExtra("username");
        String password = getIntent().getStringExtra("password");
        String note = getIntent().getStringExtra("note");
        long credentialId = getIntent().getLongExtra("id", -1); // Fixed method name

        tvSite.setText(site);
        tvUsername.setText(username);
        if (note != null && !note.isEmpty()) {
            tvNote.setText(note);
            tvNote.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
        }

        // Masked password display
        String maskedPassword = "••••••••••••";
        tvPassword.setText(maskedPassword);

        // Toggle password visibility
        btnTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                tvPassword.setText(password);
                btnTogglePassword.setImageResource(android.R.drawable.ic_menu_view); // Fixed namespace reference
            } else {
                tvPassword.setText(maskedPassword);
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
            }
        });

        // Copy Username
        btnCopyUsername.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Username", username);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Username copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        // Copy Password
        btnCopyPassword.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Password", password);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Password copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        // Delete Credential
        btnDelete.setOnClickListener(v -> {
            new Thread(() -> {
                LockBoxDatabase db = LockBoxDatabase.getInstance(this, Session.getMasterPin());
                CredentialEntity entity = new CredentialEntity();
                entity.id = credentialId;
                db.credentialDao().deleteCredential(entity);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Credential deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }).start();
        });

        btnEdit.setOnClickListener(v ->
                Toast.makeText(this, "Edit feature coming up next!", Toast.LENGTH_SHORT).show()
        );
    }
}