package com.opengrid.simplerelationshiptracker.data;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PreferenceManager {
    private static final String PREF_NAME = "relationship_tracker_prefs";
    private static final String KEY_START_DATE_MILLIS = "start_date_millis";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_TIMEZONE_ID = "timezone_id";
    private static final String KEY_LAST_SYNC_TIME = "last_sync_time";
    private static final String KEY_SERVER_OFFSET = "server_offset";
    private static final String KEY_LANGUAGE = "app_language";
    private static final String KEY_EVENTS = "calendar_events";
    private static final String KEY_CUSTOM_LANGUAGES = "custom_languages";
    private static final String KEY_TRANSLATIONS_PREFIX = "trans_";
    private static final String KEY_MAP_API_URL = "map_api_url";
    private static final String KEY_MAP_API_KEY = "map_api_key";
    private static final String KEY_TIME_API_URL = "time_api_url";
    private static final String KEY_TIME_API_KEY = "time_api_key";

    private final SharedPreferences prefs;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getMapApiUrl() {
        return prefs.getString(KEY_MAP_API_URL, "https://tile.openstreetmap.org/");
    }

    public void setMapApiUrl(String url) {
        prefs.edit().putString(KEY_MAP_API_URL, url).apply();
    }

    public String getMapApiKey() {
        return prefs.getString(KEY_MAP_API_KEY, "");
    }

    public void setMapApiKey(String key) {
        prefs.edit().putString(KEY_MAP_API_KEY, key).apply();
    }

    public String getTimeApiUrl() {
        return prefs.getString(KEY_TIME_API_URL, "https://www.timeapi.io/");
    }

    public void setTimeApiUrl(String url) {
        prefs.edit().putString(KEY_TIME_API_URL, url).apply();
    }

    public String getTimeApiKey() {
        return prefs.getString(KEY_TIME_API_KEY, "");
    }

    public void setTimeApiKey(String key) {
        prefs.edit().putString(KEY_TIME_API_KEY, key).apply();
    }

    public void deleteTranslations(String langCode) {
        SharedPreferences.Editor editor = prefs.edit();
        Map<String, ?> allEntries = prefs.getAll();
        String prefix = KEY_TRANSLATIONS_PREFIX + langCode + "_";
        for (String key : allEntries.keySet()) {
            if (key.startsWith(prefix)) {
                editor.remove(key);
            }
        }
        Set<String> customLangs = getCustomLanguages();
        customLangs.remove(langCode);
        editor.putStringSet(KEY_CUSTOM_LANGUAGES, customLangs);
        editor.apply();
    }

    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, "en");
    }

    public void setLanguage(String lang) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply();
    }

    public String getEventsJson() {
        return prefs.getString(KEY_EVENTS, "[]");
    }

    public void setEventsJson(String json) {
        prefs.edit().putString(KEY_EVENTS, json).apply();
    }

    public void saveTranslations(String langCode, Map<String, String> translations) {
        SharedPreferences.Editor editor = prefs.edit();
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            editor.putString(KEY_TRANSLATIONS_PREFIX + langCode + "_" + entry.getKey(), entry.getValue());
        }
        Set<String> customLangs = getCustomLanguages();
        customLangs.add(langCode);
        editor.putStringSet(KEY_CUSTOM_LANGUAGES, customLangs);
        editor.apply();
    }

    public Set<String> getCustomLanguages() {
        return new HashSet<>(prefs.getStringSet(KEY_CUSTOM_LANGUAGES, new HashSet<>()));
    }

    public String getTranslation(String langCode, String key, String defaultValue) {
        return prefs.getString(KEY_TRANSLATIONS_PREFIX + langCode + "_" + key, defaultValue);
    }

    public long getStartDateMillis() {
        return prefs.getLong(KEY_START_DATE_MILLIS, 0);
    }

    public void setStartDateMillis(long millis) {
        prefs.edit().putLong(KEY_START_DATE_MILLIS, millis).apply();
    }

    public double getLatitude() {
        return Double.longBitsToDouble(prefs.getLong(KEY_LATITUDE, Double.doubleToLongBits(0.0)));
    }

    public void setLocation(double lat, double lon) {
        prefs.edit()
                .putLong(KEY_LATITUDE, Double.doubleToRawLongBits(lat))
                .putLong(KEY_LONGITUDE, Double.doubleToRawLongBits(lon))
                .apply();
    }

    public double getLongitude() {
        return Double.longBitsToDouble(prefs.getLong(KEY_LONGITUDE, Double.doubleToLongBits(0.0)));
    }

    public String getTimeZoneId() {
        return prefs.getString(KEY_TIMEZONE_ID, "UTC");
    }

    public void setTimeZoneId(String id) {
        prefs.edit().putString(KEY_TIMEZONE_ID, id).apply();
    }

    public long getLastSyncTime() {
        return prefs.getLong(KEY_LAST_SYNC_TIME, 0);
    }

    public void setLastSyncTime(long millis) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, millis).apply();
    }

    public long getServerOffset() {
        return prefs.getLong(KEY_SERVER_OFFSET, 0);
    }

    public void setServerOffset(long offset) {
        prefs.edit().putLong(KEY_SERVER_OFFSET, offset).apply();
    }
}