package com.nilsson.metadataviewer.ui.views;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ScrubView extends ScrollPane {

    private final ImageView previewImageView = new ImageView();
    private final StackPane previewContainer = new StackPane();
    private final Label statusLabel = new Label("Ready to scrub");
    private File currentFile;

    public ScrubView() {
        this.setFitToWidth(true);
        this.getStyleClass().add("content-view");
        this.setHbarPolicy(ScrollBarPolicy.NEVER);

        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setAlignment(Pos.TOP_CENTER);

        // Header
        Label title = new Label("Metadata Scrubber");
        title.getStyleClass().add("content-title");
        Label subtitle = new Label("Remove all hidden metadata (EXIF, Prompts, Workflow) for privacy.");
        subtitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");

        // Drop Zone
        VBox dropZone = createDropZone();

        // Preview Area with Hint
        VBox previewWrapper = new VBox(5);
        previewWrapper.setAlignment(Pos.CENTER);

        Label fullScreenHint = new Label("Click to Fullscreen");
        fullScreenHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

        setupPreviewContainer();
        previewWrapper.getChildren().addAll(fullScreenHint, previewContainer);

        // Actions
        Button btnExport = new Button("Export Clean Copy");
        btnExport.setGraphic(new FontIcon(FontAwesome.SHIELD));
        btnExport.getStyleClass().add("button");
        btnExport.setDisable(true);
        btnExport.setOnAction(e -> exportCleanImage());

        // Enable button only when file is loaded
        previewImageView.imageProperty().addListener((obs, old, img) -> btnExport.setDisable(img == null));

        statusLabel.setStyle("-fx-text-fill: #94a3b8;");

        container.getChildren().addAll(title, subtitle, dropZone, previewWrapper, statusLabel, btnExport);
        this.setContent(container);
    }

    private void setupPreviewContainer() {
        previewContainer.getStyleClass().add("income-stats-box");
        previewContainer.setPrefSize(300, 300);
        previewContainer.setMaxSize(300, 300);

        previewImageView.setFitWidth(280);
        previewImageView.setFitHeight(280);
        previewImageView.setPreserveRatio(true);
        previewImageView.setSmooth(true);

        previewContainer.getChildren().add(previewImageView);

        // --- Fullscreen Logic ---
        previewContainer.setCursor(javafx.scene.Cursor.HAND);
        previewContainer.setOnMouseClicked(e -> {
            if (currentFile != null && currentFile.exists()) {
                showFullScreenImage(currentFile);
            }
        });
    }

    /**
     * Launches a modal fullscreen stage to view the image in high resolution.
     */
    private void showFullScreenImage(File file) {
        Stage stage = new Stage();
        stage.initOwner(this.getScene().getWindow());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);

        // Load full resolution image
        ImageView fullView = new ImageView(new Image(file.toURI().toString()));
        fullView.setPreserveRatio(true);
        fullView.setSmooth(true);

        // Bind image size to stage size so it scales
        fullView.fitWidthProperty().bind(stage.widthProperty());
        fullView.fitHeightProperty().bind(stage.heightProperty());

        StackPane root = new StackPane(fullView);
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9);"); // Dimmed background

        // Close on Click
        root.setOnMouseClicked(e -> stage.close());

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);

        // Close on ESC Key
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });

        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.show();
    }

    private VBox createDropZone() {
        VBox dz = new VBox(10);
        dz.getStyleClass().add("drop-zone");
        dz.setMinHeight(120);
        dz.setAlignment(Pos.CENTER);
        dz.getChildren().addAll(new FontIcon(FontAwesome.CLOUD_UPLOAD), new Label("Drop Image to Scrub"));

        dz.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
                dz.getStyleClass().add("drop-zone-active");
            }
            e.consume();
        });
        dz.setOnDragExited(e -> dz.getStyleClass().remove("drop-zone-active"));
        dz.setOnDragDropped(e -> {
            if (e.getDragboard().hasFiles()) {
                loadFile(e.getDragboard().getFiles().get(0));
                e.setDropCompleted(true);
            }
            dz.getStyleClass().remove("drop-zone-active");
            e.consume();
        });
        return dz;
    }

    private void loadFile(File file) {
        this.currentFile = file;
        try {
            Image img = new Image(file.toURI().toString());
            previewImageView.setImage(img);
            statusLabel.setText("Loaded: " + file.getName());
            statusLabel.setStyle("-fx-text-fill: -app-accent;");
        } catch (Exception e) {
            statusLabel.setText("Error loading image");
        }
    }

    private void exportCleanImage() {
        if (currentFile == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Scrubbed Image");
        fileChooser.setInitialFileName("clean_" + currentFile.getName());

        if (currentFile.getParentFile() != null) {
            fileChooser.setInitialDirectory(currentFile.getParentFile());
        }

        File dest = fileChooser.showSaveDialog(this.getScene().getWindow());
        if (dest != null) {
            try {
                // Reading into BufferedImage and writing back strips metadata
                Image fxImg = previewImageView.getImage();
                BufferedImage bImg = SwingFXUtils.fromFXImage(fxImg, null);

                String name = dest.getName().toLowerCase();
                String format = "png";
                if (name.endsWith(".jpg") || name.endsWith(".jpeg")) format = "jpg";
                else if (name.endsWith(".bmp")) format = "bmp";

                ImageIO.write(bImg, format, dest);

                statusLabel.setText("Success! Saved to " + dest.getName());
                statusLabel.setStyle("-fx-text-fill: #4ade80;");
            } catch (IOException e) {
                statusLabel.setText("Export failed: " + e.getMessage());
                statusLabel.setStyle("-fx-text-fill: -app-warning-red;");
            }
        }
    }
}