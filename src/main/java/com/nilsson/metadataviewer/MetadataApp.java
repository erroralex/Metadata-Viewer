package com.nilsson.metadataviewer;

import com.nilsson.metadataviewer.ui.CustomTitleBar;
import com.nilsson.metadataviewer.ui.DevCredit;
import com.nilsson.metadataviewer.ui.ResizeHelper;
import com.nilsson.metadataviewer.ui.views.ExtractorView;
import com.nilsson.metadataviewer.ui.views.SettingsDialog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.util.List;

public class MetadataApp extends Application {

    private static final int TARGET_WIDTH = 1280;
    private static final int TARGET_HEIGHT = 1024;

    @Override
    public void start(Stage primaryStage) {
        try {
            primaryStage.initStyle(StageStyle.UNDECORATED);

            ExtractorView extractorView = new ExtractorView();
            CustomTitleBar titleBar = new CustomTitleBar(
                    primaryStage,
                    () -> System.exit(0),
                    () -> SettingsDialog.show(primaryStage)
            );

            HBox footer = new HBox(DevCredit.create());
            footer.setAlignment(Pos.BOTTOM_LEFT);
            footer.setPadding(new Insets(0, 0, 8, 15));

            BorderPane mainWrapper = new BorderPane();
            mainWrapper.setTop(titleBar);
            mainWrapper.setCenter(extractorView);
            mainWrapper.setBottom(footer);

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            double appWidth = Math.min(TARGET_WIDTH, screenBounds.getWidth() * 0.90);
            double appHeight = Math.min(TARGET_HEIGHT, screenBounds.getHeight() * 0.90);

            Scene scene = new Scene(mainWrapper, appWidth, appHeight);

            String cssPath = getClass().getResource("/latent-theme.css") != null
                    ? getClass().getResource("/latent-theme.css").toExternalForm()
                    : "";
            if (!cssPath.isEmpty()) {
                scene.getStylesheets().add(cssPath);
            }

            ResizeHelper.addResizeListener(primaryStage);

            primaryStage.setScene(scene);
            primaryStage.setTitle("Metadata Viewer by ALX");
            if (getClass().getResource("/icon.png") != null) {
                primaryStage.getIcons().add(new javafx.scene.image.Image(getClass().getResource("/icon.png").toExternalForm()));
            }

            // Show FIRST to ensure Scene and Window are ready
            primaryStage.show();

            primaryStage.setX((screenBounds.getWidth() - appWidth) / 2 + screenBounds.getMinX());
            primaryStage.setY((screenBounds.getHeight() - appHeight) / 2 + screenBounds.getMinY());

            List<String> args = getParameters().getRaw();
            if (!args.isEmpty()) {
                File file = new File(args.get(0));
                String lowerName = file.getName().toLowerCase();

                // Case-insensitive check + jpeg support
                if (file.exists() && (lowerName.endsWith(".png") ||
                        lowerName.endsWith(".jpg") ||
                        lowerName.endsWith(".jpeg") ||
                        lowerName.endsWith(".webp"))) {

                    // Run on UI thread to be safe
                    Platform.runLater(() -> extractorView.process(file));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");
        System.setProperty("sun.stdout.encoding", "UTF-8");
        System.setProperty("sun.stderr.encoding", "UTF-8");
        launch(args);
    }
}