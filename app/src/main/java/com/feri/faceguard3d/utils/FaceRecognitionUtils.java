package com.feri.faceguard3d.utils;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;
import com.feri.faceguard3d.models.FacialFeatures;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class FaceRecognitionUtils {

    private static final String TAG = "FaceRecognitionUtils";

    // Constante pentru validarea feței
    private static final float MIN_FACE_SIZE = 0.15f;
    private static final float MIN_DETECTION_CONFIDENCE = 0.8f;
    private static final float MAX_EULER_Y = 36.0f;  // Rotație stânga-dreapta
    private static final float MAX_EULER_Z = 36.0f;  // Înclinare
    private static final float MAX_EULER_X = 36.0f;  // Sus-jos

    public static boolean isFaceDetectionValid(Face face) {
        if (face == null) return false;

        // Verifică dimensiunea feței
        // Convertim Rect la RectF
        Rect boundingBoxRect = face.getBoundingBox();
        RectF boundingBox = new RectF(boundingBoxRect);
        float faceSize = Math.min(boundingBox.width(), boundingBox.height());

        if (faceSize < MIN_FACE_SIZE) {
            Log.d(TAG, "Face too small: " + faceSize);
            return false;
        }

        // Verifică încrederea detecției
        // Înlocuim verificarea tracking confidence cu alte verificări
        float leftEyeOpen = face.getLeftEyeOpenProbability() != null ?
                face.getLeftEyeOpenProbability() : 0f;
        float rightEyeOpen = face.getRightEyeOpenProbability() != null ?
                face.getRightEyeOpenProbability() : 0f;
        float detectionConfidence = (leftEyeOpen + rightEyeOpen) / 2.0f;

        if (detectionConfidence < MIN_DETECTION_CONFIDENCE) {
            Log.d(TAG, "Low detection confidence: " + detectionConfidence);
            return false;
        }


        // Verifică unghiurile Euler
        if (Math.abs(face.getHeadEulerAngleY()) > MAX_EULER_Y ||
                Math.abs(face.getHeadEulerAngleZ()) > MAX_EULER_Z ||
                Math.abs(face.getHeadEulerAngleX()) > MAX_EULER_X) {
            Log.d(TAG, "Invalid head pose - Y: " + face.getHeadEulerAngleY() +
                    ", Z: " + face.getHeadEulerAngleZ() +
                    ", X: " + face.getHeadEulerAngleX());
            return false;
        }

        return true;
    }

    public static float[] extractFacialLandmarks(Face face) {
        List<PointF> landmarks = new ArrayList<>();

        // Adaugă punctele cheie faciale în ordine specifică
        addLandmarkPoint(landmarks, face, FaceLandmark.LEFT_EYE);
        addLandmarkPoint(landmarks, face, FaceLandmark.RIGHT_EYE);
        addLandmarkPoint(landmarks, face, FaceLandmark.NOSE_BASE);
        addLandmarkPoint(landmarks, face, FaceLandmark.MOUTH_LEFT);
        addLandmarkPoint(landmarks, face, FaceLandmark.MOUTH_RIGHT);
        addLandmarkPoint(landmarks, face, FaceLandmark.LEFT_EAR);
        addLandmarkPoint(landmarks, face, FaceLandmark.RIGHT_EAR);
        addLandmarkPoint(landmarks, face, FaceLandmark.LEFT_CHEEK);
        addLandmarkPoint(landmarks, face, FaceLandmark.RIGHT_CHEEK);

        // Convertește lista de puncte într-un array de float
        float[] landmarkArray = new float[landmarks.size() * 2];
        for (int i = 0; i < landmarks.size(); i++) {
            landmarkArray[i * 2] = landmarks.get(i).x;
            landmarkArray[i * 2 + 1] = landmarks.get(i).y;
        }

        return landmarkArray;
    }

    private static void addLandmarkPoint(List<PointF> landmarks, Face face,
                                         @FaceLandmark.LandmarkType int landmarkType) {
        FaceLandmark landmark = face.getLandmark(landmarkType);
        if (landmark != null) {
            landmarks.add(landmark.getPosition());
        }
    }

    public static Point[] convertLandmarksToPoints(float[] landmarks) {
        Point[] points = new Point[landmarks.length / 2];
        for (int i = 0; i < points.length; i++) {
            points[i] = new Point(landmarks[i * 2], landmarks[i * 2 + 1]);
        }
        return points;
    }

    public static float calculateFaceSimilarity(FacialFeatures face1, FacialFeatures face2) {
        if (face1 == null || face2 == null ||
                face1.getEmbedding() == null || face2.getEmbedding() == null) {
            return 0.0f;
        }

        float[] embedding1 = face1.getEmbedding();
        float[] embedding2 = face2.getEmbedding();

        if (embedding1.length != embedding2.length) {
            return 0.0f;
        }

        // Calculează similaritatea cosinus
        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;

        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }

        norm1 = (float) Math.sqrt(norm1);
        norm2 = (float) Math.sqrt(norm2);

        if (norm1 == 0 || norm2 == 0) return 0.0f;

        return dotProduct / (norm1 * norm2);
    }

    public static float calculatePoseSimilarity(Face face1, Face face2) {
        float[] pose1 = new float[] {
                face1.getHeadEulerAngleX(),
                face1.getHeadEulerAngleY(),
                face1.getHeadEulerAngleZ()
        };

        float[] pose2 = new float[] {
                face2.getHeadEulerAngleX(),
                face2.getHeadEulerAngleY(),
                face2.getHeadEulerAngleZ()
        };

        float diffSum = 0;
        for (int i = 0; i < 3; i++) {
            float diff = Math.abs(pose1[i] - pose2[i]);
            diffSum += diff;
        }

        // Normalizează diferența (0 = identic, 1 = complet diferit)
        return Math.max(0, 1 - (diffSum / (3 * 180)));
    }

    public static boolean isLivenessPassed(Face face, Bitmap image) {
        // Verifică expresia facială
        if (face.getSmilingProbability() != null &&
                face.getSmilingProbability() > 0.9f) {
            return false; // Expresie prea exagerată
        }

        // Verifică deschiderea ochilor
        if ((face.getLeftEyeOpenProbability() != null &&
                face.getLeftEyeOpenProbability() < 0.5f) ||
                (face.getRightEyeOpenProbability() != null &&
                        face.getRightEyeOpenProbability() < 0.5f)) {
            return false; // Ochi închiși
        }

        // Verifică blurarea
        Mat mat = new Mat();
        Utils.bitmapToMat(image, mat);
        float blurriness = ImageNormalizer.calculateBlurriness(mat);
        mat.release();

        if (blurriness < 100) { // Imagine prea blurată
            return false;
        }

        return true;
    }

    public static MatOfPoint2f getFacialLandmarksMatrix(Face face) {
        List<Point> points = new ArrayList<>();

        // Adaugă toate reperele faciale disponibile
        for (FaceLandmark landmark : face.getAllLandmarks()) {
            points.add(new Point(landmark.getPosition().x, landmark.getPosition().y));
        }

        MatOfPoint2f landmarks = new MatOfPoint2f();
        landmarks.fromList(points);
        return landmarks;
    }
}
