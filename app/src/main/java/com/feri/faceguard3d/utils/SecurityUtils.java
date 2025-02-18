package com.feri.faceguard3d.utils;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SecurityUtils {

    private static final String TAG = "SecurityUtils";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "FaceGuard3DKey";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    public static byte[] getEncryptionKey(Context context) {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                generateKey();
            }

            KeyStore.SecretKeyEntry keyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(
                    KEY_ALIAS, null);
            return keyEntry.getSecretKey().getEncoded();
        } catch (Exception e) {
            Log.e(TAG, "Error getting encryption key", e);
            return null;
        }
    }

    private static void generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);

            KeyGenParameterSpec keySpec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .setKeySize(256)
                    .build();

            keyGenerator.init(keySpec);
            keyGenerator.generateKey();
        } catch (Exception e) {
            Log.e(TAG, "Error generating key", e);
        }
    }

    public static byte[] encrypt(byte[] data, int length, byte[] key) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);

            byte[] encryptedData = cipher.doFinal(data, 0, length);

            // Combină IV cu datele criptate
            byte[] combined = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);

            return combined;
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting data", e);
            return null;
        }
    }

    public static byte[] decrypt(byte[] encryptedData, int length, byte[] key) {
        try {
            // Extrage IV din datele criptate
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);

            // Extrage datele criptate
            byte[] ciphertext = new byte[length - GCM_IV_LENGTH];
            System.arraycopy(encryptedData, GCM_IV_LENGTH, ciphertext, 0,
                    length - GCM_IV_LENGTH);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

            cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);

            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting data", e);
            return null;
        }
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Error hashing password", e);
            return null;
        }
    }

    public static String generateEncryptedFileName() {
        return UUID.randomUUID().toString().replace("-", "") + ".enc";
    }

    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        android.app.ActivityManager manager =
                (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.encodeToString(randomBytes, Base64.NO_WRAP);
    }

    public static boolean verifyHash(String password, String storedHash) {
        String hashedPassword = hashPassword(password);
        return hashedPassword != null && hashedPassword.equals(storedHash);
    }
}
