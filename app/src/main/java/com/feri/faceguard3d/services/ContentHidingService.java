package com.feri.faceguard3d.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.feri.faceguard3d.R;
import com.feri.faceguard3d.managers.ContentHidingManager;
import com.feri.faceguard3d.managers.SecuritySettingsManager;
import com.feri.faceguard3d.models.HiddenContent;
import com.feri.faceguard3d.utils.LogUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ContentHidingService extends Service {

    private static final String TAG = "ContentHidingService";
    private static final String CHANNEL_ID = "content_hiding_channel";
    private static final int NOTIFICATION_ID = 2;

    private ContentHidingManager contentHidingManager;
    private SecuritySettingsManager securityManager;
    private ExecutorService executor;
    private ScheduledExecutorService scheduledExecutor;
    private boolean isContentHidden = false;

    @Override
    public void onCreate() {
        super.onCreate();
        contentHidingManager = new ContentHidingManager(this);
        securityManager = SecuritySettingsManager.getInstance(this);
        executor = Executors.newSingleThreadExecutor();
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        // Începe verificarea periodică
        startPeriodicCheck();
        LogUtils.i(TAG, "ContentHidingService started");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Content Hiding Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Manages protected content visibility");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.content_protection_active))
                .setContentText(getString(R.string.monitoring_protected_content))
                .setSmallIcon(R.drawable.ic_shield)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startPeriodicCheck() {
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            if (securityManager.isMultiFaceDetectionEnabled()) {
                checkForMultipleFaces();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void checkForMultipleFaces() {
        executor.execute(() -> {
            if (FaceDetectionService.areMultipleFacesDetected()) {
                hideProtectedContent();
            }
        });
    }

    public void hideProtectedContent() {
        if (isContentHidden) return;

        executor.execute(() -> {
            try {
                isContentHidden = true;
                List<HiddenContent> contents = securityManager.getHiddenContents();

                for (HiddenContent content : contents) {
                    if (!content.isEncrypted()) {
                        contentHidingManager.hideContent(content);
                    }
                }

                LogUtils.i(TAG, "Protected content hidden");
            } catch (Exception e) {
                LogUtils.e(TAG, "Error hiding content", e);
            }
        });
    }

    public void showProtectedContent() {
        if (!isContentHidden) return;

        executor.execute(() -> {
            try {
                isContentHidden = false;
                List<HiddenContent> contents = securityManager.getHiddenContents();

                for (HiddenContent content : contents) {
                    if (content.isEncrypted()) {
                        contentHidingManager.revealContent(content);
                    }
                }

                LogUtils.i(TAG, "Protected content revealed");
            } catch (Exception e) {
                LogUtils.e(TAG, "Error revealing content", e);
            }
        });
    }

    public boolean isContentCurrentlyHidden() {
        return isContentHidden;
    }

    private void stopPeriodicCheck() {
        if (scheduledExecutor != null && !scheduledExecutor.isShutdown()) {
            scheduledExecutor.shutdown();
            try {
                if (!scheduledExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPeriodicCheck();

        if (executor != null) {
            executor.shutdown();
        }

        // Asigură-te că conținutul este ascuns înainte de oprire
        if (!isContentHidden) {
            hideProtectedContent();
        }

        // Repornește serviciul dacă a fost oprit
        Intent restartService = new Intent(this, ContentHidingService.class);
        startService(restartService);

        LogUtils.i(TAG, "ContentHidingService destroyed and restarted");
    }
}