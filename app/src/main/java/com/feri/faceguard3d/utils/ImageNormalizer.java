package com.feri.faceguard3d.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ImageNormalizer {

    private static final String TAG = "ImageNormalizer";

    // Constantele pentru normalizare
    private static final int TARGET_SIZE = 160;
    private static final double CONTRAST_ALPHA = 1.5;
    private static final double BRIGHTNESS_BETA = 0;
    private static final int HIST_SIZE = 256;
    private static final float[] RANGES = {0, 256};

    public static Bitmap normalizeFaceImage(Bitmap input) {
        try {
            // Convertește Bitmap la Mat
            Mat sourceMat = new Mat();
            Utils.bitmapToMat(input, sourceMat);

            // Convertește la scară de gri
            Mat grayMat = new Mat();
            Imgproc.cvtColor(sourceMat, grayMat, Imgproc.COLOR_BGR2GRAY);

            // Aplică egalizarea histogramei adaptive
            Mat equalizedMat = new Mat();
            Imgproc.equalizeHist(grayMat, equalizedMat);

            // Aplică filtrarea bilaterală pentru reducerea zgomotului
            Mat denoisedMat = new Mat();
            Imgproc.bilateralFilter(equalizedMat, denoisedMat, 9, 75, 75);

            // Îmbunătățește contrastul
            Mat contrastMat = new Mat();
            denoisedMat.convertTo(contrastMat, -1, CONTRAST_ALPHA, BRIGHTNESS_BETA);

            // Normalizează luminozitatea
            Mat normalizedMat = normalizeIllumination(contrastMat);

            // Redimensionează la dimensiunea țintă
            Mat resizedMat = new Mat();
            Imgproc.resize(normalizedMat, resizedMat, new Size(TARGET_SIZE, TARGET_SIZE));

            // Convertește înapoi la Bitmap
            Bitmap result = Bitmap.createBitmap(TARGET_SIZE, TARGET_SIZE,
                    Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(resizedMat, result);

            // Eliberează resursele
            sourceMat.release();
            grayMat.release();
            equalizedMat.release();
            denoisedMat.release();
            contrastMat.release();
            normalizedMat.release();
            resizedMat.release();

            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error normalizing face image", e);
            return input;
        }
    }

    private static Mat normalizeIllumination(Mat input) {
        Mat output = new Mat();

        // Calculează histograma
        List<Mat> images = new ArrayList<>();
        images.add(input);
        Mat hist = new Mat();
        Imgproc.calcHist(images, new MatOfInt(0), new Mat(), hist,
                new MatOfInt(HIST_SIZE), new MatOfFloat(RANGES));

        // Normalizează histograma
        Core.normalize(hist, hist, 0, 255, Core.NORM_MINMAX);

        // Aplică normalizarea gamma
        Mat lookUpTable = new Mat(1, 256, CvType.CV_8U);
        byte[] lookUpData = new byte[256];
        for (int i = 0; i < 256; i++) {
            lookUpData[i] = (byte) Math.min(255,
                    Math.max(0, Math.pow(i / 255.0, 0.8) * 255));
        }
        lookUpTable.put(0, 0, lookUpData);
        Core.LUT(input, lookUpTable, output);

        return output;
    }

    public static Bitmap rotate(Bitmap bitmap, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
    }

    public static Bitmap alignFace(Bitmap bitmap, Point leftEye, Point rightEye) {
        // Calculează unghiul pentru aliniere
        double dY = rightEye.y - leftEye.y;
        double dX = rightEye.x - leftEye.x;
        double angle = Math.toDegrees(Math.atan2(dY, dX));

        // Rotește imaginea
        Matrix matrix = new Matrix();
        matrix.postRotate((float) angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
    }

    public static Bitmap adjustBrightness(Bitmap input, float factor) {
        Mat sourceMat = new Mat();
        Utils.bitmapToMat(input, sourceMat);

        Mat adjustedMat = new Mat();
        sourceMat.convertTo(adjustedMat, -1, 1, factor);

        Bitmap result = Bitmap.createBitmap(input.getWidth(), input.getHeight(),
                Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(adjustedMat, result);

        sourceMat.release();
        adjustedMat.release();

        return result;
    }

    public static float calculateBlurriness(Mat image) {
        Mat laplacian = new Mat();
        Imgproc.Laplacian(image, laplacian, CvType.CV_64F);

        Mat mean = new Mat();
        Mat stddev = new Mat();
        Core.meanStdDev(laplacian, mean, stddev);

        double variance = Math.pow(stddev.get(0, 0)[0], 2);
        return (float) variance;
    }

    public static boolean isImageQualityAcceptable(Bitmap image) {
        Mat mat = new Mat();
        Utils.bitmapToMat(image, mat);

        // Verifică blurarea
        float blurriness = calculateBlurriness(mat);
        if (blurriness < 100) { // Prag pentru blur
            return false;
        }

        // Verifică contrastul
        Mat grayMat = new Mat();
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY);
        Scalar mean = Core.mean(grayMat);
        if (mean.val[0] < 40 || mean.val[0] > 220) { // Praguri pentru contrast
            return false;
        }

        mat.release();
        grayMat.release();

        return true;
    }
}