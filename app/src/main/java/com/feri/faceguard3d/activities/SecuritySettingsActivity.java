package com.feri.faceguard3d.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.feri.faceguard3d.R;
import com.feri.faceguard3d.databinding.ActivitySecuritySettingsBinding;
import com.feri.faceguard3d.managers.SecuritySettingsManager;
import com.feri.faceguard3d.managers.FacialChangesManager;
import com.feri.faceguard3d.utils.SecurityUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class SecuritySettingsActivity extends AppCompatActivity {

    private ActivitySecuritySettingsBinding binding;
    private SecuritySettingsManager securityManager;
    private FacialChangesManager facialChangesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySecuritySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.security_settings);

        securityManager = SecuritySettingsManager.getInstance(this);
        facialChangesManager = new FacialChangesManager(this);

        setupUI();
    }

    private void setupUI() {
        // Setări pentru recunoaștere facială
        binding.switchAdaptiveLearning.setChecked(securityManager.isAdaptiveLearningEnabled());
        binding.switchAdaptiveLearning.setOnCheckedChangeListener((buttonView, isChecked) -> {
            securityManager.setAdaptiveLearningEnabled(isChecked);
            facialChangesManager.setLearningEnabled(isChecked);
            Toast.makeText(this, getString(isChecked ?
                            R.string.adaptive_learning_enabled : R.string.adaptive_learning_disabled),
                    Toast.LENGTH_SHORT).show();
        });

        binding.btnReenrollFace.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.reenroll_face_title)
                    .setMessage(R.string.reenroll_face_message)
                    .setPositiveButton(R.string.reenroll, (dialog, which) -> {
                        startFaceReenrollment();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        binding.btnUpdateBackupPassword.setOnClickListener(v -> showUpdatePasswordDialog());

        // Setări pentru protecția aplicațiilor
        binding.switchMultiFaceDetection.setChecked(securityManager.isMultiFaceDetectionEnabled());
        binding.switchMultiFaceDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            securityManager.setMultiFaceDetectionEnabled(isChecked);
            Toast.makeText(this, getString(isChecked ?
                            R.string.multi_face_detection_enabled : R.string.multi_face_detection_disabled),
                    Toast.LENGTH_SHORT).show();
        });

        binding.btnManageProtectedApps.setOnClickListener(v ->
                startActivity(new Intent(this, ProtectedAppsActivity.class)));

        binding.btnManageProtectedFiles.setOnClickListener(v ->
                startActivity(new Intent(this, ProtectedFilesActivity.class)));

        // Setări pentru confidențialitate
        binding.switchHideNotifications.setChecked(securityManager.isNotificationHidingEnabled());
        binding.switchHideNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            securityManager.setNotificationHidingEnabled(isChecked);
        });

        binding.switchHideAppThumbnails.setChecked(securityManager.isAppThumbnailHidingEnabled());
        binding.switchHideAppThumbnails.setOnCheckedChangeListener((buttonView, isChecked) -> {
            securityManager.setAppThumbnailHidingEnabled(isChecked);
        });

        // Setări avansate
        binding.btnClearLearningData.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.clear_learning_data_title)
                    .setMessage(R.string.clear_learning_data_message)
                    .setPositiveButton(R.string.clear, (dialog, which) -> {
                        facialChangesManager.clearLearningData();
                        Toast.makeText(this, R.string.learning_data_cleared,
                                Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        binding.btnResetAllSettings.setOnClickListener(v -> showResetConfirmationDialog());
    }

    private void showUpdatePasswordDialog() {
        TextInputEditText passwordInput = new TextInputEditText(this);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint(R.string.enter_new_password);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.update_backup_password)
                .setView(passwordInput)
                .setPositiveButton(R.string.update, (dialog, which) -> {
                    String password = passwordInput.getText().toString();
                    if (password.length() >= 6) {
                        String hashedPassword = SecurityUtils.hashPassword(password);
                        securityManager.setBackupPassword(hashedPassword);
                        Toast.makeText(this, R.string.password_updated,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.password_too_short,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void startFaceReenrollment() {
        Intent intent = new Intent(this, FaceEnrollmentActivity.class);
        intent.putExtra("isReenrollment", true);
        startActivity(intent);
        finish();
    }

    private void showResetConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reset_settings_title)
                .setMessage(R.string.reset_settings_message)
                .setPositiveButton(R.string.reset, (dialog, which) -> {
                    resetAllSettings();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void resetAllSettings() {
        securityManager.clearAllSecuritySettings();
        facialChangesManager.clearLearningData();
        Toast.makeText(this, R.string.settings_reset_complete,
                Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}