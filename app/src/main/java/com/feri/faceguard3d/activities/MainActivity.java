package com.feri.faceguard3d.activities;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.feri.faceguard3d.R;
import com.feri.faceguard3d.databinding.ActivityMainBinding;
import com.feri.faceguard3d.managers.SecuritySettingsManager;
import com.feri.faceguard3d.services.FaceAuthService;
import com.feri.faceguard3d.utils.SecurityUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SecuritySettingsManager securityManager;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private final String[] REQUIRED_PERMISSIONS = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.PACKAGE_USAGE_STATS
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        securityManager = SecuritySettingsManager.getInstance(this);

        if (!checkPermissions()) {
            requestPermissions();
        } else {
            initializeApp();
        }

        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV initialization failed");
        }

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnEnrollFace.setOnClickListener(v -> {
            if (checkPermissions()) {
                startActivity(new Intent(this, FaceEnrollmentActivity.class));
            } else {
                requestPermissions();
            }
        });

        binding.btnSecuritySettings.setOnClickListener(v ->
                startActivity(new Intent(this, SecuritySettingsActivity.class)));

        binding.btnProtectedApps.setOnClickListener(v ->
                startActivity(new Intent(this, ProtectedAppsActivity.class)));

        binding.btnProtectedFiles.setOnClickListener(v ->
                startActivity(new Intent(this, ProtectedFilesActivity.class)));
    }

    private boolean checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                initializeApp();
            } else {
                Toast.makeText(this, "Permissions are required to use the app",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initializeApp() {
        if (securityManager.isFirstRun()) {
            // First time setup
            startActivity(new Intent(this, FaceEnrollmentActivity.class));
            finish();
        } else if (!SecurityUtils.isServiceRunning(this, FaceAuthService.class)) {
            // Start the face authentication service
            startService(new Intent(this, FaceAuthService.class));
        }

        updateUI();
    }

    private void updateUI() {
        boolean isFaceEnrolled = securityManager.isFaceEnrolled();
        binding.btnEnrollFace.setEnabled(!isFaceEnrolled);
        binding.btnEnrollFace.setText(isFaceEnrolled ?
                R.string.face_already_enrolled : R.string.enroll_face);

        binding.btnSecuritySettings.setEnabled(isFaceEnrolled);
        binding.btnProtectedApps.setEnabled(isFaceEnrolled);
        binding.btnProtectedFiles.setEnabled(isFaceEnrolled);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissions()) {
            updateUI();
        }
    }
}