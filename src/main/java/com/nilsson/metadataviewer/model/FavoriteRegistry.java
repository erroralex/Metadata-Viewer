package com.nilsson.metadataviewer.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton Registry for managing favorite metadata entries.
 * Updated to save FULL image copies to preserve metadata.
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

    public static final String IMAGES_PATH = BASE_PATH + File.separator + "saved_images" + File.separator;

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private List<FavoriteData> favoritesList;

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
        if (favorite.getThumbnailPath() != null) {
            File imgFile = new File(favorite.getThumbnailPath());
            if (imgFile.exists()) {
                imgFile.delete();
            }
        }
        favoritesList.removeIf(f -> f.getId().equals(favorite.getId()));
        saveToDisk();
    }

    private void saveToDisk() {
        try {
            File file = new File(DATA_PATH);
            File dir = file.getParentFile();
            if (!dir.exists()) dir.mkdirs();

            File imgDir = new File(IMAGES_PATH);
            if (!imgDir.exists()) imgDir.mkdirs();

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

    // Copies the original source file to the app's persistent storage instead of re-compressing the image to preserve metadata.
    public String saveImage(File sourceFile, String id) {
        if (sourceFile == null || !sourceFile.exists()) return null;

        File targetDir = new File(IMAGES_PATH);
        if (!targetDir.exists()) targetDir.mkdirs();

        // Preserve original extension (png, jpg, webp)
        String originalName = sourceFile.getName();
        String extension = "png";
        int i = originalName.lastIndexOf('.');
        if (i > 0) {
            extension = originalName.substring(i + 1);
        }

        File targetFile = new File(targetDir, id + "." + extension);

        try {
            // Perform actual file copy
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return targetFile.getAbsolutePath();
        } catch (IOException e) {
            System.err.println("Failed to copy image: " + e.getMessage());
            return null;
        }
    }
}