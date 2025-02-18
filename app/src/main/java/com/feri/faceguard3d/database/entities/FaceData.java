package com.feri.faceguard3d.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.TypeConverters;

import com.feri.faceguard3d.database.converters.DateConverter;
import com.feri.faceguard3d.database.converters.FloatArrayConverter;

import java.util.Date;

@Entity(tableName = "face_data")
public class FaceData {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "embedding")
    @TypeConverters(FloatArrayConverter.class)
    private float[] embedding;

    @ColumnInfo(name = "landmarks")
    @TypeConverters(FloatArrayConverter.class)
    private float[] landmarks;

    @ColumnInfo(name = "pose_3d")
    @TypeConverters(FloatArrayConverter.class)
    private float[] pose3D;

    @ColumnInfo(name = "creation_date")
    @TypeConverters(DateConverter.class)
    private Date creationDate;

    @ColumnInfo(name = "last_update")
    @TypeConverters(DateConverter.class)
    private Date lastUpdate;

    @ColumnInfo(name = "confidence")
    private float confidence;

    @ColumnInfo(name = "lighting_condition")
    private float lightingCondition;

    @ColumnInfo(name = "has_glasses")
    private boolean hasGlasses;

    @ColumnInfo(name = "has_beard")
    private boolean hasBeard;

    @ColumnInfo(name = "quality_score")
    private float qualityScore;

    @ColumnInfo(name = "is_active")
    private boolean isActive;

    public FaceData() {
        this.creationDate = new Date();
        this.lastUpdate = new Date();
        this.isActive = true;
    }

    // Getters
    public long getId() {
        return id;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public float[] getLandmarks() {
        return landmarks;
    }

    public float[] getPose3D() {
        return pose3D;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public float getConfidence() {
        return confidence;
    }

    public float getLightingCondition() {
        return lightingCondition;
    }

    public boolean hasGlasses() {
        return hasGlasses;
    }

    public boolean hasBeard() {
        return hasBeard;
    }

    public float getQualityScore() {
        return qualityScore;
    }

    public boolean isActive() {
        return isActive;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
        this.lastUpdate = new Date();
    }

    public void setLandmarks(float[] landmarks) {
        this.landmarks = landmarks;
        this.lastUpdate = new Date();
    }

    public void setPose3D(float[] pose3D) {
        this.pose3D = pose3D;
        this.lastUpdate = new Date();
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
        this.lastUpdate = new Date();
    }

    public void setLightingCondition(float lightingCondition) {
        this.lightingCondition = lightingCondition;
        this.lastUpdate = new Date();
    }

    public void setHasGlasses(boolean hasGlasses) {
        this.hasGlasses = hasGlasses;
        this.lastUpdate = new Date();
    }

    public void setHasBeard(boolean hasBeard) {
        this.hasBeard = hasBeard;
        this.lastUpdate = new Date();
    }

    public void setQualityScore(float qualityScore) {
        this.qualityScore = qualityScore;
        this.lastUpdate = new Date();
    }

    public void setActive(boolean active) {
        this.isActive = active;
        this.lastUpdate = new Date();
    }

    // Metoda pentru verificarea calității datelor faciale
    public boolean isQualityAcceptable() {
        return qualityScore >= 0.7f && confidence >= 0.85f;
    }

    // Metoda pentru verificarea vârstei datelor
    public boolean isRecent(long maxAgeMillis) {
        return (System.currentTimeMillis() - lastUpdate.getTime()) <= maxAgeMillis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FaceData faceData = (FaceData) o;
        return id == faceData.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}