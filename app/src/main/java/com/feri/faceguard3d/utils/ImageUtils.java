package com.feri.faceguard3d.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.face.Face;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;


public class ImageUtils {

    private static final String TAG = "ImageUtils";

    public static Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    public static com.google.mlkit.vision.common.InputImage imageProxyToInputImage(
            ImageProxy image) {
        try {
            return com.google.mlkit.vision.common.InputImage.fromMediaImage(
                    image.getImage(), image.getImageInfo().getRotationDegrees());
        } catch (Exception e) {
            Log.e(TAG, "Error converting ImageProxy to InputImage", e);
            return null;
        }
    }

    public static Bitmap cropAndAlignFace(Bitmap original, Face face, int targetSize) {
        try {
            // Obține dreptunghiul feței
            Rect bounds = face.getBoundingBox();

            // Extinde zona de decupare pentru a include mai mult context
            int expandSize = (int) (Math.min(bounds.width(), bounds.height()) * 0.3f);
            bounds.left = Math.max(0, bounds.left - expandSize);
            bounds.top = Math.max(0, bounds.top - expandSize);
            bounds.right = Math.min(original.getWidth(), bounds.right + expandSize);
            bounds.bottom = Math.min(original.getHeight(), bounds.bottom + expandSize);

            // Decupează imaginea
            Bitmap cropped = Bitmap.createBitmap(original,
                    bounds.left, bounds.top, bounds.width(), bounds.height());

            // Rotește imaginea pentru aliniere folosind unghiurile Euler
            Matrix matrix = new Matrix();
            matrix.postRotate(-face.getHeadEulerAngleZ());

            Bitmap rotated = Bitmap.createBitmap(cropped, 0, 0,
                    cropped.getWidth(), cropped.getHeight(), matrix, true);

            // Redimensionează la dimensiunea țintă
            return Bitmap.createScaledBitmap(rotated, targetSize, targetSize, true);
        } catch (Exception e) {
            Log.e(TAG, "Error cropping and aligning face", e);
            return null;
        }
    }

    public static Mat bitmapToMat(Bitmap bitmap) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        return mat;
    }

    public static Bitmap matToBitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(),
                Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }

    public static MappedByteBuffer loadModelFile(Context context, String modelPath)
            throws IOException {
        String modelFile = context.getAssets().openFd(modelPath).toString();
        FileInputStream inputStream = new FileInputStream(new File(modelFile));
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = 0;
        long declaredLength = fileChannel.size();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public static Bitmap normalizeImage(Bitmap original) {
        Mat mat = bitmapToMat(original);

        // Convertește la scară de gri
        Mat grayMat = new Mat();
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY);

        // Aplică egalizarea histogramei adaptive
        Mat equalizedMat = new Mat();
        Imgproc.equalizeHist(grayMat, equalizedMat);

        // Aplică filtru Gaussian pentru reducerea zgomotului
        Imgproc.GaussianBlur(equalizedMat, equalizedMat, new org.opencv.core.Size(5, 5), 0);

        return matToBitmap(equalizedMat);
    }

    public static float calculateBrightness(Bitmap bitmap) {
        if (bitmap == null) return 0;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int pixelCount = width * height;
        int[] pixels = new int[pixelCount];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        long totalLuminance = 0;
        for (int pixel : pixels) {
            int r = (pixel >> 16) & 0xff;
            int g = (pixel >> 8) & 0xff;
            int b = pixel & 0xff;
            // Formula pentru luminanță percepută
            totalLuminance += (int) (0.2126 * r + 0.7152 * g + 0.0722 * b);
        }

        return (float) totalLuminance / pixelCount / 255.0f;
    }

    public static void alignFacialLandmarks(MatOfPoint2f sourceLandmarks,
                                            MatOfPoint2f targetLandmarks, Mat image) {
        // Calculează transformarea afină
        Mat transform = Imgproc.estimateAffinePartial2D(sourceLandmarks, targetLandmarks);

        if (transform != null) {
            // Aplică transformarea
            Mat result = new Mat();
            Imgproc.warpAffine(image, result, transform, image.size());
            result.copyTo(image);
        }
    }

    public static Point[] getLandmarkPoints(Face face) {
        if (face.getLandmarks() == null) return new Point[0];

        Point[] points = new Point[face.getLandmarks().size()];
        for (int i = 0; i < face.getLandmarks().size(); i++) {
            points[i] = new Point(
                    face.getLandmarks().get(i).getPosition().x,
                    face.getLandmarks().get(i).getPosition().y
            );
        }
        return points;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        if (rotationDegrees == 0) return bitmap;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        return Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    public static Bitmap yuv420ToRgb(ImageProxy image) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            Log.e(TAG, "Unsupported image format: " + image.getFormat());
            return null;
        }

        Image mediaImage = image.getImage();
        if (mediaImage == null) return null;

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        // U și V sunt intercalate
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21,
                image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0,
                yuvImage.getWidth(), yuvImage.getHeight()), 100, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }
}
