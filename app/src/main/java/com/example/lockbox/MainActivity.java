package com.example.lockbox;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lockbox.data.LockBoxDatabase;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("LockBoxPrefs", MODE_PRIVATE);
        String savedPinHash = prefs.getString("master_pin_hash", null);

        if (savedPinHash != null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        EditText etCreatePin = findViewById(R.id.etCreatePin);
        EditText etConfirmPin = findViewById(R.id.etConfirmPin);
        Button btnSetPin = findViewById(R.id.btnSetPin);

        btnSetPin.setOnClickListener(v -> {
            String createPin = etCreatePin.getText().toString().trim();
            String confirmPin = etConfirmPin.getText().toString().trim();

            if (createPin.isEmpty() || confirmPin.isEmpty()) {
                Toast.makeText(this, "Please enter a PIN", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!createPin.equals(confirmPin)) {
                Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (createPin.length() < 4) {
                Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show();
                return;
            }

            String pinHash = HashUtils.hashPin(createPin);
            prefs.edit().putString("master_pin_hash", pinHash).apply();

            // Initialize database with the raw PIN as the SQLCipher passphrase
            LockBoxDatabase.getInstance(this, createPin);

            Toast.makeText(this, "Master PIN set successfully", Toast.LENGTH_SHORT).show();

            startActivity(new Intent(MainActivity.this, CredentialsActivity.class));
            finish();
        });
    }
}