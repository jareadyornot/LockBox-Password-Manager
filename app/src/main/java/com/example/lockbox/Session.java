package com.example.lockbox;

/**
 * In-memory only session. Holds the PIN (needed to encrypt/decrypt backups)
 * and the derived database key (needed to open the SQLCipher database).
 * Nothing here is ever written to disk.
 */
public class Session {
    private static String pin = "";
    private static String dbKey = "";

    public static synchronized void start(String newPin, String newDbKey) {
        pin = newPin;
        dbKey = newDbKey;
    }

    public static synchronized String getPin() {
        return pin;
    }

    public static synchronized String getDbKey() {
        return dbKey;
    }

    public static synchronized boolean isActive() {
        return dbKey != null && !dbKey.isEmpty();
    }

    public static synchronized void clear() {
        pin = "";
        dbKey = "";
    }
}