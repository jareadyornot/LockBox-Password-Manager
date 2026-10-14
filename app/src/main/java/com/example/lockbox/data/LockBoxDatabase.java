package com.example.lockbox.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SupportFactory;

@Database(entities = {CredentialEntity.class}, version = 1, exportSchema = false)
public abstract class LockBoxDatabase extends RoomDatabase {

    private static volatile LockBoxDatabase INSTANCE;

    public abstract CredentialDao credentialDao();

    public static synchronized LockBoxDatabase getInstance(Context context, String passphrase) {
        if (INSTANCE == null) {
            SQLiteDatabase.loadLibs(context);

            SupportFactory factory = new SupportFactory(passphrase.getBytes());

            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            LockBoxDatabase.class, "lockbox_secure.db")
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
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