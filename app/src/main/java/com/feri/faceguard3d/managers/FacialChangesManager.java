package com.feri.faceguard3d.managers;

import android.content.Context;
import android.util.Log;

import com.feri.faceguard3d.models.FacialFeatures;
import com.feri.faceguard3d.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FacialChangesManager {

    private static final String TAG = "FacialChangesManager";
    private static final int MAX_STORED_FEATURES = 10;
    private static final float SIMILARITY_THRESHOLD = 0.85f;
    private static final float LEARNING_RATE = 0.1f;

    private final Context context;
    private final SecuritySettingsManager securityManager;
    private final ExecutorService executor;
    private boolean isLearningEnabled = true;

    public FacialChangesManager(Context context) {
        this.context = context.getApplicationContext();
        this.securityManager = SecuritySettingsManager.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void processSuccessfulAuthentication(FacialFeatures newFeatures) {
        if (!isLearningEnabled) return;

        executor.execute(() -> {
            try {
                List<FacialFeatures> storedFeatures = securityManager.getFacialFeatures();
                if (shouldUpdateFeatures(newFeatures, storedFeatures)) {
                    updateStoredFeatures(newFeatures, storedFeatures);
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "Error processing authentication", e);
            }
        });
    }

    private boolean shouldUpdateFeatures(FacialFeatures newFeatures,
                                         List<FacialFeatures> storedFeatures) {
        if (storedFeatures.isEmpty()) return true;

        // Calculează similaritatea medie cu caracteristicile stocate
        float totalSimilarity = 0;
        int count = 0;

        for (FacialFeatures stored : storedFeatures) {
            float similarity = newFeatures.calculateSimilarity(stored);
            if (similarity >= SIMILARITY_THRESHOLD) {
                totalSimilarity += similarity;
                count++;
            }
        }

        if (count == 0) return false;
        float averageSimilarity = totalSimilarity / count;

        // Decide dacă trebuie actualizate caracteristicile
        return averageSimilarity >= SIMILARITY_THRESHOLD &&
                averageSimilarity < 0.95f; // Nu actualiza dacă similaritatea e prea mare
    }

    private void updateStoredFeatures(FacialFeatures newFeatures,
                                      List<FacialFeatures> storedFeatures) {
        // Menține numărul maxim de caracteristici stocate
        while (storedFeatures.size() >= MAX_STORED_FEATURES) {
            // Elimină caracteristicile cele mai vechi
            storedFeatures.remove(0);
        }

        // Adaugă noile caracteristici
        storedFeatures.add(newFeatures);

        // Salvează caracteristicile actualizate
        securityManager.saveFacialFeatures(storedFeatures);

        LogUtils.i(TAG, "Facial features updated with new sample");
    }

    public void detectFacialChanges(FacialFeatures currentFeatures) {
        executor.execute(() -> {
            List<FacialFeatures> storedFeatures = securityManager.getFacialFeatures();
            if (storedFeatures.isEmpty()) return;

            // Analizează schimbările în timp
            analyzeFacialChanges(currentFeatures, storedFeatures);
        });
    }

    private void analyzeFacialChanges(FacialFeatures current,
                                      List<FacialFeatures> stored) {
        List<String> changes = new ArrayList<>();

        // Verifică schimbările în atributele faciale
        if (hasBeardChanged(current, stored)) {
            changes.add("beard");
        }
        if (hasGlassesChanged(current, stored)) {
            changes.add("glasses");
        }

        // Dacă sunt detectate schimbări, actualizează modelul
        if (!changes.isEmpty()) {
            adaptToChanges(current, changes);
            LogUtils.i(TAG, "Detected facial changes: " + String.join(", ", changes));
        }
    }

    private boolean hasBeardChanged(FacialFeatures current,
                                    List<FacialFeatures> stored) {
        int beardCount = 0;
        for (FacialFeatures feature : stored) {
            if (feature.hasBeard()) beardCount++;
        }
        float beardRatio = (float) beardCount / stored.size();

        return (beardRatio > 0.7f && !current.hasBeard()) ||
                (beardRatio < 0.3f && current.hasBeard());
    }

    private boolean hasGlassesChanged(FacialFeatures current,
                                      List<FacialFeatures> stored) {
        int glassesCount = 0;
        for (FacialFeatures feature : stored) {
            if (feature.hasGlasses()) glassesCount++;
        }
        float glassesRatio = (float) glassesCount / stored.size();

        return (glassesRatio > 0.7f && !current.hasGlasses()) ||
                (glassesRatio < 0.3f && current.hasGlasses());
    }

    private void adaptToChanges(FacialFeatures current, List<String> changes) {
        List<FacialFeatures> storedFeatures = securityManager.getFacialFeatures();

        // Actualizează gradual modelul pentru a se adapta la schimbări
        for (String change : changes) {
            switch (change) {
                case "beard":
                    updateBeardModel(current, storedFeatures);
                    break;
                case "glasses":
                    updateGlassesModel(current, storedFeatures);
                    break;
            }
        }

        securityManager.saveFacialFeatures(storedFeatures);
    }

    private void updateBeardModel(FacialFeatures current,
                                  List<FacialFeatures> stored) {
        // Actualizează gradual modelul pentru a se adapta la prezența/absența bărbii
        for (FacialFeatures feature : stored) {
            feature.setHasBeard(current.hasBeard());
        }
    }

    private void updateGlassesModel(FacialFeatures current,
                                    List<FacialFeatures> stored) {
        // Actualizează gradual modelul pentru a se adapta la prezența/absența ochelarilor
        for (FacialFeatures feature : stored) {
            feature.setHasGlasses(current.hasGlasses());
        }
    }

    public void setLearningEnabled(boolean enabled) {
        this.isLearningEnabled = enabled;
        LogUtils.i(TAG, "Adaptive learning " + (enabled ? "enabled" : "disabled"));
    }

    public void clearLearningData() {
        executor.execute(() -> {
            List<FacialFeatures> features = securityManager.getFacialFeatures();
            if (!features.isEmpty()) {
                // Păstrează doar primul set de caracteristici
                FacialFeatures initial = features.get(0);
                features.clear();
                features.add(initial);
                securityManager.saveFacialFeatures(features);
            }
            LogUtils.i(TAG, "Learning data cleared");
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}