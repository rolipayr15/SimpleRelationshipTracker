package com.opengrid.simplerelationshiptracker.util;

import android.content.Context;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.opengrid.simplerelationshiptracker.data.PreferenceManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslationManager {
    public interface TranslationCallback {
        void onProgress(int current, int total);
        void onComplete(Map<String, String> translatedStrings);
        void onError(Exception e);
    }

    private final PreferenceManager preferenceManager;

    public TranslationManager(Context context) {
        this.preferenceManager = new PreferenceManager(context);
    }

    public void translateApp(String targetLangCode, Map<String, String> sourceStrings, TranslationCallback callback) {
        String mlKitCode = getMlKitCode(targetLangCode);
        if (mlKitCode == null) {
            callback.onError(new Exception("Unsupported language code: " + targetLangCode));
            return;
        }

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(mlKitCode)
                .build();

        final Translator translator = Translation.getClient(options);
        DownloadConditions conditions = new DownloadConditions.Builder().requireWifi().build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    performTranslation(translator, sourceStrings, callback);
                })
                .addOnFailureListener(callback::onError);
    }

    private void performTranslation(Translator translator, Map<String, String> sourceStrings, TranslationCallback callback) {
        final Map<String, String> results = new HashMap<>();
        final List<String> keys = new java.util.ArrayList<>(sourceStrings.keySet());
        final int total = keys.size();

        translateNext(translator, keys, sourceStrings, results, 0, total, callback);
    }

    private void translateNext(Translator translator, List<String> keys, Map<String, String> sourceStrings, 
                              Map<String, String> results, int index, int total, TranslationCallback callback) {
        if (index >= total) {
            translator.close();
            callback.onComplete(results);
            return;
        }

        String key = keys.get(index);
        String text = sourceStrings.get(key);

        translator.translate(text)
                .addOnSuccessListener(translatedText -> {
                    results.put(key, translatedText);
                    callback.onProgress(index + 1, total);
                    translateNext(translator, keys, sourceStrings, results, index + 1, total, callback);
                })
                .addOnFailureListener(e -> {
                    translator.close();
                    callback.onError(e);
                });
    }

    private String getMlKitCode(String langCode) {
        // Map common codes to ML Kit codes if necessary. 
        // ML Kit uses ISO 639-1.
        return TranslateLanguage.fromLanguageTag(langCode);
    }
}