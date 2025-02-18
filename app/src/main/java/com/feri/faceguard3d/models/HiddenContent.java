package com.feri.faceguard3d.models;

import android.net.Uri;
import java.util.Objects;

public class HiddenContent {

    public enum ContentType {
        FILE,
        APP,
        CONTACT,
        IMAGE,
        VIDEO,
        TEXT
    }

    private String id;
    private String originalPath;
    private String hiddenPath;
    private ContentType contentType;
    private long size;
    private String mimeType;
    private String name;
    private long dateHidden;
    private String thumbnailPath;
    private Uri contentUri;
    private boolean isEncrypted;

    public HiddenContent() {
        // Constructor gol necesar pentru Gson
    }

    public HiddenContent(String originalPath, ContentType contentType, String name) {
        this.id = java.util.UUID.randomUUID().toString();
        this.originalPath = originalPath;
        this.contentType = contentType;
        this.name = name;
        this.dateHidden = System.currentTimeMillis();
        this.isEncrypted = false;
    }

    // Getters
    public String getId() { return id; }
    public String getOriginalPath() { return originalPath; }
    public String getHiddenPath() { return hiddenPath; }
    public ContentType getContentType() { return contentType; }
    public long getSize() { return size; }
    public String getMimeType() { return mimeType; }
    public String getName() { return name; }
    public long getDateHidden() { return dateHidden; }
    public String getThumbnailPath() { return thumbnailPath; }
    public Uri getContentUri() { return contentUri; }
    public boolean isEncrypted() { return isEncrypted; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setOriginalPath(String originalPath) { this.originalPath = originalPath; }
    public void setHiddenPath(String hiddenPath) { this.hiddenPath = hiddenPath; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }
    public void setSize(long size) { this.size = size; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public void setName(String name) { this.name = name; }
    public void setDateHidden(long dateHidden) { this.dateHidden = dateHidden; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }
    public void setContentUri(Uri contentUri) { this.contentUri = contentUri; }
    public void setEncrypted(boolean encrypted) { isEncrypted = encrypted; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HiddenContent that = (HiddenContent) o;
        return size == that.size &&
                dateHidden == that.dateHidden &&
                isEncrypted == that.isEncrypted &&
                Objects.equals(id, that.id) &&
                Objects.equals(originalPath, that.originalPath) &&
                Objects.equals(hiddenPath, that.hiddenPath) &&
                contentType == that.contentType &&
                Objects.equals(mimeType, that.mimeType) &&
                Objects.equals(name, that.name) &&
                Objects.equals(thumbnailPath, that.thumbnailPath) &&
                Objects.equals(contentUri, that.contentUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, originalPath, hiddenPath, contentType, size,
                mimeType, name, dateHidden, thumbnailPath, contentUri, isEncrypted);
    }

    @Override
    public String toString() {
        return "HiddenContent{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", contentType=" + contentType +
                ", size=" + size +
                ", dateHidden=" + dateHidden +
                ", isEncrypted=" + isEncrypted +
                '}';
    }
}
