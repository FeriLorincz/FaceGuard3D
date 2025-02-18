package com.feri.faceguard3d.managers;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import com.feri.faceguard3d.activities.FaceAuthenticationActivity;
import com.feri.faceguard3d.models.AppInfo;
import com.feri.faceguard3d.utils.LogUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppProtectionManager {

    private static final String TAG = "AppProtectionManager";

    private final Context context;
    private final SecuritySettingsManager securityManager;
    private final ExecutorService executor;
    private Set<String> protectedApps;
    private String lastForegroundApp = "";
    private boolean isAuthenticationRequired = false;

    public AppProtectionManager(Context context) {
        this.context = context.getApplicationContext();
        this.securityManager = SecuritySettingsManager.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
        loadProtectedApps();
    }

    private void loadProtectedApps() {
        protectedApps = new HashSet<>(securityManager.getProtectedApps());
    }

    public void addProtectedApp(String packageName) {
        executor.execute(() -> {
            protectedApps.add(packageName);
            securityManager.addProtectedApp(packageName);
            LogUtils.i(TAG, "Added protected app: " + packageName);
        });
    }

    public void removeProtectedApp(String packageName) {
        executor.execute(() -> {
            protectedApps.remove(packageName);
            securityManager.removeProtectedApp(packageName);
            LogUtils.i(TAG, "Removed protected app: " + packageName);
        });
    }

    public boolean isAppProtected(String packageName) {
        return protectedApps.contains(packageName);
    }

    public void checkAppProtection(String packageName) {
        if (!packageName.equals(lastForegroundApp)) {
            lastForegroundApp = packageName;

            if (protectedApps.contains(packageName)) {
                if (!isAuthenticationRequired) {
                    isAuthenticationRequired = true;
                    requestAuthentication();
                }
            } else {
                isAuthenticationRequired = false;
            }
        }
    }

    private void requestAuthentication() {
        Intent authIntent = new Intent(context, FaceAuthenticationActivity.class);
        authIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(authIntent);
    }

    public List<AppInfo> getInstalledApps() {
        List<AppInfo> appList = new ArrayList<>();
        PackageManager pm = context.getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
        Set<String> bundleIds = new HashSet<>();

        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.activityInfo == null || resolveInfo.activityInfo.packageName == null) {
                continue;
            }

            String packageName = resolveInfo.activityInfo.packageName;
            if (bundleIds.contains(packageName)) {
                continue;
            }
            bundleIds.add(packageName);

            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                // Exclude aplicațiile de sistem
                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    AppInfo app = new AppInfo(
                            resolveInfo.loadLabel(pm).toString(),
                            packageName,
                            resolveInfo.loadIcon(pm),
                            protectedApps.contains(packageName)
                    );
                    appList.add(app);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Error getting app info for " + packageName, e);
            }
        }

        return appList;
    }

    public String getCurrentForegroundApp() {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
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

    public void handleMultipleFacesDetected() {
        String currentApp = getCurrentForegroundApp();
        if (currentApp != null && protectedApps.contains(currentApp)) {
            // Ascunde aplicația curentă
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(homeIntent);

            LogUtils.i(TAG, "Protected app hidden due to multiple faces detected");
        }
    }

    public void updateProtectedAppsList() {
        executor.execute(() -> {
            Set<String> updatedApps = securityManager.getProtectedApps();
            synchronized (this) {
                protectedApps = new HashSet<>(updatedApps);
            }
        });
    }

    public void clearProtectedApps() {
        executor.execute(() -> {
            protectedApps.clear();
            securityManager.clearAllSecuritySettings();
            LogUtils.i(TAG, "All protected apps cleared");
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}