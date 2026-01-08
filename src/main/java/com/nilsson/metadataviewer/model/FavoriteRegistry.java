package com.nilsson.metadataviewer.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoriteRegistry {

    // --- DIRECTORY CONSTANTS ---
    private static final String APP_DIR = "data";
    private static final String FAVORITES_DIR = APP_DIR + File.separator + "favorites";
    private static final String IMAGES_DIR = FAVORITES_DIR + File.separator + "images";
    private static final String DATA_FILE = FAVORITES_DIR + File.separator + "favorites.json";
    private static final String SETTINGS_FILE = APP_DIR + File.separator + "settings.json";

    // --- SINGLETON INSTANCE ---
    private static final FavoriteRegistry INSTANCE = new FavoriteRegistry();

    private List<FavoriteData> favorites = new ArrayList<>();
    private Map<String, String> settings = new HashMap<>(); // Store user prefs
    private final ObjectMapper mapper = new ObjectMapper();

    private FavoriteRegistry() {
        new File(APP_DIR).mkdirs();
        new File(FAVORITES_DIR).mkdirs();
        new File(IMAGES_DIR).mkdirs();

        loadFavorites();
        loadSettings(); // Load prefs
    }

    public static FavoriteRegistry getInstance() {
        return INSTANCE;
    }

    public List<FavoriteData> getFavorites() {
        return favorites;
    }

    // --- SETTINGS METHODS ---
    public String getSetting(String key, String defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }

    public void setSetting(String key, String value) {
        settings.put(key, value);
        saveSettings();
    }
    // ------------------------

    public void addFavorite(FavoriteData data) {
        favorites.add(data);
        saveFavorites();
    }

    public void removeFavorite(FavoriteData data) {
        favorites.remove(data);
        if (data.getThumbnailPath() != null) {
            new File(data.getThumbnailPath()).delete();
        }
        saveFavorites();
    }

    public String saveImage(File sourceFile, String id) {
        try {
            String ext = getFileExtension(sourceFile);
            File destFile = new File(IMAGES_DIR, id + "." + ext);
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return destFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        return (lastIndexOf == -1) ? "jpg" : name.substring(lastIndexOf + 1);
    }

    private void saveFavorites() {
        try {
            mapper.writeValue(new File(DATA_FILE), favorites);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadFavorites() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try {
                favorites = mapper.readValue(file, new TypeReference<List<FavoriteData>>() {});
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveSettings() {
        try {
            mapper.writeValue(new File(SETTINGS_FILE), settings);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSettings() {
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            try {
                settings = mapper.readValue(file, new TypeReference<Map<String, String>>() {});
            } catch (IOException e) {
                // Ignore if file doesn't exist yet
            }
        }
    }
}