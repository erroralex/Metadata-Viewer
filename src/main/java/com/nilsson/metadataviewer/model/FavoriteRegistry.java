package com.nilsson.metadataviewer.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import javafx.scene.image.Image;

/**
 * Singleton Registry for managing favorite metadata entries.
 * Updated to use OS-standard persistent data directories for standalone deployment.
 */
public class FavoriteRegistry {

    // --- OS-Specific Path Logic ---
    private static String getDataDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        String appFolder = "MetadataViewer";

        if (os.contains("win")) {
            // Windows: %APPDATA%\MetadataViewer
            String appData = System.getenv("APPDATA");
            return (appData != null ? appData : userHome) + File.separator + appFolder;
        } else if (os.contains("mac")) {
            // macOS: ~/Library/Application Support/MetadataViewer
            return userHome + File.separator + "Library" + File.separator + "Application Support" + File.separator + appFolder;
        } else {
            // Linux/Other: ~/.local/share/MetadataViewer
            return userHome + File.separator + ".local" + File.separator + "share" + File.separator + appFolder;
        }
    }

    private static final String BASE_PATH = getDataDirectory();
    private static final String DATA_PATH = BASE_PATH + File.separator + "favorites.json";
    public static final String THUMB_PATH = BASE_PATH + File.separator + "thumbnails" + File.separator;

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private List<FavoriteData> favoritesList;

    // Private constructor for Singleton pattern
    private FavoriteRegistry() {
        this.favoritesList = loadFromDisk();
    }

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
        // Delete the physical thumbnail file if it exists
        if (favorite.getThumbnailPath() != null) {
            File thumbFile = new File(favorite.getThumbnailPath());
            if (thumbFile.exists()) {
                boolean deleted = thumbFile.delete();
                if (!deleted) {
                    System.err.println("Warning: Could not delete thumbnail file at " + favorite.getThumbnailPath());
                }
            }
        }

        // Remove the entry from the in-memory list
        favoritesList.removeIf(f -> f.getId().equals(favorite.getId()));

        // Persist the updated list to favorites.json
        saveToDisk();
    }

    private void saveToDisk() {
        try {
            File file = new File(DATA_PATH);
            // Ensure the directory structure (including the thumbnails folder) exists
            File dir = file.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Also ensure thumbnail directory exists
            File thumbDir = new File(THUMB_PATH);
            if (!thumbDir.exists()) {
                thumbDir.mkdirs();
            }

            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, favoritesList);
        } catch (IOException e) {
            System.err.println("Could not save favorites to " + DATA_PATH + ": " + e.getMessage());
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
            System.err.println("Could not load favorites from " + DATA_PATH + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public String saveThumbnail(Image image, String id) {
        if (image == null) return null;
        File thumbDir = new File(THUMB_PATH);
        if (!thumbDir.exists()) thumbDir.mkdirs();

        File thumbFile = new File(thumbDir, id + ".png");
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", thumbFile);
            return thumbFile.getAbsolutePath();
        } catch (IOException e) {
            System.err.println("Failed to save thumbnail: " + e.getMessage());
            return null;
        }
    }
}