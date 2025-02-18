package com.feri.faceguard3d.services;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.feri.faceguard3d.activities.FaceAuthenticationActivity;
import com.feri.faceguard3d.managers.ContentHidingManager;
import com.feri.faceguard3d.managers.SecuritySettingsManager;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AppMonitoringService extends AccessibilityService{

    private static final String TAG = "AppMonitoringService";
    private SecuritySettingsManager securityManager;
    private ContentHidingManager contentHidingManager;
    private ExecutorService executor;
    private ScheduledExecutorService scheduledExecutor;
    private String lastForegroundApp = "";
    private boolean isAuthenticationRequired = false;

    @Override
    public void onCreate() {
        super.onCreate();
        securityManager = SecuritySettingsManager.getInstance(this);
        contentHidingManager = new ContentHidingManager(this);
        executor = Executors.newSingleThreadExecutor();
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

        // Pornește verificarea periodică a aplicațiilor
        startPeriodicCheck();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName() != null ?
                    event.getPackageName().toString() : "";

            if (!packageName.equals(lastForegroundApp)) {
                lastForegroundApp = packageName;
                checkAppProtection(packageName);
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Service interrupted");
    }

    private void startPeriodicCheck() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            String currentApp = getCurrentForegroundApp();
            if (currentApp != null && !currentApp.equals(lastForegroundApp)) {
                lastForegroundApp = currentApp;
                checkAppProtection(currentApp);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private String getCurrentForegroundApp() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();

        if (processes != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : processes) {
                if (processInfo.importance ==
                        ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    return processInfo.processName;
                }
            }
        }
        return null;
    }

    private void checkAppProtection(String packageName) {
        executor.execute(() -> {
            Set<String> protectedApps = securityManager.getProtectedApps();

            if (protectedApps.contains(packageName)) {
                if (!isAuthenticationRequired) {
                    isAuthenticationRequired = true;
                    requestAuthentication();
                }
            } else {
                isAuthenticationRequired = false;
            }
        });
    }

    private void requestAuthentication() {
        // Ascunde conținutul până la autentificare
        contentHidingManager.hideProtectedContent();

        // Lansează activitatea de autentificare
        Intent authIntent = new Intent(this, FaceAuthenticationActivity.class);
        authIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(authIntent);
    }

    private void checkMultipleFaces() {
        Intent faceDetectionIntent = new Intent(this, FaceDetectionService.class);
        startService(faceDetectionIntent);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Service connected");

        // Verifică și pornește serviciul de detecție a fețelor
        if (!isServiceRunning(FaceDetectionService.class)) {
            startService(new Intent(this, FaceDetectionService.class));
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        scheduledExecutor.shutdown();

        // Repornește serviciul dacă a fost oprit
        Intent restartService = new Intent(this, AppMonitoringService.class);
        startService(restartService);
    }

    public static boolean isAccessibilityServiceEnabled(Context context) {
        try {
            int accessibilityEnabled = android.provider.Settings.Secure.getInt(
                    context.getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);

            if (accessibilityEnabled == 1) {
                String servicesString = android.provider.Settings.Secure.getString(
                        context.getContentResolver(),
                        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

                if (servicesString != null) {
                    return servicesString.contains(context.getPackageName() + "/" +
                            AppMonitoringService.class.getName());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking accessibility service status", e);
        }
        return false;
    }

    public static void openAccessibilitySettings(Context context) {
        Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}