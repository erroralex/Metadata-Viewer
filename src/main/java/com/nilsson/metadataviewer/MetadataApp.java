package com.nilsson.metadataviewer;

import com.nilsson.metadataviewer.ui.CustomTitleBar;
import com.nilsson.metadataviewer.ui.ResizeHelper;
import com.nilsson.metadataviewer.ui.RootLayout;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.util.List;

/**
 * Main Entry Point for the Metadata Viewer.
 * Directly initializes the persistent UI layout.
 */
public class MetadataApp extends Application {

    // Default target size, but we will limit this based on screen size below
    private static final int TARGET_WIDTH = 1280;
    private static final int TARGET_HEIGHT = 1024;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Style setup: Undecorated for custom title bar feel
            primaryStage.initStyle(StageStyle.UNDECORATED);

            // 1. Custom Title Bar
            CustomTitleBar titleBar = new CustomTitleBar(primaryStage, () -> System.exit(0));

            // 2. Main Root Layout (Persistent Sidebar + Content Area)
            RootLayout rootLayout = new RootLayout(primaryStage, titleBar);

            // Check for command line arguments (File drop on EXE)
            List<String> args = getParameters().getRaw();
            if (!args.isEmpty()) {
                File file = new File(args.get(0));
                if (file.exists() && (file.getName().endsWith(".png") ||
                        file.getName().endsWith(".jpg") ||
                        file.getName().endsWith(".webp"))) {
                    // pass to root layout to handle opening
                    rootLayout.openInitialFile(file);
                }
            }

            // 3. Main Wrapper
            BorderPane mainWrapper = new BorderPane();
            mainWrapper.setTop(titleBar);
            mainWrapper.setCenter(rootLayout);

            // --- SMART SIZING LOGIC ---
            // Get visual bounds (screen size minus taskbar)
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            // Use the smaller of: Default Target OR 90% of Screen Size
            double appWidth = Math.min(TARGET_WIDTH, screenBounds.getWidth() * 0.90);
            double appHeight = Math.min(TARGET_HEIGHT, screenBounds.getHeight() * 0.90);

            Scene scene = new Scene(mainWrapper, appWidth, appHeight);

            // Load Global CSS (Dark Theme by default)
            String cssPath = getClass().getResource("/dark-theme.css") != null
                    ? getClass().getResource("/dark-theme.css").toExternalForm()
                    : "";
            if (!cssPath.isEmpty()) {
                scene.getStylesheets().add(cssPath);
            }

            // Enable Resizing
            ResizeHelper.addResizeListener(primaryStage);

            primaryStage.setScene(scene);
            primaryStage.setTitle("Metadata Extractor by ALX");
            if (getClass().getResource("/icon.png") != null) {
                primaryStage.getIcons().add(new javafx.scene.image.Image(getClass().getResource("/icon.png").toExternalForm()));
            }

            // Show first to validate dimensions
            primaryStage.show();

            // --- CENTER ON SCREEN ---
            // Manually center the stage within the visual bounds
            primaryStage.setX((screenBounds.getWidth() - appWidth) / 2 + screenBounds.getMinX());
            primaryStage.setY((screenBounds.getHeight() - appHeight) / 2 + screenBounds.getMinY());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // FORCE UTF-8 ENCODING
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");
        System.setProperty("sun.stdout.encoding", "UTF-8");
        System.setProperty("sun.stderr.encoding", "UTF-8");
        launch(args);
    }
}