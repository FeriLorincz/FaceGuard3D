package com.feri.faceguard3d.managers;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import androidx.documentfile.provider.DocumentFile;

import com.feri.faceguard3d.models.HiddenContent;
import com.feri.faceguard3d.utils.SecurityUtils;
import com.feri.faceguard3d.utils.HiddenStorageUtils;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContentHidingManager {

    private static final String TAG = "ContentHidingManager";
    private final Context context;
    private final SecuritySettingsManager securityManager;
    private final ExecutorService executor;
    private final String hiddenFolderPath;
    private boolean isContentHidden = false;

    public ContentHidingManager(Context context) {
        this.context = context;
        this.securityManager = SecuritySettingsManager.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.hiddenFolderPath = HiddenStorageUtils.getHiddenFolderPath(context);
        initializeHiddenFolder();
    }

    private void initializeHiddenFolder() {
        File hiddenFolder = new File(hiddenFolderPath);
        if (!hiddenFolder.exists()) {
            if (!hiddenFolder.mkdirs()) {
                Log.e(TAG, "Failed to create hidden folder");
                return;
            }
            // Creează un fișier .nomedia pentru a ascunde media
            File nomedia = new File(hiddenFolder, ".nomedia");
            try {
                nomedia.createNewFile();
            } catch (Exception e) {
                Log.e(TAG, "Failed to create .nomedia file", e);
            }
        }
    }

    public void hideProtectedContent() {
        if (isContentHidden) return;

        executor.execute(() -> {
            try {
                // Ascunde aplicațiile protejate
                hideProtectedApps();

                // Ascunde fișierele protejate
                hideProtectedFiles();

                isContentHidden = true;
            } catch (Exception e) {
                Log.e(TAG, "Error hiding content", e);
            }
        });
    }

    public void showProtectedContent() {
        if (!isContentHidden) return;

        executor.execute(() -> {
            try {
                // Arată aplicațiile protejate
                showProtectedApps();

                // Arată fișierele protejate
                showProtectedFiles();

                isContentHidden = false;
            } catch (Exception e) {
                Log.e(TAG, "Error showing content", e);
            }
        });
    }

    private void hideProtectedApps() {
        Set<String> protectedApps = securityManager.getProtectedApps();
        PackageManager pm = context.getPackageManager();

        for (String packageName : protectedApps) {
            try {
                // Dezactivează temporar componentele aplicației
                pm.setApplicationEnabledSetting(
                        packageName,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
                        0);
            } catch (Exception e) {
                Log.e(TAG, "Error hiding app: " + packageName, e);
            }
        }
    }

    private void showProtectedApps() {
        Set<String> protectedApps = securityManager.getProtectedApps();
        PackageManager pm = context.getPackageManager();

        for (String packageName : protectedApps) {
            try {
                // Reactivează componentele aplicației
                pm.setApplicationEnabledSetting(
                        packageName,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        0);
            } catch (Exception e) {
                Log.e(TAG, "Error showing app: " + packageName, e);
            }
        }
    }

    private void hideProtectedFiles() {
        List<HiddenContent> contents = securityManager.getHiddenContents();

        for (HiddenContent content : contents) {
            if (content.isEncrypted()) continue; // Skip already encrypted content

            try {
                File originalFile = new File(content.getOriginalPath());
                if (!originalFile.exists()) continue;

                // Generează un nume de fișier criptat
                String encryptedName = SecurityUtils.generateEncryptedFileName();
                File hiddenFile = new File(hiddenFolderPath, encryptedName);

                // Copiază și criptează fișierul
                if (moveAndEncryptFile(originalFile, hiddenFile)) {
                    content.setHiddenPath(hiddenFile.getAbsolutePath());
                    content.setEncrypted(true);
                    updateHiddenContent(content);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error hiding file: " + content.getName(), e);
            }
        }
    }

    private void showProtectedFiles() {
        List<HiddenContent> contents = securityManager.getHiddenContents();

        for (HiddenContent content : contents) {
            if (!content.isEncrypted()) continue;

            try {
                File hiddenFile = new File(content.getHiddenPath());
                if (!hiddenFile.exists()) continue;

                File originalFile = new File(content.getOriginalPath());

                // Decriptează și mută fișierul înapoi
                if (decryptAndMoveFile(hiddenFile, originalFile)) {
                    content.setHiddenPath(null);
                    content.setEncrypted(false);
                    updateHiddenContent(content);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error showing file: " + content.getName(), e);
            }
        }
    }

    public void revealContent(HiddenContent content) {
        if (!content.isEncrypted()) {
            return;
        }

        executor.execute(() -> {
            try {
                File hiddenFile = new File(content.getHiddenPath());
                File originalFile = new File(content.getOriginalPath());

                // Decriptează și mută fișierul înapoi
                if (decryptAndMoveFile(hiddenFile, originalFile)) {
                    content.setHiddenPath(null);
                    content.setEncrypted(false);
                    updateHiddenContent(content);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error revealing content", e);
            }
        });
    }

    public void hideContent(HiddenContent content) {
        if (content.isEncrypted()) {
            return;
        }

        executor.execute(() -> {
            try {
                File originalFile = new File(content.getOriginalPath());
                String encryptedName = SecurityUtils.generateEncryptedFileName();
                File hiddenFile = new File(hiddenFolderPath, encryptedName);

                // Copiază și criptează fișierul
                if (moveAndEncryptFile(originalFile, hiddenFile)) {
                    content.setHiddenPath(hiddenFile.getAbsolutePath());
                    content.setEncrypted(true);
                    updateHiddenContent(content);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error hiding content", e);
            }
        });
    }


    private boolean moveAndEncryptFile(File source, File destination) {
        try {
            FileInputStream inStream = new FileInputStream(source);
            FileOutputStream outStream = new FileOutputStream(destination);

            // Obține cheia de criptare
            byte[] encryptionKey = SecurityUtils.getEncryptionKey(context);

            // Criptează datele în timpul copierii
            byte[] buffer = new byte[8192];
            int count;
            while ((count = inStream.read(buffer)) > 0) {
                byte[] encryptedBuffer = SecurityUtils.encrypt(buffer, count, encryptionKey);
                outStream.write(encryptedBuffer);
            }

            outStream.close();
            inStream.close();

            // Șterge fișierul original după criptare
            return source.delete();
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting file", e);
            return false;
        }
    }

    private boolean decryptAndMoveFile(File source, File destination) {
        try {
            FileInputStream inStream = new FileInputStream(source);
            FileOutputStream outStream = new FileOutputStream(destination);

            // Obține cheia de decriptare
            byte[] decryptionKey = SecurityUtils.getEncryptionKey(context);

            // Decriptează datele în timpul copierii
            byte[] buffer = new byte[8192];
            int count;
            while ((count = inStream.read(buffer)) > 0) {
                byte[] decryptedBuffer = SecurityUtils.decrypt(buffer, count, decryptionKey);
                outStream.write(decryptedBuffer);
            }

            outStream.close();
            inStream.close();

            // Șterge fișierul criptat după decriptare
            return source.delete();
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting file", e);
            return false;
        }
    }

    private void updateHiddenContent(HiddenContent content) {
        List<HiddenContent> contents = securityManager.getHiddenContents();
        for (int i = 0; i < contents.size(); i++) {
            if (contents.get(i).getId().equals(content.getId())) {
                contents.set(i, content);
                break;
            }
        }
        securityManager.saveHiddenContents(contents);
    }

    public void addProtectedContent(HiddenContent content) {
        List<HiddenContent> contents = securityManager.getHiddenContents();
        contents.add(content);
        securityManager.saveHiddenContents(contents);
    }

    public void removeProtectedContent(String id) {
        List<HiddenContent> contents = securityManager.getHiddenContents();
        contents.removeIf(content -> content.getId().equals(id));
        securityManager.saveHiddenContents(contents);
    }

    public List<HiddenContent> getProtectedContents() {
        return securityManager.getHiddenContents();
    }

    public void release() {
        executor.shutdown();
    }
}
