package com.feri.faceguard3d;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.feri.faceguard3d.managers.SecuritySettingsManager;
import com.feri.faceguard3d.services.AppMonitoringService;
import com.feri.faceguard3d.services.FaceAuthService;
import com.feri.faceguard3d.services.FaceDetectionService;
import com.feri.faceguard3d.utils.LogUtils;

public class FaceGuard3DApplication extends Application {

    static {
        try {
            System.loadLibrary("opencv_java4100");
        } catch (UnsatisfiedLinkError e) {
            Log.e("OpenCV", "Error loading OpenCV: " + e.getMessage());
        }
    }

    private static final String TAG = "FaceGuard3DApp";

    public static final String CHANNEL_ID = "FaceGuard3DChannel";
    private static FaceGuard3DApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Inițializează sistemul de logging
        LogUtils.init(this);
        LogUtils.i(TAG, "Application started");

        // Creează canalul de notificări pentru Android 8.0+
        createNotificationChannel();

        // Inițializează managerul de setări de securitate
        SecuritySettingsManager.getInstance(this);

        // Pornește serviciile necesare dacă există o față înregistrată
        if (SecuritySettingsManager.getInstance(this).isFaceEnrolled()) {
            startServices();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "FaceGuard3D Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Channel for FaceGuard3D security services");
            serviceChannel.enableVibration(false);
            serviceChannel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
                LogUtils.i(TAG, "Notification channel created");
            }
        }
    }

    private void startServices() {
        // Verifică și pornește serviciul de monitorizare a aplicațiilor
        if (AppMonitoringService.isAccessibilityServiceEnabled(this)) {
            startService(AppMonitoringService.class);
            LogUtils.i(TAG, "AppMonitoringService started");
        }

        // Pornește serviciul de autentificare facială
        startService(FaceAuthService.class);
        LogUtils.i(TAG, "FaceAuthService started");

        // Pornește serviciul de detecție a fețelor
        startService(FaceDetectionService.class);
        LogUtils.i(TAG, "FaceDetectionService started");
    }

    private void startService(Class<?> serviceClass) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new android.content.Intent(this, serviceClass));
        } else {
            startService(new android.content.Intent(this, serviceClass));
        }
    }

    public static FaceGuard3DApplication getInstance() {
        return instance;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_MODERATE) {
            // Eliberează resursele care nu sunt esențiale
            System.gc();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        // Eliberează resursele care consumă multă memorie
        System.gc();
        LogUtils.w(TAG, "Low memory condition detected");
    }

    public void restartServices() {
        // Oprește serviciile existente
        stopService(new android.content.Intent(this, AppMonitoringService.class));
        stopService(new android.content.Intent(this, FaceAuthService.class));
        stopService(new android.content.Intent(this, FaceDetectionService.class));

        // Repornește serviciile
        startServices();
        LogUtils.i(TAG, "Services restarted");
    }

    public void cleanup() {
        // Oprește serviciile
        stopService(new android.content.Intent(this, AppMonitoringService.class));
        stopService(new android.content.Intent(this, FaceAuthService.class));
        stopService(new android.content.Intent(this, FaceDetectionService.class));

        // Curăță resursele
        LogUtils.shutdown();
        System.gc();
        LogUtils.i(TAG, "Application cleanup completed");
    }
}