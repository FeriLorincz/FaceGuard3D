package com.feri.faceguard3d.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.feri.faceguard3d.databinding.ItemAppBinding;
import com.feri.faceguard3d.models.AppInfo;

import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {

    private final Context context;
    private List<AppInfo> apps;
    private final AppSelectionListener listener;

    public interface AppSelectionListener {
        void onAppProtectionChanged(AppInfo app, boolean isProtected);
    }

    public AppListAdapter(Context context, List<AppInfo> apps, AppSelectionListener listener) {
        this.context = context;
        this.apps = apps;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAppBinding binding = ItemAppBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new AppViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo app = apps.get(position);
        holder.bind(app);
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public void updateApps(List<AppInfo> newApps) {
        this.apps = newApps;
        notifyDataSetChanged();
    }

    class AppViewHolder extends RecyclerView.ViewHolder {
        private final ItemAppBinding binding;

        AppViewHolder(ItemAppBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AppInfo app) {
            binding.ivAppIcon.setImageDrawable(app.getIcon());
            binding.tvAppName.setText(app.getName());
            binding.tvPackageName.setText(app.getPackageName());
            binding.switchProtection.setChecked(app.isProtected());

            binding.switchProtection.setOnCheckedChangeListener((buttonView, isChecked) -> {
                app.setProtected(isChecked);
                listener.onAppProtectionChanged(app, isChecked);
            });

            // Permite și click pe întregul element pentru a schimba starea
            binding.getRoot().setOnClickListener(v -> {
                boolean newState = !binding.switchProtection.isChecked();
                binding.switchProtection.setChecked(newState);
                app.setProtected(newState);
                listener.onAppProtectionChanged(app, newState);
            });
        }
    }
}
