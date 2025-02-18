package com.feri.faceguard3d.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.feri.faceguard3d.R;
import com.feri.faceguard3d.adapters.ProtectedFilesAdapter;
import com.feri.faceguard3d.databinding.ActivityProtectedFilesBinding;
import com.feri.faceguard3d.managers.ContentHidingManager;
import com.feri.faceguard3d.managers.SecuritySettingsManager;
import com.feri.faceguard3d.models.HiddenContent;
import com.feri.faceguard3d.utils.FileUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProtectedFilesActivity extends AppCompatActivity implements PrtectedFilesActivity.FileActionListener {

    private ActivityProtectedFilesBinding binding;
    private SecuritySettingsManager securityManager;
    private ContentHidingManager contentHidingManager;
    private ProtectedFilesAdapter adapter;
    private ExecutorService executor;
    private List<HiddenContent> protectedFiles;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null) {
                                handleFileSelection(data.getData());
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProtectedFilesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.protected_files);

        securityManager = SecuritySettingsManager.getInstance(this);
        contentHidingManager = new ContentHidingManager(this);
        executor = Executors.newSingleThreadExecutor();

        setupRecyclerView();
        checkPermissionsAndLoadFiles();
        setupFab();
    }

    private void setupRecyclerView() {
        adapter = new ProtectedFilesAdapter(this, new ArrayList<>(), this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupFab() {
        binding.fabAddFile.setOnClickListener(v -> showFilePickerOptions());
    }

    private void showFilePickerOptions() {
        String[] options = {
                getString(R.string.pick_file),
                getString(R.string.pick_image),
                getString(R.string.pick_video)
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.select_content_type)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            launchFilePicker();
                            break;
                        case 1:
                            launchImagePicker();
                            break;
                        case 2:
                            launchVideoPicker();
                            break;
                    }
                })
                .show();
    }

    private void launchFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        filePickerLauncher.launch(intent);
    }

    private void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        filePickerLauncher.launch(intent);
    }

    private void launchVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        filePickerLauncher.launch(intent);
    }

    private void handleFileSelection(Uri uri) {
        executor.execute(() -> {
            try {
                String filePath = FileUtils.getPathFromUri(this, uri);
                if (filePath == null) {
                    showError(getString(R.string.error_file_path));
                    return;
                }

                HiddenContent.ContentType contentType =
                        FileUtils.getContentType(this, uri);
                String fileName = FileUtils.getFileName(this, uri);

                HiddenContent content = new HiddenContent(filePath, contentType, fileName);

                runOnUiThread(() -> {
                    contentHidingManager.addProtectedContent(content);
                    refreshFilesList();
                    showSuccess(getString(R.string.file_protected_success));
                });
            } catch (Exception e) {
                showError(getString(R.string.error_protecting_file));
            }
        });
    }

    private void checkPermissionsAndLoadFiles() {
        if (hasRequiredPermissions()) {
            loadProtectedFiles();
        } else {
            ActivityCompat.requestPermissions(this,
                    REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    private boolean hasRequiredPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void loadProtectedFiles() {
        binding.progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            protectedFiles = contentHidingManager.getProtectedContents();
            runOnUiThread(() -> {
                adapter.updateFiles(protectedFiles);
                binding.progressBar.setVisibility(View.GONE);
                updateEmptyState();
            });
        });
    }

    private void updateEmptyState() {
        if (protectedFiles.isEmpty()) {
            binding.tvNoFiles.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
        } else {
            binding.tvNoFiles.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onFileClick(HiddenContent file) {
        if (!file.isEncrypted()) {
            FileUtils.openFile(this, file);
        } else {
            showError(getString(R.string.file_encrypted));
        }
    }

    @Override
    public void onFileRemove(HiddenContent file) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.remove_protection)
                .setMessage(getString(R.string.remove_protection_confirm, file.getName()))
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    contentHidingManager.removeProtectedContent(file.getId());
                    refreshFilesList();
                    showSuccess(getString(R.string.protection_removed));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void refreshFilesList() {
        loadProtectedFiles();
    }

    private void showSuccess(String message) {
        runOnUiThread(() ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void showError(String message) {
        runOnUiThread(() ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadProtectedFiles();
            } else {
                showError(getString(R.string.permissions_required));
                finish();
            }
        }
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
        executor.shutdown();
    }
}