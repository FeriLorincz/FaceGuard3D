package com.feri.faceguard3d.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.feri.faceguard3d.models.FacialFeatures;
import com.feri.faceguard3d.utils.ImageUtils;

import org.tensorflow.lite.Interpreter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MultiFaceEnrollmentManager {

    private static final String TAG = "MultiFaceEnrollment";
    private final Context context;
    private final FaceDetector detector;
    private final Interpreter faceNetInterpreter;
    private final Executor executor;
    private final int EMBEDDING_SIZE = 512;
    private final int IMAGE_SIZE = 160;

    public interface FaceAnalysisCallback {
        void onFaceDetected(FacialFeatures features);
        void onError(String error);
    }

    public interface EnrollmentCallback {
        void onFeatureExtracted(FacialFeatures features);
        void onError(String error);
    }

    public MultiFaceEnrollmentManager(Context context) {
        this.context = context;

        // Configurare detector ML Kit
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .enableTracking()
                .build();

        this.detector = FaceDetection.getClient(options);

        // Inițializare model FaceNet
        try {
            this.faceNetInterpreter = new Interpreter(
                    ImageUtils.loadModelFile(context, "facenet_model.tflite"));
        } catch (IOException e) {
            throw new RuntimeException("Error loading FaceNet model", e);
        }

        this.executor = Executors.newSingleThreadExecutor();
    }

    public void analyzeFacePosition(ImageProxy image, FaceAnalysisCallback callback) {
        InputImage inputImage = ImageUtils.imageProxyToInputImage(image);
        if (inputImage == null) {
            callback.onError("Failed to process image");
            return;
        }

        detector.process(inputImage)
                .addOnSuccessListener(faces -> {
                    if (faces.isEmpty()) {
                        callback.onError("No face detected");
                        return;
                    }
                    if (faces.size() > 1) {
                        callback.onError("Multiple faces detected");
                        return;
                    }

                    Face face = faces.get(0);
                    FacialFeatures features = extractFacialFeatures(face, image);
                    callback.onFaceDetected(features);
                })
                .addOnFailureListener(e -> callback.onError("Face detection failed: " + e.getMessage()))
                .addOnCompleteListener(task -> image.close());
    }

    public void processCapturedImage(ImageProxy image, EnrollmentCallback callback) {
        executor.execute(() -> {
            try {
                InputImage inputImage = ImageUtils.imageProxyToInputImage(image);
                if (inputImage == null) {
                    callback.onError("Failed to process image");
                    return;
                }

                detector.process(inputImage)
                        .addOnSuccessListener(faces -> {
                            if (faces.isEmpty()) {
                                callback.onError("No face detected");
                                return;
                            }
                            if (faces.size() > 1) {
                                callback.onError("Multiple faces detected");
                                return;
                            }

                            Face face = faces.get(0);
                            processFaceForEnrollment(face, image, callback);
                        })
                        .addOnFailureListener(e -> callback.onError("Face detection failed: " + e.getMessage()));
            } finally {
                image.close();
            }
        });
    }

    private void processFaceForEnrollment(Face face, ImageProxy image, EnrollmentCallback callback) {
        Bitmap originalBitmap = ImageUtils.imageProxyToBitmap(image);
        if (originalBitmap == null) {
            callback.onError("Failed to convert image");
            return;
        }

        try {
            // Crop and align face
            Bitmap alignedFace = ImageUtils.cropAndAlignFace(originalBitmap, face, IMAGE_SIZE);
            if (alignedFace == null) {
                callback.onError("Failed to align face");
                return;
            }

            // Extract embedding
            float[] embedding = extractEmbedding(alignedFace);

            // Create facial features object
            FacialFeatures features = new FacialFeatures();
            features.setEmbedding(embedding);
            features.setTimestamp(String.valueOf(System.currentTimeMillis()));

            // Set pose information
            float[] pose3D = new float[3];
            pose3D[0] = face.getHeadEulerAngleX();
            pose3D[1] = face.getHeadEulerAngleY();
            pose3D[2] = face.getHeadEulerAngleZ();
            features.setPose3D(pose3D);

            // Set confidence
            features.setConfidence(face.getSmilingProbability() != null ? face.getSmilingProbability() : 0.0f);

            // Calculate lighting condition
            float lightingCondition = calculateLightingCondition(alignedFace);
            features.setLightingCondition(lightingCondition);

            callback.onFeatureExtracted(features);

            alignedFace.recycle();
        } catch (Exception e) {
            callback.onError("Error processing face: " + e.getMessage());
        } finally {
            originalBitmap.recycle();
        }
    }

    private float[] extractEmbedding(Bitmap alignedFace) {
        ByteBuffer imgData = ByteBuffer.allocateDirect(IMAGE_SIZE * IMAGE_SIZE * 3 * 4);
        imgData.order(ByteOrder.nativeOrder());

        int[] intValues = new int[IMAGE_SIZE * IMAGE_SIZE];
        alignedFace.getPixels(intValues, 0, alignedFace.getWidth(), 0, 0,
                alignedFace.getWidth(), alignedFace.getHeight());

        imgData.rewind();
        for (int value : intValues) {
            imgData.putFloat(((value >> 16) & 0xFF) / 255.0f);
            imgData.putFloat(((value >> 8) & 0xFF) / 255.0f);
            imgData.putFloat((value & 0xFF) / 255.0f);
        }

        float[][] embeddings = new float[1][EMBEDDING_SIZE];
        Object[] inputArray = {imgData};
        Object[] outputArray = {embeddings};

        faceNetInterpreter.run(inputArray, outputArray);

        return embeddings[0];
    }

    private float calculateLightingCondition(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int pixelCount = width * height;
        long totalBrightness = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = image.getPixel(x, y);
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;

                // Calculate luminance
                totalBrightness += (r + g + b) / 3;
            }
        }

        // Normalize to 0-1 range
        return (float) totalBrightness / (pixelCount * 255);
    }

    private FacialFeatures extractFacialFeatures(Face face, ImageProxy image) {
        FacialFeatures features = new FacialFeatures();

        // Set pose information
        float[] pose3D = new float[3];
        pose3D[0] = face.getHeadEulerAngleX();
        pose3D[1] = face.getHeadEulerAngleY();
        pose3D[2] = face.getHeadEulerAngleZ();
        features.setPose3D(pose3D);

        // Set tracking confidence
        features.setConfidence(face.getSmilingProbability() != null ? face.getSmilingProbability() : 0.0f);

        return features;
    }

    public void release() {
        detector.close();
        faceNetInterpreter.close();
    }
}