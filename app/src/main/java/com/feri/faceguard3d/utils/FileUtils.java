package com.feri.faceguard3d.utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import androidx.core.content.FileProvider;

import com.feri.faceguard3d.models.HiddenContent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtils {

    private static final String TAG = "FileUtils";
    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public static String getPathFromUri(Context context, Uri uri) {
        String path = null;

        try {
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                String[] projection = {MediaStore.MediaColumns.DATA};
                Cursor cursor = null;

                try {
                    cursor = context.getContentResolver().query(
                            uri, projection, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                        path = cursor.getString(columnIndex);
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }

                if (path == null) {
                    path = getDataColumn(context, uri, null, null);
                }
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                path = uri.getPath();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return path;
    }

    private static String getDataColumn(Context context, Uri uri,
                                        String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(
                    uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(columnIndex);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(
                    uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public static HiddenContent.ContentType getContentType(Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType == null) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                return HiddenContent.ContentType.IMAGE;
            } else if (mimeType.startsWith("video/")) {
                return HiddenContent.ContentType.VIDEO;
            } else if (mimeType.startsWith("text/")) {
                return HiddenContent.ContentType.TEXT;
            }
        }

        return HiddenContent.ContentType.FILE;
    }

    public static String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.getDefault(), "%.1f %s",
                size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    public static String formatDate(long timestamp) {
        return dateFormat.format(new Date(timestamp));
    }

    public static void openFile(Context context, HiddenContent content) {
        try {
            File file = new File(content.getOriginalPath());
            Uri contentUri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".provider", file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, getMimeType(file));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getMimeType(File file) {
        String extension = getFileExtension(file);
        if (extension != null) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return "*/*";
    }

    private static String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return null;
        }
        return name.substring(lastIndexOf + 1).toLowerCase();
    }

    public static boolean isMediaFile(String path) {
        String mimeType = getMimeType(new File(path));
        return mimeType != null && (
                mimeType.startsWith("image/") ||
                        mimeType.startsWith("video/") ||
                        mimeType.startsWith("audio/"));
    }

    public static void createNoMediaFile(String directoryPath) {
        File noMediaFile = new File(directoryPath, ".nomedia");
        if (!noMediaFile.exists()) {
            try {
                noMediaFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
