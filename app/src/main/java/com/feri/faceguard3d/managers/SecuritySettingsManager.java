package com.feri.faceguard3d.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.feri.faceguard3d.models.FacialFeatures;
import com.feri.faceguard3d.models.HiddenContent;
import com.feri.faceguard3d.utils.SecurityUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SecuritySettingsManager {

    private static SecuritySettingsManager instance;
    private final SharedPreferences securePrefs;
    private final Context context;
    private static final String PREFS_FILE_NAME = "secure_settings";
    private static final String KEY_FACE_ENROLLED = "face_enrolled";
    private static final String KEY_BACKUP_PASSWORD = "backup_password";
    private static final String KEY_PROTECTED_APPS = "protected_apps";
    private static final String KEY_PROTECTED_FILES = "protected_files";
    private static final String KEY_FIRST_RUN = "first_run";
    private static final String KEY_FACIAL_FEATURES = "facial_features";
    private static final String KEY_HIDDEN_CONTENT = "hidden_content";
    private static final String KEY_ADAPTIVE_LEARNING = "adaptive_learning";
    private static final String KEY_MULTI_FACE_DETECTION = "multi_face_detection";
    private static final String KEY_NOTIFICATION_HIDING = "notification_hiding";
    private static final String KEY_APP_THUMBNAIL_HIDING = "app_thumbnail_hiding";
    private static final String DEFAULT_BACKUP_PASSWORD = "FaceGuard123";

    private SecuritySettingsManager(Context context) {
        this.context = context.getApplicationContext();

        try {
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    "master_key",
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build();

            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyGenParameterSpec(spec)
                    .build();

            securePrefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Could not create secure preferences", e);
        }
    }

    public static synchronized SecuritySettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SecuritySettingsManager(context);
        }
        return instance;
    }

    public boolean isFirstRun() {
        return securePrefs.getBoolean(KEY_FIRST_RUN, true);
    }

    public void setFirstRunCompleted() {
        securePrefs.edit().putBoolean(KEY_FIRST_RUN, false).apply();
    }

    public boolean isFaceEnrolled() {
        return securePrefs.getBoolean(KEY_FACE_ENROLLED, false);
    }

    public void setFaceEnrolled(boolean enrolled) {
        securePrefs.edit().putBoolean(KEY_FACE_ENROLLED, enrolled).apply();
    }

    public void initializeDefaultBackupPassword() {
        if (getBackupPassword() == null) {
            String hashedPassword = SecurityUtils.hashPassword(DEFAULT_BACKUP_PASSWORD);
            setBackupPassword(hashedPassword);
        }
    }

    public void setBackupPassword(String hashedPassword) {
        securePrefs.edit().putString(KEY_BACKUP_PASSWORD, hashedPassword).apply();
    }

    public String getBackupPassword() {
        return securePrefs.getString(KEY_BACKUP_PASSWORD, null);
    }

    public void addProtectedApp(String packageName) {
        Set<String> protectedApps = new HashSet<>(getProtectedApps());
        protectedApps.add(packageName);
        securePrefs.edit().putStringSet(KEY_PROTECTED_APPS, protectedApps).apply();
    }

    public void removeProtectedApp(String packageName) {
        Set<String> protectedApps = new HashSet<>(getProtectedApps());
        protectedApps.remove(packageName);
        securePrefs.edit().putStringSet(KEY_PROTECTED_APPS, protectedApps).apply();
    }

    public Set<String> getProtectedApps() {
        return securePrefs.getStringSet(KEY_PROTECTED_APPS, new HashSet<>());
    }

    public void addProtectedFile(String filePath) {
        Set<String> protectedFiles = new HashSet<>(getProtectedFiles());
        protectedFiles.add(filePath);
        securePrefs.edit().putStringSet(KEY_PROTECTED_FILES, protectedFiles).apply();
    }

    public void removeProtectedFile(String filePath) {
        Set<String> protectedFiles = new HashSet<>(getProtectedFiles());
        protectedFiles.remove(filePath);
        securePrefs.edit().putStringSet(KEY_PROTECTED_FILES, protectedFiles).apply();
    }

    public Set<String> getProtectedFiles() {
        return securePrefs.getStringSet(KEY_PROTECTED_FILES, new HashSet<>());
    }

    public void saveFacialFeatures(List<FacialFeatures> features) {
        Gson gson = new Gson();
        String json = gson.toJson(features);
        securePrefs.edit().putString(KEY_FACIAL_FEATURES, json).apply();
    }

    public List<FacialFeatures> getFacialFeatures() {
        String json = securePrefs.getString(KEY_FACIAL_FEATURES, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        return gson.fromJson(json, new TypeToken<List<FacialFeatures>>(){}.getType());
    }

    public void addHiddenContent(HiddenContent content) {
        List<HiddenContent> hiddenContents = getHiddenContents();
        hiddenContents.add(content);
        saveHiddenContents(hiddenContents);
    }

    public void removeHiddenContent(HiddenContent content) {
        List<HiddenContent> hiddenContents = getHiddenContents();
        hiddenContents.remove(content);
        saveHiddenContents(hiddenContents);
    }

    public void saveHiddenContents(List<HiddenContent> contents) {
        Gson gson = new Gson();
        String json = gson.toJson(contents);
        securePrefs.edit().putString(KEY_HIDDEN_CONTENT, json).apply();
    }

    public List<HiddenContent> getHiddenContents() {
        String json = securePrefs.getString(KEY_HIDDEN_CONTENT, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        return gson.fromJson(json, new TypeToken<List<HiddenContent>>(){}.getType());
    }

    public void clearAllSecuritySettings() {
        securePrefs.edit().clear().apply();
    }

    public static String hashPassword(String password) {
        // Implementarea metodei de hashing
        return SecurityUtils.hashPassword(password);
    }

    public boolean isAdaptiveLearningEnabled() {
        return securePrefs.getBoolean(KEY_ADAPTIVE_LEARNING, false);
    }

    public void setAdaptiveLearningEnabled(boolean enabled) {
        securePrefs.edit().putBoolean(KEY_ADAPTIVE_LEARNING, enabled).apply();
    }

    public boolean isMultiFaceDetectionEnabled() {
        return securePrefs.getBoolean(KEY_MULTI_FACE_DETECTION, true);
    }

    public void setMultiFaceDetectionEnabled(boolean enabled) {
        securePrefs.edit().putBoolean(KEY_MULTI_FACE_DETECTION, enabled).apply();
    }

    public boolean isNotificationHidingEnabled() {
        return securePrefs.getBoolean(KEY_NOTIFICATION_HIDING, true);
    }

    public void setNotificationHidingEnabled(boolean enabled) {
        securePrefs.edit().putBoolean(KEY_NOTIFICATION_HIDING, enabled).apply();
    }

    public boolean isAppThumbnailHidingEnabled() {
        return securePrefs.getBoolean(KEY_APP_THUMBNAIL_HIDING, true);
    }

    public void setAppThumbnailHidingEnabled(boolean enabled) {
        securePrefs.edit().putBoolean(KEY_APP_THUMBNAIL_HIDING, enabled).apply();
    }
}