package com.feri.faceguard3d.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.feri.faceguard3d.R;
import com.feri.faceguard3d.databinding.ActivityFaceEnrollmentBinding;
import com.feri.faceguard3d.managers.MultiFaceEnrollmentManager;
import com.feri.faceguard3d.managers.SecuritySettingsManager;
import com.feri.faceguard3d.models.FacialFeatures;
import com.feri.faceguard3d.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceEnrollmentActivity extends AppCompatActivity{

    private ActivityFaceEnrollmentBinding binding;
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;
    private MultiFaceEnrollmentManager enrollmentManager;
    private SecuritySettingsManager securityManager;

    private static final int PERMISSION_REQUEST_CODE = 10;
    private static final int REQUIRED_CAPTURES = 5;
    private int captureCount = 0;
    private List<FacialFeatures> capturedFeatures;
    private boolean isEnrollmentInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFaceEnrollmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        enrollmentManager = new MultiFaceEnrollmentManager(this);
        securityManager = SecuritySettingsManager.getInstance(this);
        capturedFeatures = new ArrayList<>();
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (checkPermissions()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
        }

        setupUI();
    }

    private void setupUI() {
        binding.btnStartEnrollment.setOnClickListener(v -> {
            if (!isEnrollmentInProgress) {
                startEnrollment();
            }
        });

        binding.btnCancel.setOnClickListener(v -> finish());

        updateProgress();
    }

    private void startEnrollment() {
        isEnrollmentInProgress = true;
        captureCount = 0;
        capturedFeatures.clear();
        binding.btnStartEnrollment.setEnabled(false);
        binding.progressEnrollment.setVisibility(View.VISIBLE);
        binding.tvInstructions.setText(R.string.enrollment_instructions);

        // Începe captura automată
        startAutomaticCapture();
    }

    private void startAutomaticCapture() {
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeFacePosition);
    }

    private void analyzeFacePosition(ImageProxy image) {
        if (captureCount >= REQUIRED_CAPTURES || !isEnrollmentInProgress) {
            image.close();
            return;
        }

        enrollmentManager.analyzeFacePosition(image, new MultiFaceEnrollmentManager.FaceAnalysisCallback() {
            @Override
            public void onFaceDetected(FacialFeatures features) {
                if (isGoodCapture(features)) {
                    captureImage();
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    binding.tvInstructions.setText(error);
                });
            }
        });
    }

    private boolean isGoodCapture(FacialFeatures features) {
        return features.isValidPose() && features.isGoodLightingCondition();
    }

    private void captureImage() {
        imageCapture.takePicture(cameraExecutor,
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        processImageCapture(image);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException error) {
                        runOnUiThread(() -> {
                            Toast.makeText(FaceEnrollmentActivity.this,
                                    "Capture failed: " + error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void processImageCapture(ImageProxy image) {
        enrollmentManager.processCapturedImage(image, new MultiFaceEnrollmentManager.EnrollmentCallback() {
            @Override
            public void onFeatureExtracted(FacialFeatures features) {
                capturedFeatures.add(features);
                captureCount++;

                runOnUiThread(() -> {
                    updateProgress();
                    if (captureCount >= REQUIRED_CAPTURES) {
                        completeEnrollment();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(FaceEnrollmentActivity.this,
                            error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateProgress() {
        int progress = (captureCount * 100) / REQUIRED_CAPTURES;
        binding.progressEnrollment.setProgress(progress);
        binding.tvProgress.setText(getString(R.string.enrollment_progress,
                captureCount, REQUIRED_CAPTURES));
    }

    private void completeEnrollment() {
        isEnrollmentInProgress = false;

        // Salvează caracteristicile faciale
        securityManager.saveFacialFeatures(capturedFeatures);
        securityManager.setFaceEnrolled(true);
        securityManager.setFirstRunCompleted();

        Toast.makeText(this, R.string.enrollment_complete, Toast.LENGTH_LONG).show();
        finish();
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .setTargetResolution(new Size(1280, 720))
                .build();

        imageCapture = new ImageCapture.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            if (isEnrollmentInProgress) {
                analyzeFacePosition(image);
            } else {
                image.close();
            }
        });

        // Selectează camera frontală
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        try {
            // Unbind toate use case-urile înainte de a le reface legăturile
            cameraProvider.unbindAll();

            // Bind use case-urile la cameră
            cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
            );

            preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());
        } catch (Exception e) {
            Toast.makeText(this, "Error binding camera use cases: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required for enrollment",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    // Implementare helper pentru actualizarea UI în funcție de starea capturii
    private void updateCaptureUI(String message) {
        runOnUiThread(() -> {
            binding.tvInstructions.setText(message);
            if (captureCount >= REQUIRED_CAPTURES) {
                binding.btnStartEnrollment.setEnabled(true);
                binding.btnStartEnrollment.setText(R.string.complete_enrollment);
            }
        });
    }

    // Helper pentru verificarea calității capturii
    private boolean validateCaptureQuality(FacialFeatures features) {
        if (features == null) return false;

        // Verifică dacă poziția feței este bună
        boolean isPositionGood = features.isValidPose();

        // Verifică iluminarea
        boolean isLightingGood = features.isGoodLightingCondition();

        // Verifică confidența detecției
        boolean isConfidenceGood = features.getConfidence() > 0.8f;

        return isPositionGood && isLightingGood && isConfidenceGood;
    }

    // Helper pentru salvarea backup password
    private void saveBackupPassword(String password) {
        String hashedPassword = SecuritySettingsManager.hashPassword(password);
        securityManager.setBackupPassword(hashedPassword);
    }

    @Override
    public void onBackPressed() {
        if (isEnrollmentInProgress) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.cancel_enrollment_title)
                    .setMessage(R.string.cancel_enrollment_message)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        isEnrollmentInProgress = false;
                        super.onBackPressed();
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}
