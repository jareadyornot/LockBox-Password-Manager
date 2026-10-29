package com.example.lockbox;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
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
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.SecretKey;

public class DataStorageActivity extends AppCompatActivity {

    private TextView tvStoragePath;
    private ActivityResultLauncher<Intent> selectDirectoryLauncher;
    private ActivityResultLauncher<Intent> restoreFileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_storage);

        ImageView ivBack = findViewById(R.id.ivBack);
        tvStoragePath = findViewById(R.id.tvStoragePath);

        ivBack.setOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences("LockBoxPrefs", MODE_PRIVATE);
        String savedUriString = prefs.getString("backup_folder_uri", "");
        if (!savedUriString.isEmpty()) {
            tvStoragePath.setText(R.string.folder_configured);
        }

        // Initialize Activity Result Launchers
        selectDirectoryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                            getContentResolver().takePersistableUriPermission(uri, takeFlags);

                            SharedPreferences.Editor editor = getSharedPreferences("LockBoxPrefs", MODE_PRIVATE).edit();
                            editor.putString("backup_folder_uri", uri.toString()).apply();

                            tvStoragePath.setText(R.string.folder_configured);
                            Toast.makeText(this, R.string.backup_folder_selected, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        restoreFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            askBackupPinAndRestore(uri);
                        }
                    }
                }
        );

        tvStoragePath.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            selectDirectoryLauncher.launch(intent);
        });

        findViewById(R.id.btnCreateBackup).setOnClickListener(v -> createEncryptedBackup());

        findViewById(R.id.btnRestoreBackup).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/octet-stream");
            restoreFileLauncher.launch(intent);
        });

        findViewById(R.id.rowResetData).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Reset app data?")
                        .setMessage("This permanently deletes all saved credentials and your master PIN from this app. "
                                + "Backup files in your storage folder are NOT deleted. This cannot be undone.")
                        .setPositiveButton("Reset", (d, w) -> resetAppData())
                        .setNegativeButton("Cancel", null)
                        .show()
        );

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

    // Returns the app to its first-install state. Backup files in the user's chosen
    // folder live outside the app's private storage, so they are left untouched.
    private void resetAppData() {
        new Thread(() -> {
            try {
                // Close the encrypted database and forget the session before deleting files
                LockBoxDatabase.destroyInstance();
                Session.clear();

                // Deletes lockbox_secure.db plus its -journal / -wal / -shm companions
                deleteDatabase("lockbox_secure.db");

                // Master PIN verifier, salts and backup folder setting
                getSharedPreferences("LockBoxPrefs", MODE_PRIVATE).edit().clear().commit();

                // Leftover temporary backup/restore files
                File[] cached = getCacheDir().listFiles();
                if (cached != null) {
                    for (File f : cached) {
                        //noinspection ResultOfMethodCallIgnored
                        f.delete();
                    }
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "App data reset", Toast.LENGTH_LONG).show();
                    // MainActivity sees no PIN and shows the first-time setup screen
                    Intent i = new Intent(this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finishAffinity();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Reset failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void createEncryptedBackup() {
        SharedPreferences prefs = getSharedPreferences("LockBoxPrefs", MODE_PRIVATE);
        String savedUriString = prefs.getString("backup_folder_uri", "");
        String currentPin = Session.getPin();

        if (savedUriString.isEmpty()) {
            Toast.makeText(this, R.string.select_storage_location, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Session.isActive()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            File tempZip = new File(getCacheDir(), "temp_backup.zip");
            try {
                File dbFile = getDatabasePath("lockbox_secure.db");

                // Flush pending WAL data into the main database file so the copy is complete
                // The cursor must be read, otherwise SQLite never executes the PRAGMA
                try (android.database.Cursor c = LockBoxDatabase.getInstance(this, Session.getDbKey())
                        .getOpenHelper().getWritableDatabase()
                        .query("PRAGMA wal_checkpoint(TRUNCATE)")) {
                    c.moveToFirst();
                }

                if (!dbFile.exists()) throw new IOException("Database file not found");

                File sharedPrefsFile = new File(getApplicationInfo().dataDir + "/shared_prefs/LockBoxPrefs.xml");

                try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZip))) {
                    addToZip(zos, dbFile, "lockbox_secure.db");
                    if (sharedPrefsFile.exists()) {
                        addToZip(zos, sharedPrefsFile, "LockBoxPrefs.xml");
                    }
                }

                byte[] zipBytes = readFileToByteArray(tempZip);
                byte[] salt = CryptoUtils.generateSalt();
                SecretKey key = CryptoUtils.deriveKey(currentPin, salt);

                byte[] iv = CryptoUtils.generateIv();

                byte[] encryptedData = CryptoUtils.encrypt(zipBytes, key, iv);

                Uri treeUri = Uri.parse(savedUriString);
                String docId = DocumentsContract.getTreeDocumentId(treeUri);
                Uri parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);

                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String backupFileName = "lockbox_backup_" + timestamp + ".lbox";

                Uri newFileUri = DocumentsContract.createDocument(
                        getContentResolver(),
                        parentDocUri,
                        "application/octet-stream",
                        backupFileName
                );

                if (newFileUri != null) {
                    try (OutputStream out = getContentResolver().openOutputStream(newFileUri)) {
                        if (out != null) {
                            out.write(salt);
                            out.write(iv);
                            out.write(encryptedData);
                            out.flush();
                        }
                    }
                    runOnUiThread(() -> Toast.makeText(this, R.string.backup_created, Toast.LENGTH_LONG).show());
                }

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.backup_failed, e.getMessage()), Toast.LENGTH_LONG).show());
            } finally {
                if (tempZip.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    tempZip.delete();
                }
            }
        }).start();
    }

    // The backup is encrypted with the PIN that was active when it was created, which may
    // differ from the current PIN (for example after a reset or on a new phone), so ask for it.
    private void askBackupPinAndRestore(Uri backupUri) {
        EditText input = new EditText(this);
        input.setHint("PIN used when the backup was made");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(this);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Restore backup")
                .setMessage("Enter the master PIN that was set when this backup was created. "
                        + "Restoring replaces the current credentials and master PIN with those in the backup.")
                .setView(container)
                .setPositiveButton("Restore", (d, w) -> {
                    String backupPin = input.getText().toString().trim();
                    if (backupPin.isEmpty()) {
                        Toast.makeText(this, "Enter the backup PIN", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    restoreEncryptedBackup(backupUri, backupPin);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void restoreEncryptedBackup(Uri backupUri, String backupPin) {
        String currentPin = backupPin;
        new Thread(() -> {
            File tempZip = new File(getCacheDir(), "temp_restore.zip");
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

                SecretKey key = CryptoUtils.deriveKey(currentPin, salt);
                byte[] decryptedZip = CryptoUtils.decrypt(buffer.toByteArray(), key, iv);

                try (FileOutputStream fos = new FileOutputStream(tempZip)) {
                    fos.write(decryptedZip);
                }

                LockBoxDatabase.destroyInstance();
                try (ZipInputStream zis = new ZipInputStream(new FileInputStream(tempZip))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        String name = entry.getName();
                        if (name.contains("..")) throw new Exception(getString(R.string.invalid_backup_entry));

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

                // The old session belongs to the old database and preferences
                Session.clear();

                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.restore_successful, Toast.LENGTH_LONG).show();
                    Intent i = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    if (i != null) {
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                    }
                    finishAffinity();

                    // SharedPreferences are cached in memory and would overwrite the restored file,
                    // so restart the process to make the app load the restored data fresh.
                    new Handler(Looper.getMainLooper()).postDelayed(
                            () -> android.os.Process.killProcess(android.os.Process.myPid()), 500);
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, R.string.restore_failed, Toast.LENGTH_LONG).show());
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

    private void addToZip(ZipOutputStream zos, File file, String entryName) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            zos.putNextEntry(new ZipEntry(entryName));
            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            zos.closeEntry();
        }
    }

    private byte[] readFileToByteArray(File file) throws Exception {
        byte[] b = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int readTotal = 0;
            while (readTotal < b.length) {
                int read = fis.read(b, readTotal, b.length - readTotal);
                if (read == -1) break;
                readTotal += read;
            }
        }
        return b;
    }
}