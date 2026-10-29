package com.example.lockbox;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * PIN hashing helpers.
 * The PIN is never stored. Two independent values are derived from it, each with its own random salt:
 *  - a verifier (stored) used only to check the PIN at login
 *  - a database key (never stored) used as the SQLCipher passphrase
 * Because the salts differ, the stored verifier cannot be used to open the database.
 */
public class HashUtils {

    private static final int ITERATIONS = 120000;
    private static final int KEY_BITS = 256;

    public static byte[] generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static String pbkdf2Hex(String pin, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
            spec.clearPassword();
            return toHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] fromHex(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return out;
    }
}