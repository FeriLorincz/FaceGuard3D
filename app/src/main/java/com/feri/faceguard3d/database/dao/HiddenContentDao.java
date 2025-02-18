package com.feri.faceguard3d.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.feri.faceguard3d.database.entities.HiddenContentEntry;
import com.feri.faceguard3d.models.HiddenContent.ContentType;

import java.util.Date;
import java.util.List;

@Dao
public interface HiddenContentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(HiddenContentEntry content);

    @Update
    void update(HiddenContentEntry content);

    @Delete
    void delete(HiddenContentEntry content);

    @Query("SELECT * FROM hidden_content WHERE id = :id")
    HiddenContentEntry getById(String id);

    @Query("SELECT * FROM hidden_content")
    List<HiddenContentEntry> getAll();

    @Query("SELECT * FROM hidden_content WHERE content_type = :type")
    List<HiddenContentEntry> getAllByType(ContentType type);

    @Query("SELECT * FROM hidden_content WHERE is_encrypted = 1")
    List<HiddenContentEntry> getEncryptedContent();

    @Query("SELECT * FROM hidden_content WHERE original_path = :path")
    HiddenContentEntry getByOriginalPath(String path);

    @Query("DELETE FROM hidden_content WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM hidden_content")
    void deleteAll();

    @Transaction
    @Query("SELECT * FROM hidden_content WHERE date_hidden >= :date")
    List<HiddenContentEntry> getRecentContent(Date date);

    @Query("SELECT COUNT(*) FROM hidden_content")
    int getCount();

    @Query("SELECT SUM(size) FROM hidden_content")
    long getTotalSize();

    @Query("SELECT * FROM hidden_content ORDER BY date_hidden DESC LIMIT :limit")
    List<HiddenContentEntry> getRecentlyHidden(int limit);

    @Query("SELECT * FROM hidden_content ORDER BY last_access DESC LIMIT :limit")
    List<HiddenContentEntry> getRecentlyAccessed(int limit);

    @Query("UPDATE hidden_content SET last_access = :accessDate WHERE id = :id")
    void updateLastAccess(String id, Date accessDate);

    @Query("SELECT * FROM hidden_content WHERE name LIKE '%' || :query || '%'")
    List<HiddenContentEntry> searchByName(String query);

    @Transaction
    @Query("SELECT * FROM hidden_content WHERE mime_type LIKE :mimeTypePattern")
    List<HiddenContentEntry> getByMimeType(String mimeTypePattern);

    @Transaction
    public default void addOrUpdate(HiddenContentEntry content) {
        HiddenContentEntry existing = getById(content.getId());
        if (existing == null) {
            insert(content);
        } else {
            update(content);
        }
    }

    @Transaction
    public default void updateContentStatus(String id, boolean isEncrypted,
                                            String hiddenPath, String encryptionKeyId) {
        HiddenContentEntry content = getById(id);
        if (content != null) {
            content.setEncrypted(isEncrypted);
            content.setHiddenPath(hiddenPath);
            content.setEncryptionKeyId(encryptionKeyId);
            content.updateLastAccess();
            update(content);
        }
    }

    @Query("SELECT * FROM hidden_content WHERE " +
            "last_access < :cutoffDate AND is_encrypted = 1")
    List<HiddenContentEntry> getStaleEncryptedContent(Date cutoffDate);

    @Transaction
    public default void cleanupStaleContent(Date cutoffDate) {
        List<HiddenContentEntry> staleContent = getStaleEncryptedContent(cutoffDate);
        for (HiddenContentEntry content : staleContent) {
            delete(content);
        }
    }

    @Transaction
    @Query("SELECT COUNT(*) FROM hidden_content " +
            "WHERE content_type = :type AND is_encrypted = 1")
    int getEncryptedContentCountByType(ContentType type);

    @Query("UPDATE hidden_content SET hash = :hash WHERE id = :id")
    void updateHash(String id, String hash);

    @Transaction
    public default void moveContent(String id, String newHiddenPath) {
        HiddenContentEntry content = getById(id);
        if (content != null) {
            content.setHiddenPath(newHiddenPath);
            content.updateLastAccess();
            update(content);
        }
    }

    @Transaction
    @Query("SELECT * FROM hidden_content WHERE " +
            "is_encrypted = 1 AND encryption_key_id = :keyId")
    List<HiddenContentEntry> getContentByEncryptionKey(String keyId);
}