package com.feri.faceguard3d.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.feri.faceguard3d.database.converters.DateConverter;
import com.feri.faceguard3d.database.converters.FloatArrayConverter;
import com.feri.faceguard3d.database.dao.FaceDataDao;
import com.feri.faceguard3d.database.dao.HiddenContentDao;
import com.feri.faceguard3d.database.entities.FaceData;
import com.feri.faceguard3d.database.entities.HiddenContentEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

@Database(
        entities = {
                FaceData.class,
                HiddenContentEntry.class
        },
        version = 1,
        exportSchema = false
)
@TypeConverters({DateConverter.class, FloatArrayConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "faceguard3d.db";
    private static volatile AppDatabase instance;

    // DAO-uri
    public abstract FaceDataDao faceDataDao();
    public abstract HiddenContentDao hiddenContentDao();

    // Singleton pentru accesarea bazei de date
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME)
                            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    // Inițializări la crearea bazei de date
                                }

                                @Override
                                public void onOpen(SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    // Verificări la deschiderea bazei de date
                                }
                            })
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return instance;
    }

    // Migrare pentru versiuni viitoare ale bazei de date
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Aici vor fi adăugate instrucțiunile pentru migrare când va fi necesară
            // o actualizare a schemei bazei de date
        }
    };

    // Metoda pentru curățarea bazei de date
    public void clearAllTables() {
        if (instance != null) {
            instance.clearAllTables();
        }
    }

    // Metoda pentru închiderea bazei de date
    public static void destroyInstance() {
        if (instance != null && instance.isOpen()) {
            instance.close();
            instance = null;
        }
    }

    // Metoda pentru verificarea integrității bazei de date
    public boolean checkDatabaseIntegrity() {
        try {
            SupportSQLiteDatabase db = instance.getOpenHelper().getWritableDatabase();
            return db.isDatabaseIntegrityOk();
        } catch (Exception e) {
            return false;
        }
    }

    // Metoda pentru exportul bazei de date (backup)
    public void exportDatabase(Context context, String path) {
        try {
            File currentDB = context.getDatabasePath(DATABASE_NAME);
            File backupDB = new File(path);

            if (currentDB.exists()) {
                FileChannel src = new FileInputStream(currentDB).getChannel();
                FileChannel dst = new FileOutputStream(backupDB).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Metoda pentru importul bazei de date (restore)
    public void importDatabase(Context context, String path) {
        try {
            File importFile = new File(path);
            File currentDB = context.getDatabasePath(DATABASE_NAME);

            if (importFile.exists()) {
                FileChannel src = new FileInputStream(importFile).getChannel();
                FileChannel dst = new FileOutputStream(currentDB).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}