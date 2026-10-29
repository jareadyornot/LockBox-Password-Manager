package com.example.lockbox;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lockbox.data.LockBoxDatabase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> restoreFileLauncher;

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
        Button btnRestoreBackup = findViewById(R.id.btnRestoreBackup);

        // Initialize file picker for restoring backups during setup
        restoreFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            showPinPromptForRestore(uri);
                        }
                    }
                }
        );

        btnRestoreBackup.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            restoreFileLauncher.launch(intent);
        });

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

            btnSetPin.setEnabled(false);

            // Key derivation is deliberately slow, so keep it off the UI thread
            new Thread(() -> {
                try {
                    byte[] pinSalt = HashUtils.generateSalt();
                    byte[] dbSalt = HashUtils.generateSalt();

                    // verifier: stored, only used to check the PIN at login
                    String verifier = HashUtils.pbkdf2Hex(createPin, pinSalt);
                    // database key: never stored, derived again at every login
                    String dbKey = HashUtils.pbkdf2Hex(createPin, dbSalt);

                    // Open (and therefore create) the encrypted database now
                    LockBoxDatabase.destroyInstance();
                    LockBoxDatabase db = LockBoxDatabase.getInstance(this, dbKey);
                    db.getOpenHelper().getWritableDatabase();

                    prefs.edit()
                            .putString("pin_salt", HashUtils.toHex(pinSalt))
                            .putString("db_salt", HashUtils.toHex(dbSalt))
                            .putString("master_pin_hash", verifier)
                            .apply();

                    Session.start(createPin, dbKey);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Master PIN set successfully", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, CredentialsActivity.class));
                        finish();
                    });
                } catch (Exception e) {
                    LockBoxDatabase.destroyInstance();
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Setup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnSetPin.setEnabled(true);
                    });
                }
            }).start();
        });
    }

    private void showPinPromptForRestore(Uri backupUri) {
        final EditText inputPin = new EditText(this);
        inputPin.setHint("Enter backup Master PIN");
        inputPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        // Add proper padding directly to the EditText view instead of the builder
        int padding = 50;
        inputPin.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Restore Backup")
                .setMessage("Enter the Master PIN that was used when this backup was created.")
                .setView(inputPin)
                .setPositiveButton("Restore", (dialog, which) -> {
                    String pin = inputPin.getText().toString().trim();
                    if (!pin.isEmpty()) {
                        performRestore(backupUri, pin);
                    } else {
                        Toast.makeText(this, "PIN cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performRestore(Uri backupUri, String pin) {
        new Thread(() -> {
            File tempZip = new File(getCacheDir(), "temp_setup_restore.zip");
            try {
                InputStream in = getContentResolver().openInputStream(backupUri);
                if (in == null) throw new IOException("Cannot open backup file");

                byte[] salt = new byte[16];
                byte[] iv = new byte[CryptoUtils.IV_LENGTH];
                readFully(in, salt);
                readFully(in, iv);

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] data = new byte[4096];
                int nRead;
                while ((nRead = in.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                in.close();

                // AES-GCM decryption will fail and throw if the PIN/key is wrong
                SecretKey key = CryptoUtils.deriveKey(pin, salt);
                byte[] decryptedZip = CryptoUtils.decrypt(buffer.toByteArray(), key, iv);

                try (FileOutputStream fos = new FileOutputStream(tempZip)) {
                    fos.write(decryptedZip);
                }

                LockBoxDatabase.destroyInstance();
                try (ZipInputStream zis = new ZipInputStream(new FileInputStream(tempZip))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        String name = entry.getName();
                        if (name.contains("..")) throw new Exception("Invalid backup entry");

                        File outFile;
                        if (name.equals("lockbox_secure.db")) {
                            outFile = getDatabasePath("lockbox_secure.db");
                            //noinspection ResultOfMethodCallIgnored
                            new File(outFile.getPath() + "-wal").delete();
                            //noinspection ResultOfMethodCallIgnored
                            new File(outFile.getPath() + "-shm").delete();
                        } else if (name.equals("LockBoxPrefs.xml")) {
                            outFile = new File(getApplicationInfo().dataDir + "/shared_prefs/LockBoxPrefs.xml");
                        } else {
                            continue;
                        }

                        File parent = outFile.getParentFile();
                        if (parent != null && !parent.exists()) {
                            //noinspection ResultOfMethodCallIgnored
                            parent.mkdirs();
                        }

                        try (FileOutputStream fos = new FileOutputStream(outFile)) {
                            byte[] buf = new byte[4096];
                            int len;
                            while ((len = zis.read(buf)) > 0) {
                                fos.write(buf, 0, len);
                            }
                        }
                        zis.closeEntry();
                    }
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Restore successful!", Toast.LENGTH_LONG).show();
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
                runOnUiThread(() -> Toast.makeText(this, "Restore failed: Incorrect PIN or invalid file", Toast.LENGTH_LONG).show());
            } finally {
                if (tempZip.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    tempZip.delete();
                }
            }
        }).start();
    }

    private void readFully(InputStream in, byte[] b) throws IOException {
        int offset = 0;
        while (offset < b.length) {
            int read = in.read(b, offset, b.length - offset);
            if (read == -1) throw new IOException("Unexpected end of stream");
            offset += read;
        }
    }
}