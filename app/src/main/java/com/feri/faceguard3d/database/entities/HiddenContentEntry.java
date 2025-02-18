package com.feri.faceguard3d.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.TypeConverters;

import com.feri.faceguard3d.database.converters.DateConverter;
import com.feri.faceguard3d.models.HiddenContent;

import java.util.Date;

@Entity(tableName = "hidden_content")
public class HiddenContentEntry {

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "original_path")
    private String originalPath;

    @ColumnInfo(name = "hidden_path")
    private String hiddenPath;

    @ColumnInfo(name = "content_type")
    private HiddenContent.ContentType contentType;

    @ColumnInfo(name = "size")
    private long size;

    @ColumnInfo(name = "mime_type")
    private String mimeType;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "date_hidden")
    @TypeConverters(DateConverter.class)
    private Date dateHidden;

    @ColumnInfo(name = "thumbnail_path")
    private String thumbnailPath;

    @ColumnInfo(name = "is_encrypted")
    private boolean isEncrypted;

    @ColumnInfo(name = "encryption_key_id")
    private String encryptionKeyId;

    @ColumnInfo(name = "last_access")
    @TypeConverters(DateConverter.class)
    private Date lastAccess;

    @ColumnInfo(name = "hash")
    private String hash;

    public HiddenContentEntry() {
        this.dateHidden = new Date();
        this.lastAccess = new Date();
    }

    // Constructor pentru conversia de la modelul HiddenContent
    public HiddenContentEntry(HiddenContent content) {
        this.id = content.getId();
        this.originalPath = content.getOriginalPath();
        this.hiddenPath = content.getHiddenPath();
        this.contentType = content.getContentType();
        this.size = content.getSize();
        this.mimeType = content.getMimeType();
        this.name = content.getName();
        this.dateHidden = new Date(content.getDateHidden());
        this.thumbnailPath = content.getThumbnailPath();
        this.isEncrypted = content.isEncrypted();
        this.lastAccess = new Date();
    }

    // Getters
    public String getId() { return id; }
    public String getOriginalPath() { return originalPath; }
    public String getHiddenPath() { return hiddenPath; }
    public HiddenContent.ContentType getContentType() { return contentType; }
    public long getSize() { return size; }
    public String getMimeType() { return mimeType; }
    public String getName() { return name; }
    public Date getDateHidden() { return dateHidden; }
    public String getThumbnailPath() { return thumbnailPath; }
    public boolean isEncrypted() { return isEncrypted; }
    public String getEncryptionKeyId() { return encryptionKeyId; }
    public Date getLastAccess() { return lastAccess; }
    public String getHash() { return hash; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setOriginalPath(String originalPath) { this.originalPath = originalPath; }
    public void setHiddenPath(String hiddenPath) { this.hiddenPath = hiddenPath; }
    public void setContentType(HiddenContent.ContentType contentType) {
        this.contentType = contentType;
    }
    public void setSize(long size) { this.size = size; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public void setName(String name) { this.name = name; }
    public void setDateHidden(Date dateHidden) { this.dateHidden = dateHidden; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }
    public void setEncrypted(boolean encrypted) { this.isEncrypted = encrypted; }
    public void setEncryptionKeyId(String encryptionKeyId) {
        this.encryptionKeyId = encryptionKeyId;
    }
    public void setLastAccess(Date lastAccess) { this.lastAccess = lastAccess; }
    public void setHash(String hash) { this.hash = hash; }

    // Conversie înapoi la modelul HiddenContent
    public HiddenContent toHiddenContent() {
        HiddenContent content = new HiddenContent(originalPath, contentType, name);
        content.setId(id);
        content.setHiddenPath(hiddenPath);
        content.setSize(size);
        content.setMimeType(mimeType);
        content.setDateHidden(dateHidden.getTime());
        content.setThumbnailPath(thumbnailPath);
        content.setEncrypted(isEncrypted);
        return content;
    }

    // Metodă pentru actualizarea timpului de acces
    public void updateLastAccess() {
        this.lastAccess = new Date();
    }

    // Metodă pentru verificarea integrității
    public boolean verifyIntegrity(String calculatedHash) {
        return hash != null && hash.equals(calculatedHash);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HiddenContentEntry that = (HiddenContentEntry) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}