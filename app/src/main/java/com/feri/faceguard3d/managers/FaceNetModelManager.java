package com.feri.faceguard3d.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.feri.faceguard3d.utils.ImageUtils;
import com.feri.faceguard3d.utils.ImageNormalizer;
import com.google.mlkit.vision.face.Face;

import org.tensorflow.lite.Interpreter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class FaceNetModelManager {

    private static final String TAG = "FaceNetModelManager";
    private static final String MODEL_FILE = "facenet_model.tflite";
    private static final int INPUT_SIZE = 160;
    private static final int EMBEDDING_SIZE = 512;
    private static final int BATCH_SIZE = 1;
    private static final int PIXEL_SIZE = 3;
    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 128.0f;

    private final Context context;
    private Interpreter interpreter;
    private ByteBuffer inputBuffer;
    private float[][] embeddingBuffer;
    private final Map<String, Object[]> outputMap;

    private static volatile FaceNetModelManager instance;

    private FaceNetModelManager(Context context) {
        this.context = context.getApplicationContext();
        this.outputMap = new HashMap<>();
        initializeModel();
        initializeBuffers();
    }

    public static FaceNetModelManager getInstance(Context context) {
        if (instance == null) {
            synchronized (FaceNetModelManager.class) {
                if (instance == null) {
                    instance = new FaceNetModelManager(context);
                }
            }
        }
        return instance;
    }

    private void initializeModel() {
        try {
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            options.setUseNNAPI(true);
            interpreter = new Interpreter(ImageUtils.loadModelFile(context, MODEL_FILE), options);
            Log.d(TAG, "FaceNet model initialized successfully");
        } catch (IOException e) {
            Log.e(TAG, "Error initializing FaceNet model", e);
            throw new RuntimeException("Failed to load FaceNet model", e);
        }
    }

    private void initializeBuffers() {
        inputBuffer = ByteBuffer.allocateDirect(
                BATCH_SIZE * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE * 4);
        inputBuffer.order(ByteOrder.nativeOrder());

        embeddingBuffer = new float[BATCH_SIZE][EMBEDDING_SIZE];
        outputMap.put("output", new Object[]{embeddingBuffer});
    }

    public float[] generateEmbedding(Bitmap face, Face mlkitFace) {
        if (face == null || mlkitFace == null) {
            Log.e(TAG, "Invalid input for embedding generation");
            return null;
        }

        try {
            // Preprocesare imagine
            Bitmap normalizedFace = preprocessFace(face, mlkitFace);
            if (normalizedFace == null) return null;

            // Convertește imaginea în ByteBuffer
            convertBitmapToBuffer(normalizedFace);

            // Rulează inferența
            interpreter.runForMultipleInputsOutputs(
                    new Object[]{inputBuffer}, outputMap);

            // Normalizează embedding-ul
            float[] embedding = normalizeEmbedding(embeddingBuffer[0]);

            normalizedFace.recycle();
            return embedding;
        } catch (Exception e) {
            Log.e(TAG, "Error generating face embedding", e);
            return null;
        }
    }

    private Bitmap preprocessFace(Bitmap face, Face mlkitFace) {
        try {
            // Decupează și aliniază fața
            Bitmap alignedFace = ImageUtils.cropAndAlignFace(face, mlkitFace, INPUT_SIZE);
            if (alignedFace == null) return null;

            // Normalizează luminozitatea și contrastul
            return ImageNormalizer.normalizeFaceImage(alignedFace);
        } catch (Exception e) {
            Log.e(TAG, "Error preprocessing face", e);
            return null;
        }
    }

    private void convertBitmapToBuffer(Bitmap bitmap) {
        inputBuffer.rewind();
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

        for (int pixelValue : intValues) {
            float r = (((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            float g = (((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            float b = ((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD;

            inputBuffer.putFloat(r);
            inputBuffer.putFloat(g);
            inputBuffer.putFloat(b);
        }
    }

    private float[] normalizeEmbedding(float[] embedding) {
        float sum = 0;
        for (float val : embedding) {
            sum += val * val;
        }
        float norm = (float) Math.sqrt(sum);

        float[] normalizedEmbedding = new float[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            normalizedEmbedding[i] = embedding[i] / norm;
        }
        return normalizedEmbedding;
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
        instance = null;
    }

    public boolean isModelLoaded() {
        return interpreter != null;
    }

    public float calculateSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1 == null || embedding2 == null ||
                embedding1.length != embedding2.length) {
            return 0.0f;
        }

        float dotProduct = 0.0f;
        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
        }
        return dotProduct;
    }
}