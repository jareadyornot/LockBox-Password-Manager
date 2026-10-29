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
            String savedVerifier = prefs.getString("master_pin_hash", "");
            String pinSaltHex = prefs.getString("pin_salt", "");
            String dbSaltHex = prefs.getString("db_salt", "");

            if (savedVerifier.isEmpty() || pinSaltHex.isEmpty() || dbSaltHex.isEmpty()) {
                Toast.makeText(this, "Security data missing. Clear app data and set up LockBox again.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            btnUnlock.setEnabled(false);

            new Thread(() -> {
                try {
                    String enteredVerifier = HashUtils.pbkdf2Hex(enteredPin, HashUtils.fromHex(pinSaltHex));

                    if (!HashUtils.constantTimeEquals(enteredVerifier, savedVerifier)) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                            etPin.setText("");
                            btnUnlock.setEnabled(true);
                        });
                        return;
                    }

                    String dbKey = HashUtils.pbkdf2Hex(enteredPin, HashUtils.fromHex(dbSaltHex));

                    LockBoxDatabase.destroyInstance();
                    LockBoxDatabase db = LockBoxDatabase.getInstance(this, dbKey);

                    // isOpen() does not actually open the database. This does, so a wrong key
                    // or corrupted file fails here instead of later on a random screen.
                    db.getOpenHelper().getWritableDatabase();

                    Session.start(enteredPin, dbKey);

                    runOnUiThread(() -> {
                        startActivity(new Intent(LoginActivity.this, CredentialsActivity.class));
                        finish();
                    });

                } catch (Exception e) {
                    LockBoxDatabase.destroyInstance();
                    Session.clear();
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Could not unlock database: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnUnlock.setEnabled(true);
                    });
                }
            }).start();
        });
    }
}