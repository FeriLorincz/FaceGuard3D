package com.feri.faceguard3d.services;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;
import android.app.Service;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.feri.faceguard3d.managers.SecuritySettingsManager;
import com.feri.faceguard3d.models.FacialFeatures;
import com.feri.faceguard3d.utils.ImageUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceAuthService extends Service{

    private static final String TAG = "FaceAuthService";
    private FaceDetector detector;
    private SecuritySettingsManager securityManager;
    private ExecutorService executor;
    private boolean isProcessing = false;
    private static final float SIMILARITY_THRESHOLD = 0.85f;

    public interface AuthenticationCallback {
        void onAuthenticationSuccess(FacialFeatures features);
        void onAuthenticationFailure(String reason);
        void onMultipleFacesDetected();
        void onError(String error);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Configurare detector ML Kit pentru detecție facială precisă
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .enableTracking()
                .build();

        detector = FaceDetection.getClient(options);
        securityManager = SecuritySettingsManager.getInstance(this);
        executor = Executors.newSingleThreadExecutor();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void authenticate(ImageProxy image, AuthenticationCallback callback) {
        if (isProcessing) {
            image.close();
            return;
        }

        isProcessing = true;

        try {
            InputImage inputImage = ImageUtils.imageProxyToInputImage(image);
            if (inputImage == null) {
                isProcessing = false;
                callback.onError("Failed to process image");
                image.close();
                return;
            }

            detector.process(inputImage)
                    .addOnSuccessListener(faces -> {
                        executor.execute(() -> processFaces(faces, image, callback));
                    })
                    .addOnFailureListener(e -> {
                        isProcessing = false;
                        callback.onError("Face detection failed: " + e.getMessage());
                        image.close();
                    })
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            isProcessing = false;
                            image.close();
                        }
                    });
        } catch (Exception e) {
            isProcessing = false;
            callback.onError("Error processing image: " + e.getMessage());
            image.close();
        }
    }

    private void processFaces(List<Face> faces, ImageProxy image,
                              AuthenticationCallback callback) {
        try {
            if (faces.isEmpty()) {
                isProcessing = false;
                callback.onAuthenticationFailure("No face detected");
                return;
            }

            if (faces.size() > 1) {
                isProcessing = false;
                callback.onMultipleFacesDetected();
                return;
            }

            Face detectedFace = faces.get(0);

            // Verifică poziția feței
            if (!isValidFacePose(detectedFace)) {
                isProcessing = false;
                callback.onAuthenticationFailure("Invalid face position");
                return;
            }

            // Procesează și verifică caracteristicile faciale
            FacialFeatures detectedFeatures = extractFacialFeatures(detectedFace, image);
            if (detectedFeatures == null) {
                isProcessing = false;
                callback.onError("Failed to extract facial features");
                return;
            }

            // Verifică autenticitatea
            if (verifyFaceAuthenticity(detectedFeatures)) {
                callback.onAuthenticationSuccess(detectedFeatures);
            } else {
                callback.onAuthenticationFailure("Face not recognized");
            }
        } finally {
            isProcessing = false;
            image.close();
        }
    }

    private boolean isValidFacePose(Face face) {
        float rotY = Math.abs(face.getHeadEulerAngleY());  // Rotație stânga-dreapta
        float rotZ = Math.abs(face.getHeadEulerAngleZ());  // Înclinare
        float rotX = Math.abs(face.getHeadEulerAngleX());  // Sus-jos

        // Verificăm și deschiderea ochilor ca indicator suplimentar
        float leftEyeOpen = face.getLeftEyeOpenProbability() != null ?
                face.getLeftEyeOpenProbability() : 0f;
        float rightEyeOpen = face.getRightEyeOpenProbability() != null ?
                face.getRightEyeOpenProbability() : 0f;

        // Limitele acceptabile pentru unghiurile Euler și deschiderea ochilor
        return rotY <= 36.0f &&
                rotZ <= 36.0f &&
                rotX <= 36.0f &&
                leftEyeOpen >= 0.5f &&
                rightEyeOpen >= 0.5f;
    }

    private FacialFeatures extractFacialFeatures(Face face, ImageProxy image) {
        try {
            Bitmap bitmap = ImageUtils.imageProxyToBitmap(image);
            if (bitmap == null) return null;

            // Decupează și aliniază fața
            Bitmap alignedFace = ImageUtils.cropAndAlignFace(bitmap, face, 160);
            if (alignedFace == null) return null;

            // Calculează condițiile de iluminare
            float lightingCondition = ImageUtils.calculateBrightness(alignedFace);

            // Creează obiectul FacialFeatures
            FacialFeatures features = new FacialFeatures();
            // Calculăm un scor de încredere bazat pe mai mulți factori
            float confidence = calculateConfidenceScore(face);
            features.setConfidence(confidence);
            features.setLightingCondition(lightingCondition);

            // Setează poziția 3D
            float[] pose3D = new float[3];
            pose3D[0] = face.getHeadEulerAngleX();
            pose3D[1] = face.getHeadEulerAngleY();
            pose3D[2] = face.getHeadEulerAngleZ();
            features.setPose3D(pose3D);

            // Procesează reperele faciale
            if (face.getAllLandmarks() != null && !face.getAllLandmarks().isEmpty()) {
                float[] landmarks = new float[face.getAllLandmarks().size() * 2];
                for (int i = 0; i < face.getAllLandmarks().size(); i++) {
                    landmarks[i * 2] = face.getAllLandmarks().get(i).getPosition().x;
                    landmarks[i * 2 + 1] = face.getAllLandmarks().get(i).getPosition().y;
                }
                features.setLandmarks(landmarks);
            }

            // Detectează atribute faciale
            features.setHasGlasses(face.getRightEyeOpenProbability() < 0.8f);

            alignedFace.recycle();
            bitmap.recycle();

            return features;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting facial features", e);
            return null;
        }
    }

    private boolean verifyFaceAuthenticity(FacialFeatures detectedFeatures) {
        List<FacialFeatures> enrolledFeatures = securityManager.getFacialFeatures();
        if (enrolledFeatures.isEmpty()) return false;

        float maxSimilarity = 0f;
        for (FacialFeatures enrolled : enrolledFeatures) {
            float similarity = detectedFeatures.calculateSimilarity(enrolled);
            maxSimilarity = Math.max(maxSimilarity, similarity);

            // Verifică și poziția 3D pentru a preveni folosirea unei fotografii
            if (similarity >= SIMILARITY_THRESHOLD) {
                if (!verifyLivenessFeatures(detectedFeatures, enrolled)) {
                    return false;
                }
            }
        }

        return maxSimilarity >= SIMILARITY_THRESHOLD;
    }

    private boolean verifyLivenessFeatures(FacialFeatures detected, FacialFeatures enrolled) {
        // Verifică mișcarea naturală a capului
        float[] detectedPose = detected.getPose3D();
        float[] enrolledPose = enrolled.getPose3D();

        // Calculează diferența de poziție
        float poseDifference = 0;
        for (int i = 0; i < 3; i++) {
            poseDifference += Math.abs(detectedPose[i] - enrolledPose[i]);
        }

        // Verifică condițiile de iluminare
        float lightingDifference = Math.abs(
                detected.getLightingCondition() - enrolled.getLightingCondition());

        // Permite o variație naturală în poziție și iluminare
        boolean isNaturalMovement = poseDifference > 5.0f && poseDifference < 45.0f;
        boolean isAcceptableLighting = lightingDifference < 0.3f;

        return isNaturalMovement && isAcceptableLighting;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Face authentication service started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        detector.close();

        // Repornește serviciul dacă a fost oprit
        Intent restartService = new Intent(this, FaceAuthService.class);
        startService(restartService);
    }

    public static boolean isRunning(Context context) {
        ActivityManager manager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (FaceAuthService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private float calculateConfidenceScore(Face face) {
        float leftEyeOpen = face.getLeftEyeOpenProbability() != null ?
                face.getLeftEyeOpenProbability() : 0f;
        float rightEyeOpen = face.getRightEyeOpenProbability() != null ?
                face.getRightEyeOpenProbability() : 0f;

        // Calculăm un scor compus
        return (leftEyeOpen + rightEyeOpen) / 2.0f;
    }
}