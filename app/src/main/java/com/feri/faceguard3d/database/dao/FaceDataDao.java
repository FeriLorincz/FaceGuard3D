package com.feri.faceguard3d.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Transaction;

import com.feri.faceguard3d.database.entities.FaceData;

import java.util.Date;
import java.util.List;

@Dao
public interface FaceDataDao {

    @Insert
    long insert(FaceData faceData);

    @Update
    void update(FaceData faceData);

    @Delete
    void delete(FaceData faceData);

    @Query("SELECT * FROM face_data WHERE id = :id")
    FaceData getFaceDataById(long id);

    @Query("SELECT * FROM face_data WHERE is_active = 1 ORDER BY creation_date DESC")
    List<FaceData> getAllActiveFaceData();

    @Query("SELECT * FROM face_data WHERE is_active = 1 AND quality_score >= :minQuality " +
            "ORDER BY quality_score DESC")
    List<FaceData> getHighQualityFaceData(float minQuality);

    @Query("SELECT * FROM face_data WHERE is_active = 1 AND last_update >= :date")
    List<FaceData> getRecentFaceData(Date date);

    @Query("SELECT COUNT(*) FROM face_data WHERE is_active = 1")
    int getActiveFaceDataCount();

    @Query("UPDATE face_data SET is_active = 0 WHERE id = :id")
    void deactivateFaceData(long id);

    @Transaction
    @Query("UPDATE face_data SET is_active = 0 WHERE last_update < :date")
    void deactivateOldFaceData(Date date);

    @Query("DELETE FROM face_data WHERE is_active = 0")
    void deleteInactiveFaceData();

    @Query("DELETE FROM face_data")
    void deleteAll();

    @Transaction
    @Query("SELECT * FROM face_data WHERE is_active = 1 AND " +
            "quality_score >= :minQuality AND confidence >= :minConfidence " +
            "ORDER BY quality_score DESC LIMIT :limit")
    List<FaceData> getBestFaceData(float minQuality, float minConfidence, int limit);

    @Query("SELECT * FROM face_data WHERE has_glasses = :hasGlasses AND is_active = 1")
    List<FaceData> getFaceDataWithGlasses(boolean hasGlasses);

    @Query("SELECT * FROM face_data WHERE has_beard = :hasBeard AND is_active = 1")
    List<FaceData> getFaceDataWithBeard(boolean hasBeard);

    @Transaction
    @Query("SELECT * FROM face_data WHERE is_active = 1 " +
            "ORDER BY last_update DESC LIMIT 1")
    FaceData getMostRecentFaceData();

    @Query("SELECT AVG(quality_score) FROM face_data WHERE is_active = 1")
    float getAverageQualityScore();

    @Transaction
    @Query("SELECT * FROM face_data WHERE is_active = 1 AND " +
            "lighting_condition BETWEEN :minLighting AND :maxLighting")
    List<FaceData> getFaceDataByLightingCondition(float minLighting, float maxLighting);

    @Transaction
    public default void replaceFaceData(FaceData newFaceData) {
        deleteAll();
        insert(newFaceData);
    }

    @Transaction
    public default void updateFaceDataSet(List<FaceData> newFaceDataSet) {
        deleteAll();
        for (FaceData faceData : newFaceDataSet) {
            insert(faceData);
        }
    }

    @Query("SELECT COUNT(*) FROM face_data WHERE is_active = 1 AND " +
            "quality_score >= :threshold")
    int getHighQualityFaceDataCount(float threshold);

    @Transaction
    public default void cleanupOldData(Date cutoffDate, int maxEntries) {
        // Dezactivează datele vechi
        deactivateOldFaceData(cutoffDate);

        // Obține numărul total de înregistrări active
        int activeCount = getActiveFaceDataCount();

        if (activeCount > maxEntries) {
            // Obține datele sortate după calitate
            List<FaceData> allData = getAllActiveFaceData();

            // Dezactivează datele de calitate inferioară care depășesc maxEntries
            for (int i = maxEntries; i < allData.size(); i++) {
                deactivateFaceData(allData.get(i).getId());
            }
        }

        // Șterge toate datele inactive
        deleteInactiveFaceData();
    }
}