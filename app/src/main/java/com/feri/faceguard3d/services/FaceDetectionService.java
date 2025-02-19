package com.feri.faceguard3d.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;

import com.feri.faceguard3d.models.FacialFeatures;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.feri.faceguard3d.managers.ContentHidingManager;
import com.feri.faceguard3d.managers.SecuritySettingsManager;
import com.feri.faceguard3d.utils.ImageUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceDetectionService extends Service{

    private static final String TAG = "FaceDetectionService";
    private FaceDetector detector;
    private ExecutorService executorService;
    private ContentHidingManager contentHidingManager;
    private SecuritySettingsManager securityManager;
    private boolean isProcessing = false;
    private static boolean multipleFacesDetected = false;

    // Constante pentru validarea poziției feței
    private static final float MAX_EULER_Y = 36.0f; // Grade pentru rotația stânga-dreapta
    private static final float MAX_EULER_Z = 36.0f; // Grade pentru înclinare
    private static final float MAX_EULER_X = 36.0f; // Grade pentru înclinare sus-jos

    @Override
    public void onCreate() {
        super.onCreate();

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .enableTracking()
                .build();

        detector = FaceDetection.getClient(options);
        executorService = Executors.newSingleThreadExecutor();
        contentHidingManager = new ContentHidingManager(this);
        securityManager = SecuritySettingsManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void processImage(ImageProxy imageProxy) {
        if (isProcessing) return;
        isProcessing = true;

        Bitmap bitmap = ImageUtils.imageProxyToBitmap(imageProxy);
        if (bitmap == null) {
            isProcessing = false;
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    executorService.execute(() -> handleFaceDetectionResult(faces));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Face detection failed: " + e.getMessage());
                    isProcessing = false;
                })
                .addOnCompleteListener(task -> {
                    imageProxy.close();
                    bitmap.recycle();
                    isProcessing = false;
                });
    }

    private boolean isFacePositionValid(Face face) {
        if (face == null) return false;

        // Obține unghiurile Euler ale feței
        float rotY = Math.abs(face.getHeadEulerAngleY());  // Rotație stânga-dreapta
        float rotZ = Math.abs(face.getHeadEulerAngleZ());  // Înclinare
        float rotX = Math.abs(face.getHeadEulerAngleX());  // Sus-jos

        // Verifică dacă unghiurile sunt în limitele acceptabile
        boolean isYValid = rotY <= MAX_EULER_Y;
        boolean isZValid = rotZ <= MAX_EULER_Z;
        boolean isXValid = rotX <= MAX_EULER_X;

        // Verifică și probabilitatea de detecție
        // Folosim deschiderea ochilor și alte proprietăți pentru încredere
        float leftEyeOpen = face.getLeftEyeOpenProbability() != null ?
                face.getLeftEyeOpenProbability() : 0f;
        float rightEyeOpen = face.getRightEyeOpenProbability() != null ?
                face.getRightEyeOpenProbability() : 0f;

        boolean isConfident = (leftEyeOpen + rightEyeOpen) / 2.0f >= 0.85f;

        return isYValid && isZValid && isXValid && isConfident;
    }

    private void processFacialFeatures(Face face, float lightingCondition) {
        FacialFeatures features = new FacialFeatures();

        // Setează caracteristicile detectate
        features.setLightingCondition(lightingCondition);
        features.setConfidence(calculateFaceConfidence(face));
        features.setHasGlasses(false); // Se va implementa logic pentru detectarea ochelarilor
        features.setHasBeard(false);   // Se va implementa logic pentru detectarea bărbii

        // Setează poziția 3D
        float[] pose3D = new float[3];
        pose3D[0] = face.getHeadEulerAngleX();
        pose3D[1] = face.getHeadEulerAngleY();
        pose3D[2] = face.getHeadEulerAngleZ();
        features.setPose3D(pose3D);

        // Procesează reperele faciale (landmarks)
        if (face.getAllLandmarks() != null && !face.getAllLandmarks().isEmpty()) {
            float[] landmarks = new float[face.getAllLandmarks().size() * 2];
            for (int i = 0; i < face.getAllLandmarks().size(); i++) {
                landmarks[i * 2] = face.getAllLandmarks().get(i).getPosition().x;
                landmarks[i * 2 + 1] = face.getAllLandmarks().get(i).getPosition().y;
            }
            features.setLandmarks(landmarks);
        }

        // Verifică dacă această față corespunde cu cea înregistrată
        if (verifyFace(features)) {
            contentHidingManager.showProtectedContent();
        } else {
            contentHidingManager.hideProtectedContent();
        }
    }

    private boolean verifyFace(FacialFeatures detectedFeatures) {
        List<FacialFeatures> enrolledFeatures = securityManager.getFacialFeatures();
        if (enrolledFeatures.isEmpty()) return false;

        // Calculează similaritatea cu fețele înregistrate
        float maxSimilarity = 0f;
        for (FacialFeatures enrolled : enrolledFeatures) {
            float similarity = detectedFeatures.calculateSimilarity(enrolled);
            maxSimilarity = Math.max(maxSimilarity, similarity);
        }

        // Pragul de similaritate pentru autentificare
        return maxSimilarity >= 0.85f;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
        detector.close();
    }

    public static boolean areMultipleFacesDetected() {
        return multipleFacesDetected;
    }

    public static void setMultipleFacesDetected(boolean detected) {
        multipleFacesDetected = detected;
    }

    // Vom actualiza această variabilă în timpul procesării imaginii
    private void handleFaceDetectionResult(List<Face> faces) {
        setMultipleFacesDetected(faces.size() > 1);
        // restul codului existent
    }

    private float calculateFaceConfidence(Face face) {
        float eyeOpenScore = ((face.getLeftEyeOpenProbability() != null ?
                face.getLeftEyeOpenProbability() : 0f) +
                (face.getRightEyeOpenProbability() != null ?
                        face.getRightEyeOpenProbability() : 0f)) / 2.0f;

        return eyeOpenScore;
    }

}
