package com.feri.faceguard3d.models;

import java.util.Arrays;
import java.util.Date;

public class FaceEmbedding {

    private float[] embedding;
    private float[] landmarks;
    private long timestamp;
    private String userId;
    private float confidence;
    private float[] pose;
    private float lightingCondition;
    private boolean isEnrolled;
    private String modelVersion;
    private float[] qualityScores;

    public FaceEmbedding() {
        this.timestamp = new Date().getTime();
        this.isEnrolled = false;
    }

    public FaceEmbedding(float[] embedding, float[] landmarks, String userId) {
        this();
        this.embedding = embedding;
        this.landmarks = landmarks;
        this.userId = userId;
    }

    // Getters
    public float[] getEmbedding() {
        return embedding;
    }

    public float[] getLandmarks() {
        return landmarks;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public float getConfidence() {
        return confidence;
    }

    public float[] getPose() {
        return pose;
    }

    public float getLightingCondition() {
        return lightingCondition;
    }

    public boolean isEnrolled() {
        return isEnrolled;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public float[] getQualityScores() {
        return qualityScores;
    }

    // Setters
    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public void setLandmarks(float[] landmarks) {
        this.landmarks = landmarks;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public void setPose(float[] pose) {
        this.pose = pose;
    }

    public void setLightingCondition(float lightingCondition) {
        this.lightingCondition = lightingCondition;
    }

    public void setEnrolled(boolean enrolled) {
        this.isEnrolled = enrolled;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public void setQualityScores(float[] qualityScores) {
        this.qualityScores = qualityScores;
    }

    // Metodă pentru calcularea similarității cu alt embedding
    public float calculateSimilarity(FaceEmbedding other) {
        if (this.embedding == null || other.embedding == null ||
                this.embedding.length != other.embedding.length) {
            return 0.0f;
        }

        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;

        for (int i = 0; i < this.embedding.length; i++) {
            dotProduct += this.embedding[i] * other.embedding[i];
            normA += this.embedding[i] * this.embedding[i];
            normB += other.embedding[i] * other.embedding[i];
        }

        normA = (float) Math.sqrt(normA);
        normB = (float) Math.sqrt(normB);

        if (normA == 0 || normB == 0) return 0.0f;

        return dotProduct / (normA * normB);
    }

    // Metodă pentru verificarea calității embedding-ului
    public boolean isQualityAcceptable() {
        if (qualityScores == null || qualityScores.length < 3) {
            return false;
        }

        // Verifică scorurile minime pentru diferite aspecte
        float minBlurScore = 0.3f;
        float minLightingScore = 0.4f;
        float minPoseScore = 0.5f;

        return qualityScores[0] >= minBlurScore &&    // Blur score
                qualityScores[1] >= minLightingScore && // Lighting score
                qualityScores[2] >= minPoseScore;       // Pose score
    }

    // Metodă pentru verificarea vârstei embedding-ului
    public boolean isRecent(long maxAgeMillis) {
        return (System.currentTimeMillis() - timestamp) <= maxAgeMillis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FaceEmbedding that = (FaceEmbedding) o;
        return timestamp == that.timestamp &&
                Float.compare(that.confidence, confidence) == 0 &&
                Float.compare(that.lightingCondition, lightingCondition) == 0 &&
                isEnrolled == that.isEnrolled &&
                Arrays.equals(embedding, that.embedding) &&
                Arrays.equals(landmarks, that.landmarks) &&
                Arrays.equals(pose, that.pose) &&
                Arrays.equals(qualityScores, that.qualityScores) &&
                userId.equals(that.userId) &&
                modelVersion.equals(that.modelVersion);
    }

    @Override
    public int hashCode() {
        int result = userId.hashCode();
        result = 31 * result + Arrays.hashCode(embedding);
        result = 31 * result + Arrays.hashCode(landmarks);
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "FaceEmbedding{" +
                "userId='" + userId + '\'' +
                ", timestamp=" + timestamp +
                ", confidence=" + confidence +
                ", lightingCondition=" + lightingCondition +
                ", isEnrolled=" + isEnrolled +
                ", modelVersion='" + modelVersion + '\'' +
                '}';
    }
}
