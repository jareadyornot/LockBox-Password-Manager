package com.example.lockbox.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SupportFactory;

import java.nio.charset.StandardCharsets;

@Database(entities = {CredentialEntity.class}, version = 1, exportSchema = false)
public abstract class LockBoxDatabase extends RoomDatabase {

    private static volatile LockBoxDatabase INSTANCE;

    public abstract CredentialDao credentialDao();

    public static synchronized LockBoxDatabase getInstance(Context context, String passphrase) {
        if (INSTANCE == null) {
            SQLiteDatabase.loadLibs(context);

            SupportFactory factory = new SupportFactory(passphrase.getBytes(StandardCharsets.UTF_8));

            // No fallbackToDestructiveMigration(): in a password manager a schema change
            // must never silently wipe the user's saved credentials.
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            LockBoxDatabase.class, "lockbox_secure.db")
                    .openHelperFactory(factory)
                    // No WAL file: every write goes straight into lockbox_secure.db,
                    // so copying that single file for a backup is always complete.
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .build();
        }
        return INSTANCE;
    }

    // Clears the cached instance so a fresh key can be applied safely
    public static synchronized void destroyInstance() {
        if (INSTANCE != null) {
            if (INSTANCE.isOpen()) {
                INSTANCE.close();
            }
            INSTANCE = null;
        }
    }
}