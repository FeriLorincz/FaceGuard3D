package com.feri.faceguard3d.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.feri.faceguard3d.R;
import com.feri.faceguard3d.databinding.ActivityFaceAuthenticationBinding;
import com.feri.faceguard3d.managers.SecuritySettingsManager;
import com.feri.faceguard3d.models.FacialFeatures;
import com.feri.faceguard3d.services.FaceAuthService;
import com.feri.faceguard3d.utils.ImageUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceAuthenticationActivity extends AppCompatActivity {

    private ActivityFaceAuthenticationBinding binding;
    private ExecutorService cameraExecutor;
    private SecuritySettingsManager securityManager;
    private FaceAuthService authService;
    private Handler mainHandler;
    private boolean isAuthenticating = false;
    private static final int MAX_ATTEMPTS = 3;
    private int failedAttempts = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFaceAuthenticationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        securityManager = SecuritySettingsManager.getInstance(this);
        authService = new FaceAuthService();
        cameraExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        setupUI();
        startCamera();
    }

    private void setupUI() {
        binding.btnBackupAuth.setOnClickListener(v -> showBackupAuthDialog());

        // Buton invizibil pentru autentificare backup
        binding.btnHiddenBackup.setOnClickListener(v -> showBackupAuthDialog());

        // Setează zona de touch pentru butonul invizibil
        binding.viewFinder.setOnTouchListener((v, event) -> {
            float x = event.getX();
            float y = event.getY();

            // Verifică dacă touch-ul este în zona butonului invizibil (1cm de la marginea de sus)
            if (y < dpToPx(40) && x < dpToPx(40)) {
                showBackupAuthDialog();
                return true;
            }
            return false;
        });
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
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeFace);

        preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
            );
        } catch (Exception e) {
            Toast.makeText(this, "Error binding camera use cases: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void analyzeFace(ImageProxy image) {
        if (isAuthenticating) {
            image.close();
            return;
        }

        isAuthenticating = true;
        authService.authenticate(image, new FaceAuthService.AuthenticationCallback() {
            @Override
            public void onAuthenticationSuccess(FacialFeatures features) {
                mainHandler.post(() -> {
                    handleSuccessfulAuthentication();
                    isAuthenticating = false;
                });
            }

            @Override
            public void onAuthenticationFailure(String reason) {
                mainHandler.post(() -> {
                    handleFailedAuthentication(reason);
                    isAuthenticating = false;
                });
            }

            @Override
            public void onMultipleFacesDetected() {
                mainHandler.post(() -> {
                    handleMultipleFacesDetected();
                    isAuthenticating = false;
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    handleError(error);
                    isAuthenticating = false;
                });
            }
        });
    }

    private void handleSuccessfulAuthentication() {
        binding.ivAuthStatus.setImageResource(R.drawable.ic_auth_success);
        binding.tvStatus.setText(R.string.auth_success);

        // Resetează contorul de încercări eșuate
        failedAttempts = 0;

        // Întârzie finalizarea activității pentru a arăta succesul
        mainHandler.postDelayed(() -> {
            setResult(RESULT_OK);
            finish();
        }, 1000);
    }

    private void handleFailedAuthentication(String reason) {
        failedAttempts++;
        binding.ivAuthStatus.setImageResource(R.drawable.ic_auth_failed);
        binding.tvStatus.setText(getString(R.string.auth_failed, reason));

        if (failedAttempts >= MAX_ATTEMPTS) {
            showBackupAuthDialog();
        }
    }

    private void handleMultipleFacesDetected() {
        binding.tvStatus.setText(R.string.multiple_faces_detected);
        binding.ivAuthStatus.setImageResource(R.drawable.ic_warning);
    }

    private void handleError(String error) {
        binding.tvStatus.setText(getString(R.string.auth_error, error));
        binding.ivAuthStatus.setImageResource(R.drawable.ic_error);
    }

    private void showBackupAuthDialog() {
        Intent intent = new Intent(this, BackupAuthActivity.class);
        startActivityForResult(intent, REQUEST_BACKUP_AUTH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BACKUP_AUTH && resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            finish();
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private static final int REQUEST_BACKUP_AUTH = 100;
}