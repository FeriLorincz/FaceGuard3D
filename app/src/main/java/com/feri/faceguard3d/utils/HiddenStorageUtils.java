package com.feri.faceguard3d.utils;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.util.Log;

import com.feri.faceguard3d.models.HiddenContent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.UUID;

public class HiddenStorageUtils {

    private static final String TAG = "HiddenStorageUtils";
    private static final String HIDDEN_FOLDER = "protected_content";
    private static final String TEMP_FOLDER = "temp";
    private static final String NOMEDIA_FILE = ".nomedia";

    public static String getHiddenFolderPath(Context context) {
        File hiddenDir = new File(context.getFilesDir(), HIDDEN_FOLDER);
        if (!hiddenDir.exists()) {
            if (!hiddenDir.mkdirs()) {
                Log.e(TAG, "Failed to create hidden directory");
                return null;
            }
            createNoMediaFile(hiddenDir);
        }
        return hiddenDir.getAbsolutePath();
    }

    public static String getTempFolderPath(Context context) {
        File tempDir = new File(context.getCacheDir(), TEMP_FOLDER);
        if (!tempDir.exists()) {
            if (!tempDir.mkdirs()) {
                Log.e(TAG, "Failed to create temp directory");
                return null;
            }
        }
        return tempDir.getAbsolutePath();
    }

    private static void createNoMediaFile(File directory) {
        File noMedia = new File(directory, NOMEDIA_FILE);
        if (!noMedia.exists()) {
            try {
                if (!noMedia.createNewFile()) {
                    Log.e(TAG, "Failed to create .nomedia file");
                }
            } catch (IOException e) {
                Log.e(TAG, "Error creating .nomedia file", e);
            }
        }
    }

    public static String generateHiddenFilePath(Context context, String originalExtension) {
        String hiddenFolder = getHiddenFolderPath(context);
        if (hiddenFolder == null) return null;

        String randomName = UUID.randomUUID().toString();
        return hiddenFolder + File.separator + randomName +
                (originalExtension != null ? "." + originalExtension : "");
    }

    public static boolean moveFileToHiddenStorage(Context context,
                                                  HiddenContent content, boolean encrypt) {
        File sourceFile = new File(content.getOriginalPath());
        if (!sourceFile.exists()) {
            Log.e(TAG, "Source file does not exist: " + content.getOriginalPath());
            return false;
        }

        String extension = getFileExtension(sourceFile.getName());
        String hiddenPath = generateHiddenFilePath(context, extension);
        if (hiddenPath == null) return false;

        File destinationFile = new File(hiddenPath);

        try {
            if (encrypt) {
                // Criptează fișierul în timpul copierii
                return encryptAndMoveFile(sourceFile, destinationFile, context);
            } else {
                // Copiază fișierul fără criptare
                return copyFile(sourceFile, destinationFile) && sourceFile.delete();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error moving file to hidden storage", e);
            return false;
        }
    }

    public static boolean restoreFileFromHiddenStorage(Context context,
                                                       HiddenContent content, boolean decrypt) {
        File hiddenFile = new File(content.getHiddenPath());
        if (!hiddenFile.exists()) {
            Log.e(TAG, "Hidden file does not exist: " + content.getHiddenPath());
            return false;
        }

        File destinationFile = new File(content.getOriginalPath());
        try {
            if (decrypt) {
                // Decriptează fișierul în timpul restaurării
                return decryptAndMoveFile(hiddenFile, destinationFile, context);
            } else {
                // Copiază fișierul fără decriptare
                return copyFile(hiddenFile, destinationFile) && hiddenFile.delete();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error restoring file from hidden storage", e);
            return false;
        }
    }

    private static boolean copyFile(File source, File destination) throws IOException {
        try (FileChannel inputChannel = new FileInputStream(source).getChannel();
             FileChannel outputChannel = new FileOutputStream(destination).getChannel()) {
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
            return true;
        }
    }

    private static boolean encryptAndMoveFile(File source, File destination,
                                              Context context) throws IOException {
        // Obține cheia de criptare
        byte[] encryptionKey = SecurityUtils.getEncryptionKey(context);
        if (encryptionKey == null) return false;

        try (FileInputStream inStream = new FileInputStream(source);
             FileOutputStream outStream = new FileOutputStream(destination)) {

            byte[] buffer = new byte[8192];
            int count;
            while ((count = inStream.read(buffer)) > 0) {
                byte[] encryptedBuffer = SecurityUtils.encrypt(buffer, count, encryptionKey);
                if (encryptedBuffer != null) {
                    outStream.write(encryptedBuffer);
                }
            }

            return source.delete();
        }
    }

    private static boolean decryptAndMoveFile(File source, File destination,
                                              Context context) throws IOException {
        // Obține cheia de decriptare
        byte[] decryptionKey = SecurityUtils.getEncryptionKey(context);
        if (decryptionKey == null) return false;

        try (FileInputStream inStream = new FileInputStream(source);
             FileOutputStream outStream = new FileOutputStream(destination)) {

            byte[] buffer = new byte[8192];
            int count;
            while ((count = inStream.read(buffer)) > 0) {
                byte[] decryptedBuffer = SecurityUtils.decrypt(buffer, count, decryptionKey);
                if (decryptedBuffer != null) {
                    outStream.write(decryptedBuffer);
                }
            }

            return source.delete();
        }
    }

    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) return null;
        return fileName.substring(lastDot + 1);
    }

    public static void cleanupTempFiles(Context context) {
        String tempPath = getTempFolderPath(context);
        if (tempPath != null) {
            File tempDir = new File(tempPath);
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.delete()) {
                        Log.e(TAG, "Failed to delete temp file: " + file.getName());
                    }
                }
            }
        }
    }

    public static boolean deleteHiddenContent(HiddenContent content) {
        if (content.getHiddenPath() == null) return false;

        File hiddenFile = new File(content.getHiddenPath());
        return !hiddenFile.exists() || hiddenFile.delete();
    }

    public static void excludeFromMediaScanner(Context context, String path) {
        MediaScannerConnection.scanFile(context,
                new String[] { path }, null, null);
    }

    public static long getHiddenStorageSize(Context context) {
        String hiddenPath = getHiddenFolderPath(context);
        if (hiddenPath == null) return 0;

        File hiddenDir = new File(hiddenPath);
        return getFolderSize(hiddenDir);
    }

    private static long getFolderSize(File directory) {
        long size = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else {
                    size += getFolderSize(file);
                }
            }
        }
        return size;
    }
}
