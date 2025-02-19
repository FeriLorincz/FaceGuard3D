package com.feri.faceguard3d.activities;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.SearchView;

import com.feri.faceguard3d.R;
import com.feri.faceguard3d.adapters.AppListAdapter;
import com.feri.faceguard3d.databinding.ActivityProtectedAppsBinding;
import com.feri.faceguard3d.managers.SecuritySettingsManager;
import com.feri.faceguard3d.models.AppInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProtectedAppsActivity extends AppCompatActivity implements AppListAdapter.AppSelectionListener {

    private ActivityProtectedAppsBinding binding;
    private SecuritySettingsManager securityManager;
    private AppListAdapter adapter;
    private ExecutorService executor;
    private List<AppInfo> allApps;
    private Set<String> protectedApps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProtectedAppsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.protected_apps);

        securityManager = SecuritySettingsManager.getInstance(this);
        executor = Executors.newSingleThreadExecutor();

        setupRecyclerView();
        loadApps();
    }

    private void setupRecyclerView() {
        adapter = new AppListAdapter(this, new ArrayList<>(), this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void loadApps() {
        binding.progressBar.setVisibility(View.VISIBLE);
        protectedApps = securityManager.getProtectedApps();

        executor.execute(() -> {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> applications = pm.getInstalledApplications(
                    PackageManager.GET_META_DATA);

            allApps = new ArrayList<>();

            for (ApplicationInfo appInfo : applications) {
                // Exclude aplicațiile de sistem
                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    AppInfo app = new AppInfo(
                            appInfo.loadLabel(pm).toString(),
                            appInfo.packageName,
                            appInfo.loadIcon(pm),
                            protectedApps.contains(appInfo.packageName)
                    );
                    allApps.add(app);
                }
            }

            Collections.sort(allApps, (a1, a2) ->
                    a1.getName().compareToIgnoreCase(a2.getName()));

            runOnUiThread(() -> {
                adapter.updateApps(allApps);
                binding.progressBar.setVisibility(View.GONE);

                if (allApps.isEmpty()) {
                    binding.tvNoApps.setVisibility(View.VISIBLE);
                } else {
                    binding.tvNoApps.setVisibility(View.GONE);
                }
            });
        });
    }

    @Override
    public void onAppProtectionChanged(AppInfo app, boolean isProtected) {
        if (isProtected) {
            securityManager.addProtectedApp(app.getPackageName());
            showProtectionEnabledMessage(app.getName());
        } else {
            securityManager.removeProtectedApp(app.getPackageName());
            showProtectionDisabledMessage(app.getName());
        }
    }

    private void showProtectionEnabledMessage(String appName) {
        Toast.makeText(this,
                getString(R.string.app_protection_enabled, appName),
                Toast.LENGTH_SHORT).show();
    }

    private void showProtectionDisabledMessage(String appName) {
        Toast.makeText(this,
                getString(R.string.app_protection_disabled, appName),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.protected_apps_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterApps(newText);
                return true;
            }
        });

        return true;
    }

    private void filterApps(String query) {
        if (allApps == null) return;

        if (query.isEmpty()) {
            adapter.updateApps(allApps);
            return;
        }

        List<AppInfo> filteredApps = new ArrayList<>();
        String lowercaseQuery = query.toLowerCase();

        for (AppInfo app : allApps) {
            if (app.getName().toLowerCase().contains(lowercaseQuery) ||
                    app.getPackageName().toLowerCase().contains(lowercaseQuery)) {
                filteredApps.add(app);
            }
        }

        adapter.updateApps(filteredApps);

        if (filteredApps.isEmpty()) {
            binding.tvNoApps.setText(R.string.no_apps_found);
            binding.tvNoApps.setVisibility(View.VISIBLE);
        } else {
            binding.tvNoApps.setVisibility(View.GONE);
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
