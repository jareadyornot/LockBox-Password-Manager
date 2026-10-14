package com.example.lockbox;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lockbox.data.LockBoxDatabase;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText etPin = findViewById(R.id.etPin);
        Button btnUnlock = findViewById(R.id.btnUnlock);

        btnUnlock.setOnClickListener(v -> {
            String enteredPin = etPin.getText().toString().trim();

            if (enteredPin.isEmpty()) {
                Toast.makeText(this, "Enter your master PIN", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences("LockBoxPrefs", MODE_PRIVATE);
            String savedPinHash = prefs.getString("master_pin_hash", "");

            String enteredPinHash = HashUtils.hashPin(enteredPin);

            if (enteredPinHash.equals(savedPinHash)) {
                new Thread(() -> {
                    try {
                        LockBoxDatabase.destroyInstance();

                        // Initialize database with the hashed PIN
                        LockBoxDatabase db = LockBoxDatabase.getInstance(this, enteredPinHash);

                        // Force a safe open check instead of querying missing tables
                        db.isOpen();

                        // Save session
                        Session.setMasterPin(enteredPinHash);

                        runOnUiThread(() -> {
                            startActivity(new Intent(LoginActivity.this, CredentialsActivity.class));
                            finish();
                        });

                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Decryption error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                }).start();

            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                etPin.setText("");
            }
        });
    }
}