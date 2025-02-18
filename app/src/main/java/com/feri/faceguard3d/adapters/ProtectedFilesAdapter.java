package com.feri.faceguard3d.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.feri.faceguard3d.R;
import com.feri.faceguard3d.databinding.ItemProtectedFileBinding;
import com.feri.faceguard3d.models.HiddenContent;
import com.feri.faceguard3d.utils.FileUtils;

import java.util.List;

public class ProtectedFilesAdapter extends RecyclerView.Adapter<ProtectedFilesAdapter.FileViewHolder>{

    private final Context context;
    private List<HiddenContent> files;
    private final FileActionListener listener;

    public interface FileActionListener {
        void onFileClick(HiddenContent file);
        void onFileRemove(HiddenContent file);
    }

    public ProtectedFilesAdapter(Context context, List<HiddenContent> files,
                                 FileActionListener listener) {
        this.context = context;
        this.files = files;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProtectedFileBinding binding = ItemProtectedFileBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new FileViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        HiddenContent file = files.get(position);
        holder.bind(file);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public void updateFiles(List<HiddenContent> newFiles) {
        this.files = newFiles;
        notifyDataSetChanged();
    }

    class FileViewHolder extends RecyclerView.ViewHolder {
        private final ItemProtectedFileBinding binding;

        FileViewHolder(ItemProtectedFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(HiddenContent file) {
            binding.tvFileName.setText(file.getName());
            binding.tvFileType.setText(getFileTypeText(file.getContentType()));
            binding.ivFileIcon.setImageResource(getFileTypeIcon(file.getContentType()));

            // Status criptare
            binding.tvEncryptionStatus.setText(file.isEncrypted() ?
                    R.string.encrypted : R.string.decrypted);
            binding.tvEncryptionStatus.setTextColor(context.getColor(
                    file.isEncrypted() ? R.color.encrypted : R.color.decrypted));

            // Data ascunderii
            String dateHidden = FileUtils.formatDate(file.getDateHidden());
            binding.tvDateHidden.setText(context.getString(R.string.hidden_on, dateHidden));

            // Dimensiune fișier
            String fileSize = FileUtils.formatFileSize(file.getSize());
            binding.tvFileSize.setText(fileSize);

            // Click listeners
            binding.getRoot().setOnClickListener(v -> listener.onFileClick(file));
            binding.btnRemove.setOnClickListener(v -> listener.onFileRemove(file));
        }

        private String getFileTypeText(HiddenContent.ContentType type) {
            switch (type) {
                case IMAGE:
                    return context.getString(R.string.type_image);
                case VIDEO:
                    return context.getString(R.string.type_video);
                case FILE:
                    return context.getString(R.string.type_file);
                case CONTACT:
                    return context.getString(R.string.type_contact);
                case TEXT:
                    return context.getString(R.string.type_text);
                default:
                    return context.getString(R.string.type_unknown);
            }
        }

        private int getFileTypeIcon(HiddenContent.ContentType type) {
            switch (type) {
                case IMAGE:
                    return R.drawable.ic_image;
                case VIDEO:
                    return R.drawable.ic_video;
                case FILE:
                    return R.drawable.ic_file;
                case CONTACT:
                    return R.drawable.ic_contact;
                case TEXT:
                    return R.drawable.ic_text;
                default:
                    return R.drawable.ic_unknown;
            }
        }
    }
}
