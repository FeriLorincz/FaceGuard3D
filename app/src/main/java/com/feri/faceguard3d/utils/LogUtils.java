package com.feri.faceguard3d.utils;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogUtils {

    private static final String TAG = "FaceGuard3D";
    private static final boolean DEBUG = true;
    private static final String LOG_FILE_PREFIX = "faceguard3d_log_";
    private static final String LOG_FILE_EXTENSION = ".txt";
    private static final long MAX_LOG_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    private static ExecutorService executor = Executors.newSingleThreadExecutor();
    private static File logFile;
    private static Context appContext;

    public static void init(Context context) {
        appContext = context.getApplicationContext();
        createNewLogFile();
    }

    public static void d(String tag, String message) {
        if (DEBUG) {
            Log.d(tag, message);
            writeToFile("DEBUG", tag, message);
        }
    }

    public static void i(String tag, String message) {
        Log.i(tag, message);
        writeToFile("INFO", tag, message);
    }

    public static void w(String tag, String message) {
        Log.w(tag, message);
        writeToFile("WARN", tag, message);
    }

    public static void e(String tag, String message) {
        Log.e(tag, message);
        writeToFile("ERROR", tag, message);
    }

    public static void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
        writeToFile("ERROR", tag, message + "\n" + Log.getStackTraceString(throwable));
    }

    private static void writeToFile(String level, String tag, String message) {
        if (appContext == null || logFile == null) return;

        executor.execute(() -> {
            try {
                // Verifică dimensiunea fișierului
                if (logFile.length() > MAX_LOG_FILE_SIZE) {
                    createNewLogFile();
                }

                // Formatează mesajul de log
                String timestamp = dateFormat.format(new Date());
                String logMessage = String.format("%s [%s] %s: %s\n",
                        timestamp, level, tag, message);

                // Scrie în fișier
                FileWriter writer = new FileWriter(logFile, true);
                writer.append(logMessage);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                Log.e(TAG, "Error writing to log file", e);
            }
        });
    }

    private static void createNewLogFile() {
        if (appContext == null) return;

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        String fileName = LOG_FILE_PREFIX + timestamp + LOG_FILE_EXTENSION;

        File logsDir = new File(appContext.getFilesDir(), "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }

        // Șterge fișierele vechi de log dacă există prea multe
        cleanOldLogs(logsDir);

        logFile = new File(logsDir, fileName);
    }

    private static void cleanOldLogs(File logsDir) {
        File[] logFiles = logsDir.listFiles((dir, name) ->
                name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_EXTENSION));

        if (logFiles != null && logFiles.length > 5) { // Păstrează doar ultimele 5 fișiere
            // Sortează fișierele după data modificării
            java.util.Arrays.sort(logFiles, (f1, f2) ->
                    Long.compare(f2.lastModified(), f1.lastModified()));

            // Șterge fișierele vechi
            for (int i = 5; i < logFiles.length; i++) {
                logFiles[i].delete();
            }
        }
    }

    public static String getLogFilePath() {
        return logFile != null ? logFile.getAbsolutePath() : null;
    }

    public static void logSecurityEvent(String event, String details) {
        String securityMessage = String.format("Security Event: %s - Details: %s",
                event, details);
        i(TAG, securityMessage);
    }

    public static void logAuthenticationAttempt(boolean success, String details) {
        String authMessage = String.format("Authentication Attempt: %s - %s",
                success ? "Success" : "Failure", details);
        i(TAG, authMessage);
    }

    public static void logSystemEvent(String event) {
        i(TAG, "System Event: " + event);
    }

    public static void shutdown() {
        executor.shutdown();
    }
}
