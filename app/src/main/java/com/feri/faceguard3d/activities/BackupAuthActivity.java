package com.feri.faceguard3d.activities;

import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.feri.faceguard3d.R;
import com.feri.faceguard3d.databinding.ActivityBackupAuthBinding;
import com.feri.faceguard3d.managers.SecuritySettingsManager;
import com.feri.faceguard3d.utils.SecurityUtils;

public class BackupAuthActivity extends AppCompatActivity{

    private ActivityBackupAuthBinding binding;
    private SecuritySettingsManager securityManager;
    private int failedAttempts = 0;
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION = 300000; // 5 minute
    private long lastAttemptTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBackupAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        securityManager = SecuritySettingsManager.getInstance(this);

        setupUI();
    }

    private void setupUI() {
        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnAuthenticate.setEnabled(s.length() >= 6);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnAuthenticate.setOnClickListener(v -> attemptAuthentication());
        binding.btnCancel.setOnClickListener(v -> finish());
    }

    private void attemptAuthentication() {
        if (isLockedOut()) {
            long remainingTime = (LOCKOUT_DURATION - (System.currentTimeMillis() - lastAttemptTime)) / 1000;
            Toast.makeText(this, getString(R.string.account_locked, remainingTime),
                    Toast.LENGTH_LONG).show();
            return;
        }

        String password = binding.etPassword.getText().toString();
        String storedHash = securityManager.getBackupPassword();

        if (storedHash == null) {
            Toast.makeText(this, R.string.no_backup_password_set,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (SecurityUtils.verifyHash(password, storedHash)) {
            handleSuccessfulAuthentication();
        } else {
            handleFailedAuthentication();
        }
    }

    private void handleSuccessfulAuthentication() {
        // Resetează contorul de încercări eșuate
        failedAttempts = 0;
        lastAttemptTime = 0;

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnAuthenticate.setEnabled(false);

        // Adaugă o mică întârziere pentru feedback vizual
        binding.getRoot().postDelayed(() -> {
            setResult(RESULT_OK);
            finish();
        }, 1000);
    }

    private void handleFailedAuthentication() {
        failedAttempts++;
        lastAttemptTime = System.currentTimeMillis();

        if (failedAttempts >= MAX_ATTEMPTS) {
            binding.etPassword.setEnabled(false);
            binding.btnAuthenticate.setEnabled(false);

            Toast.makeText(this, getString(R.string.too_many_attempts),
                    Toast.LENGTH_LONG).show();
        } else {
            int remainingAttempts = MAX_ATTEMPTS - failedAttempts;
            Toast.makeText(this, getString(R.string.invalid_password, remainingAttempts),
                    Toast.LENGTH_SHORT).show();
        }

        binding.etPassword.setText("");
    }

    private boolean isLockedOut() {
        if (failedAttempts >= MAX_ATTEMPTS) {
            long elapsedTime = System.currentTimeMillis() - lastAttemptTime;
            if (elapsedTime < LOCKOUT_DURATION) {
                return true;
            } else {
                // Resetează după perioada de blocare
                failedAttempts = 0;
                lastAttemptTime = 0;
            }
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S) {
            getOnBackPressedDispatcher().onBackPressed();
        } else {
            setResult(RESULT_CANCELED);
            super.onBackPressed();
        }
    }
}