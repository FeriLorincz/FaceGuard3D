package com.feri.faceguard3d.models;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FacialFeatures {

    private float[] embedding;
    private float[] landmarks;
    private String timestamp;
    private float lightingCondition;
    private List<String> facialAttributes;
    private float[] pose3D;
    private boolean hasGlasses;
    private boolean hasBeard;
    private float confidence;

    public FacialFeatures() {
        // Constructor gol necesar pentru Gson
    }

    public FacialFeatures(float[] embedding, float[] landmarks, String timestamp,
                          float lightingCondition, List<String> facialAttributes,
                          float[] pose3D, boolean hasGlasses, boolean hasBeard,
                          float confidence) {
        this.embedding = embedding;
        this.landmarks = landmarks;
        this.timestamp = timestamp;
        this.lightingCondition = lightingCondition;
        this.facialAttributes = facialAttributes;
        this.pose3D = pose3D;
        this.hasGlasses = hasGlasses;
        this.hasBeard = hasBeard;
        this.confidence = confidence;
    }

    // Getters and Setters de mai sus rămân la fel

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FacialFeatures that = (FacialFeatures) o;
        return Float.compare(that.lightingCondition, lightingCondition) == 0 &&
                hasGlasses == that.hasGlasses &&
                hasBeard == that.hasBeard &&
                Float.compare(that.confidence, confidence) == 0 &&
                Arrays.equals(embedding, that.embedding) &&
                Arrays.equals(landmarks, that.landmarks) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(facialAttributes, that.facialAttributes) &&
                Arrays.equals(pose3D, that.pose3D);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(timestamp, lightingCondition, facialAttributes,
                hasGlasses, hasBeard, confidence);
        result = 31 * result + Arrays.hashCode(embedding);
        result = 31 * result + Arrays.hashCode(landmarks);
        result = 31 * result + Arrays.hashCode(pose3D);
        return result;
    }

    @Override
    public String toString() {
        return "FacialFeatures{" +
                "embedding=" + Arrays.toString(embedding) +
                ", landmarks=" + Arrays.toString(landmarks) +
                ", timestamp='" + timestamp + '\'' +
                ", lightingCondition=" + lightingCondition +
                ", facialAttributes=" + facialAttributes +
                ", pose3D=" + Arrays.toString(pose3D) +
                ", hasGlasses=" + hasGlasses +
                ", hasBeard=" + hasBeard +
                ", confidence=" + confidence +
                '}';
    }

    public float calculateSimilarity(FacialFeatures other) {
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

    public boolean isValidPose() {
        if (pose3D == null || pose3D.length != 3) return false;

        // Verifică dacă rotația feței este în limitele acceptabile
        // Valorile sunt în radiani
        float maxRotation = 0.5f; // aproximativ 30 grade
        return Math.abs(pose3D[0]) < maxRotation && // pitch
                Math.abs(pose3D[1]) < maxRotation && // yaw
                Math.abs(pose3D[2]) < maxRotation;   // roll
    }

    public boolean isGoodLightingCondition() {
        // Valoarea lightingCondition este normalizată între 0 și 1
        return lightingCondition >= 0.4f && lightingCondition <= 0.9f;
    }


    // Getteri
    public float[] getEmbedding() {
        return embedding;
    }

    public float[] getLandmarks() {
        return landmarks;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public float getLightingCondition() {
        return lightingCondition;
    }

    public List<String> getFacialAttributes() {
        return facialAttributes;
    }

    public float[] getPose3D() {
        return pose3D;
    }

    public boolean hasGlasses() {
        return hasGlasses;
    }

    public boolean hasBeard() {
        return hasBeard;
    }

    public float getConfidence() {
        return confidence;
    }

    // Setteri
    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public void setLandmarks(float[] landmarks) {
        this.landmarks = landmarks;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setLightingCondition(float lightingCondition) {
        this.lightingCondition = lightingCondition;
    }

    public void setFacialAttributes(List<String> facialAttributes) {
        this.facialAttributes = facialAttributes;
    }

    public void setPose3D(float[] pose3D) {
        this.pose3D = pose3D;
    }

    public void setHasGlasses(boolean hasGlasses) {
        this.hasGlasses = hasGlasses;
    }

    public void setHasBeard(boolean hasBeard) {
        this.hasBeard = hasBeard;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
}
