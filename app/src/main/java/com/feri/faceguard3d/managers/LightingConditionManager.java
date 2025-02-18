package com.feri.faceguard3d.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.feri.faceguard3d.utils.ImageUtils;
import com.feri.faceguard3d.utils.LogUtils;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LightingConditionManager implements SensorEventListener{

    private static final String TAG = "LightingConditionManager";

    private final Context context;
    private final SensorManager sensorManager;
    private final Sensor lightSensor;

    private float currentLightLevel = -1;
    private static final float MIN_ACCEPTABLE_LIGHT = 5.0f;  // lux
    private static final float MAX_ACCEPTABLE_LIGHT = 1000.0f; // lux

    // Pentru histograma adaptivă
    private static final int HIST_SIZE = 256;
    private static final float[] HIST_RANGE = {0f, 256f};
    private final MatOfInt histSize = new MatOfInt(HIST_SIZE);
    private final MatOfFloat histRange = new MatOfFloat(HIST_RANGE);
    private final MatOfInt channels = new MatOfInt(0);

    public LightingConditionManager(Context context) {
        this.context = context.getApplicationContext();
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        if (lightSensor != null) {
            startLightSensorMonitoring();
        } else {
            Log.w(TAG, "Light sensor not available on this device");
        }
    }

    private void startLightSensorMonitoring() {
        sensorManager.registerListener(this, lightSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            currentLightLevel = event.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Nu este necesară implementarea pentru acest caz
    }

    public boolean isLightingAcceptable() {
        if (currentLightLevel < 0) return true; // Senzorul nu este disponibil
        return currentLightLevel >= MIN_ACCEPTABLE_LIGHT &&
                currentLightLevel <= MAX_ACCEPTABLE_LIGHT;
    }

    public Bitmap normalizeLighting(Bitmap original) {
        try {
            Mat mat = new Mat();
            Utils.bitmapToMat(original, mat);

            // Convertește la scară de gri
            Mat grayMat = new Mat();
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY);

            // Calculează histograma inițială
            Mat hist = new Mat();
            List<Mat> grayMatList = Arrays.asList(grayMat);
            Imgproc.calcHist(grayMatList, channels, new Mat(), hist,
                    histSize, histRange, false);

            // Normalizează histograma
            Core.normalize(hist, hist, 0, 255, Core.NORM_MINMAX);

            // Aplică egalizarea histogramei adaptive
            Mat equalizedMat = new Mat();
            Imgproc.equalizeHist(grayMat, equalizedMat);

            // Aplică filtrul bilateral pentru a păstra detaliile marginilor
            Mat smoothedMat = new Mat();
            Imgproc.bilateralFilter(equalizedMat, smoothedMat, 9, 75, 75);

            // Convertește înapoi la RGB
            Mat resultMat = new Mat();
            Imgproc.cvtColor(smoothedMat, resultMat, Imgproc.COLOR_GRAY2BGR);

            // Convertește rezultatul în Bitmap
            Bitmap result = Bitmap.createBitmap(original.getWidth(),
                    original.getHeight(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(resultMat, result);

            // Eliberează resursele
            mat.release();
            grayMat.release();
            hist.release();
            equalizedMat.release();
            smoothedMat.release();
            resultMat.release();

            return result;
        } catch (Exception e) {
            LogUtils.e(TAG, "Error normalizing lighting", e);
            return original;
        }
    }

    public float calculateLightingScore(Bitmap image) {
        try {
            Mat mat = new Mat();
            Utils.bitmapToMat(image, mat);

            // Convertește la scară de gri
            Mat grayMat = new Mat();
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY);

            // Calculează valorile medii și deviația standard
            Mat mean = new Mat();
            Mat stddev = new Mat();
            Core.meanStdDev(grayMat, mean, stddev);

            double averageBrightness = mean.get(0, 0)[0];
            double contrastLevel = stddev.get(0, 0)[0];

            // Calculează scorul de iluminare
            float score = calculateQualityScore(averageBrightness, contrastLevel);

            // Eliberează resursele
            mat.release();
            grayMat.release();
            mean.release();
            stddev.release();

            return score;
        } catch (Exception e) {
            LogUtils.e(TAG, "Error calculating lighting score", e);
            return 0.0f;
        }
    }

    private float calculateQualityScore(double brightness, double contrast) {
        // Valorile ideale pentru luminozitate și contrast
        final double IDEAL_BRIGHTNESS = 128.0; // Mijlocul intervalului 0-255
        final double IDEAL_CONTRAST = 50.0;    // O valoare bună pentru contrast

        // Calculează diferențele față de valorile ideale
        double brightnessDiff = Math.abs(brightness - IDEAL_BRIGHTNESS) / IDEAL_BRIGHTNESS;
        double contrastDiff = Math.abs(contrast - IDEAL_CONTRAST) / IDEAL_CONTRAST;

        // Ponderile pentru fiecare factor (suma trebuie să fie 1.0)
        final double BRIGHTNESS_WEIGHT = 0.6;
        final double CONTRAST_WEIGHT = 0.4;

        // Calculează scorul final (1.0 este perfect, 0.0 este cel mai rău)
        double score = 1.0 - (brightnessDiff * BRIGHTNESS_WEIGHT +
                contrastDiff * CONTRAST_WEIGHT);

        // Normalizează scorul între 0 și 1
        return (float) Math.max(0.0, Math.min(1.0, score));
    }

    public Bitmap adjustBrightness(Bitmap image, float targetBrightness) {
        try {
            Mat mat = new Mat();
            Utils.bitmapToMat(image, mat);

            // Convertește la HSV pentru a ajusta luminozitatea
            Mat hsvMat = new Mat();
            Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_BGR2HSV);

            // Separă canalele HSV
            List<Mat> hsvChannels = new ArrayList<>();
            Core.split(hsvMat, hsvChannels);

            // Ajustează canalul de luminozitate (V)
            Mat valueChannel = hsvChannels.get(2);
            Core.multiply(valueChannel, new org.opencv.core.Scalar(targetBrightness), valueChannel);

            // Combină canalele înapoi
            Core.merge(hsvChannels, hsvMat);

            // Convertește înapoi la BGR
            Imgproc.cvtColor(hsvMat, mat, Imgproc.COLOR_HSV2BGR);

            // Convertește la Bitmap
            Bitmap result = Bitmap.createBitmap(image.getWidth(),
                    image.getHeight(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, result);

            // Eliberează resursele
            mat.release();
            hsvMat.release();
            for (Mat channel : hsvChannels) {
                channel.release();
            }

            return result;
        } catch (Exception e) {
            LogUtils.e(TAG, "Error adjusting brightness", e);
            return image;
        }
    }

    public boolean isUniformLighting(Bitmap image) {
        try {
            Mat mat = new Mat();
            Utils.bitmapToMat(image, mat);

            // Convertește la scară de gri
            Mat grayMat = new Mat();
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY);

            // Împarte imaginea în regiuni și calculează luminozitatea medie pentru fiecare
            int regions = 9; // 3x3 regiuni
            int width = grayMat.cols() / 3;
            int height = grayMat.rows() / 3;

            List<Double> regionBrightness = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    Mat region = grayMat.submat(i * height, (i + 1) * height,
                            j * width, (j + 1) * width);
                    org.opencv.core.Scalar mean = Core.mean(region);
                    regionBrightness.add(mean.val[0]);
                    region.release();
                }
            }

            // Calculează deviația standard a luminozității între regiuni
            double avg = regionBrightness.stream().mapToDouble(d -> d).average().orElse(0.0);
            double variance = regionBrightness.stream()
                    .mapToDouble(d -> Math.pow(d - avg, 2))
                    .average().orElse(0.0);
            double stdDev = Math.sqrt(variance);

            // Eliberează resursele
            mat.release();
            grayMat.release();

            // Consideră iluminarea uniformă dacă deviația standard este sub un prag
            return stdDev < 30.0; // Pragul poate fi ajustat
        } catch (Exception e) {
            LogUtils.e(TAG, "Error checking lighting uniformity", e);
            return false;
        }
    }

    public void release() {
        if (lightSensor != null) {
            sensorManager.unregisterListener(this);
        }
    }
}