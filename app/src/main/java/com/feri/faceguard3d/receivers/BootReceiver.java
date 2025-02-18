package com.feri.faceguard3d.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.feri.faceguard3d.services.AppMonitoringService;
import com.feri.faceguard3d.services.FaceAuthService;
import com.feri.faceguard3d.services.FaceDetectionService;
import com.feri.faceguard3d.managers.SecuritySettingsManager;
import com.feri.faceguard3d.utils.LogUtils;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null &&
                intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {

            LogUtils.i(TAG, "Device boot completed, starting services");

            SecuritySettingsManager securityManager =
                    SecuritySettingsManager.getInstance(context);

            // Verifică dacă există o față înregistrată
            if (securityManager.isFaceEnrolled()) {
                startServices(context);
            }
        }
    }

    private void startServices(Context context) {
        // Pornește serviciul de monitorizare a aplicațiilor
        if (AppMonitoringService.isAccessibilityServiceEnabled(context)) {
            Intent monitoringIntent = new Intent(context, AppMonitoringService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(monitoringIntent);
            } else {
                context.startService(monitoringIntent);
            }
            LogUtils.i(TAG, "Started AppMonitoringService");
        }

        // Pornește serviciul de autentificare facială
        Intent authIntent = new Intent(context, FaceAuthService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(authIntent);
        } else {
            context.startService(authIntent);
        }
        LogUtils.i(TAG, "Started FaceAuthService");

        // Pornește serviciul de detecție a fețelor
        Intent detectionIntent = new Intent(context, FaceDetectionService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(detectionIntent);
        } else {
            context.startService(detectionIntent);
        }
        LogUtils.i(TAG, "Started FaceDetectionService");
    }
}
