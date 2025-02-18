package com.feri.faceguard3d.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.feri.faceguard3d.R;
import com.feri.faceguard3d.adapters.ProtectedFilesAdapter;
import com.feri.faceguard3d.databinding.ActivityHiddenContentBinding;
import com.feri.faceguard3d.managers.ContentHidingManager;
import com.feri.faceguard3d.models.HiddenContent;
import com.feri.faceguard3d.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HiddenContentActivity extends AppCompatActivity implements ProtectedFilesAdapter.FileActionListener {

    private ActivityHiddenContentBinding binding;
    private ContentHidingManager contentHidingManager;
    private ProtectedFilesAdapter adapter;
    private ExecutorService executor;
    private List<HiddenContent> hiddenContents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHiddenContentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.hidden_content);

        contentHidingManager = new ContentHidingManager(this);
        executor = Executors.newSingleThreadExecutor();

        setupRecyclerView();
        loadHiddenContent();
    }

    private void setupRecyclerView() {
        adapter = new ProtectedFilesAdapter(this, new ArrayList<>(), this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void loadHiddenContent() {
        binding.progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            hiddenContents = contentHidingManager.getProtectedContents();
            runOnUiThread(() -> {
                adapter.updateFiles(hiddenContents);
                binding.progressBar.setVisibility(View.GONE);
                updateEmptyState();
            });
        });
    }

    private void updateEmptyState() {
        if (hiddenContents.isEmpty()) {
            binding.tvNoContent.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
        } else {
            binding.tvNoContent.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onFileClick(HiddenContent content) {
        executor.execute(() -> {
            try {
                contentHidingManager.revealContent(content);
                runOnUiThread(() -> {
                    FileUtils.openFile(this, content);
                    contentHidingManager.hideContent(content);
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, R.string.error_revealing_content,
                                Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onFileRemove(HiddenContent content) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.remove_protection)
                .setMessage(getString(R.string.remove_protection_confirm, content.getName()))
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    executor.execute(() -> {
                        contentHidingManager.removeProtectedContent(content.getId());
                        runOnUiThread(this::loadHiddenContent);
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
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
        binding = null;
    }
}