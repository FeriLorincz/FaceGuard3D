package com.feri.faceguard3d.models;

import android.graphics.drawable.Drawable;

public class AppInfo {

    private String name;
    private String packageName;
    private Drawable icon;
    private boolean isProtected;
    private long lastAccess;
    private boolean isHidden;

    public AppInfo(String name, String packageName, Drawable icon, boolean isProtected) {
        this.name = name;
        this.packageName = packageName;
        this.icon = icon;
        this.isProtected = isProtected;
        this.lastAccess = System.currentTimeMillis();
        this.isHidden = false;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean isProtected() {
        return isProtected;
    }

    public long getLastAccess() {
        return lastAccess;
    }

    public boolean isHidden() {
        return isHidden;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public void setProtected(boolean isProtected) {
        this.isProtected = isProtected;
    }

    public void setLastAccess(long lastAccess) {
        this.lastAccess = lastAccess;
    }

    public void setHidden(boolean hidden) {
        this.isHidden = hidden;
    }

    public void updateLastAccess() {
        this.lastAccess = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppInfo appInfo = (AppInfo) o;
        return packageName.equals(appInfo.packageName);
    }

    @Override
    public int hashCode() {
        return packageName.hashCode();
    }

    @Override
    public String toString() {
        return "AppInfo{" +
                "name='" + name + '\'' +
                ", packageName='" + packageName + '\'' +
                ", isProtected=" + isProtected +
                ", lastAccess=" + lastAccess +
                ", isHidden=" + isHidden +
                '}';
    }
}
