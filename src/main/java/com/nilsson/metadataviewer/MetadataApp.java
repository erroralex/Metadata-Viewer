package com.nilsson.metadataviewer;

import com.nilsson.metadataviewer.ui.CustomTitleBar;
import com.nilsson.metadataviewer.ui.RootLayout;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Main Entry Point for the Metadata Viewer.
 * Directly initializes the persistent UI layout.
 */
public class MetadataApp extends Application {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 1024;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Style setup: Undecorated for custom title bar feel
            primaryStage.initStyle(StageStyle.UNDECORATED);

            // 1. Custom Title Bar
            CustomTitleBar titleBar = new CustomTitleBar(primaryStage, () -> System.exit(0));

            // 2. Main Root Layout (Persistent Sidebar + Content Area)
            RootLayout rootLayout = new RootLayout(primaryStage, titleBar);

            // 3. Main Wrapper
            BorderPane mainWrapper = new BorderPane();
            mainWrapper.setTop(titleBar);
            mainWrapper.setCenter(rootLayout);

            Scene scene = new Scene(mainWrapper, WIDTH, HEIGHT);

            // Load Global CSS (Dark Theme by default)
            String cssPath = getClass().getResource("/dark-theme.css") != null
                    ? getClass().getResource("/dark-theme.css").toExternalForm()
                    : "";
            if (!cssPath.isEmpty()) {
                scene.getStylesheets().add(cssPath);
            }

            primaryStage.setScene(scene);
            primaryStage.setTitle("Metadata Extractor by ALX");
            primaryStage.getIcons().add(new javafx.scene.image.Image(getClass().getResource("/icon.png").toExternalForm()));
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}