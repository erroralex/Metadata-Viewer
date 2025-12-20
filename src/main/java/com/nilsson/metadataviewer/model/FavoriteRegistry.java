package com.nilsson.metadataviewer.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton Registry for managing favorite metadata entries.
 * Handles in-memory state and JSON persistence.
 */
public class FavoriteRegistry {

    private static final String DATA_PATH = "src/main/resources/data/json/favorites.json";
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private List<FavoriteData> favoritesList;

    // Private constructor for Singleton pattern
    private FavoriteRegistry() {
        this.favoritesList = loadFromDisk();
    }

    /**
     * The missing getInstance method to provide the singleton instance.
     */
    public static FavoriteRegistry getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final FavoriteRegistry INSTANCE = new FavoriteRegistry();
    }

    public List<FavoriteData> getFavorites() {
        return new ArrayList<>(favoritesList);
    }

    public void addFavorite(FavoriteData favorite) {
        favoritesList.add(favorite);
        saveToDisk();
    }

    public void removeFavorite(FavoriteData favorite) {
        favoritesList.removeIf(f -> f.getId().equals(favorite.getId()));
        saveToDisk();
    }

    private void saveToDisk() {
        try {
            File file = new File(DATA_PATH);
            // Ensure directory exists
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, favoritesList);
        } catch (IOException e) {
            System.err.println("Could not save favorites: " + e.getMessage());
        }
    }

    private List<FavoriteData> loadFromDisk() {
        File file = new File(DATA_PATH);
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(file, new TypeReference<List<FavoriteData>>() {});
        } catch (IOException e) {
            System.err.println("Could not load favorites: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}