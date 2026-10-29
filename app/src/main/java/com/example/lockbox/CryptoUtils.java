package com.example.lockbox;

import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {

    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH = 256;

    // AES-GCM encrypts AND authenticates, so a tampered or wrong-key backup fails to decrypt
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;

    public static final int SALT_LENGTH = 16;
    public static final int IV_LENGTH = 12;

    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    public static SecretKey deriveKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    public static byte[] encrypt(byte[] data, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] data, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return cipher.doFinal(data);
    }
}