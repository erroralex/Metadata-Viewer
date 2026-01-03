package com.nilsson.metadataviewer.ui.views;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpeedSorterView extends BorderPane {

    private final ImageView imageView;
    private final Label statusLabel;
    private final Label inputFolderLabel;
    private final Button[] folderButtons = new Button[5];
    private final File[] targetFolders = new File[5];

    private File inputFolder;
    private List<File> imageFiles = new ArrayList<>();
    private int currentIndex = 0;

    public SpeedSorterView() {
        // Apply CSS class for background.
        this.getStyleClass().add("content-view");

        // --- TOP BAR ---
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15));
        topBar.setAlignment(Pos.CENTER_LEFT);
        // Use borders only, no background override
        topBar.setStyle("-fx-border-color: -app-border-subtle; -fx-border-width: 0 0 1 0;");

        Button openBtn = new Button("Select Input Folder");
        openBtn.setGraphic(new FontIcon(FontAwesome.FOLDER_OPEN));
        openBtn.getStyleClass().add("button");
        openBtn.setOnAction(e -> selectInputFolder());

        inputFolderLabel = new Label("No folder selected");
        inputFolderLabel.setStyle("-fx-text-fill: -app-text-muted;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel = new Label("0 / 0");
        statusLabel.getStyleClass().add("app-title");

        topBar.getChildren().addAll(openBtn, inputFolderLabel, spacer, statusLabel);
        setTop(topBar);

        // --- CENTER (IMAGE PREVIEW + HINT) ---
        VBox centerLayout = new VBox(10);
        centerLayout.setAlignment(Pos.CENTER);
        centerLayout.setPadding(new Insets(20));

        // Fullscreen Hint
        Label fullScreenHint = new Label("Click image for Fullscreen");
        fullScreenHint.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-text-muted; -fx-font-style: italic;");

        StackPane imageContainer = new StackPane();
        VBox.setVgrow(imageContainer, Priority.ALWAYS);

        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // Bind size to container
        imageView.fitWidthProperty().bind(imageContainer.widthProperty());
        imageView.fitHeightProperty().bind(imageContainer.heightProperty());

        // CLICK TO FULLSCREEN
        imageView.setCursor(javafx.scene.Cursor.HAND);
        imageView.setOnMouseClicked(e -> {
            if (!imageFiles.isEmpty() && currentIndex < imageFiles.size()) {
                showFullScreenImage(imageFiles.get(currentIndex));
            }
        });

        imageContainer.getChildren().add(imageView);
        centerLayout.getChildren().addAll(fullScreenHint, imageContainer);
        setCenter(centerLayout);

        // --- BOTTOM (CONTROLS) ---
        HBox bottomBar = new HBox(15);
        bottomBar.setPadding(new Insets(20));
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setStyle("-fx-border-color: -app-border-subtle; -fx-border-width: 1 0 0 0;");

        for (int i = 0; i < 5; i++) {
            final int index = i;
            VBox slot = new VBox(5);
            slot.setAlignment(Pos.CENTER);
            slot.setPadding(new Insets(10));
            // Use card background variable from CSS
            slot.setStyle("-fx-background-color: -app-bg-card; -fx-background-radius: 8; -fx-border-color: -app-border-subtle; -fx-border-radius: 8;");
            slot.setPrefWidth(160);

            Label keyLabel = new Label("Key [" + (i + 1) + "]");
            keyLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -app-accent;");

            Button selectBtn = new Button("Set Folder");
            selectBtn.setGraphic(new FontIcon(FontAwesome.FOLDER));
            selectBtn.getStyleClass().add("button");
            selectBtn.setMaxWidth(Double.MAX_VALUE);
            selectBtn.setOnAction(e -> selectTargetFolder(index));

            folderButtons[i] = selectBtn;

            slot.getChildren().addAll(keyLabel, selectBtn);
            bottomBar.getChildren().add(slot);
        }

        Label hint = new Label("SPACE to Skip");
        hint.setStyle("-fx-font-size: 14px; -fx-text-fill: -app-text-muted; -fx-padding: 0 0 0 20;");
        bottomBar.getChildren().add(hint);

        setBottom(bottomBar);

        // Keyboard Handling
        this.setFocusTraversable(true);
        this.setOnKeyPressed(this::handleKeyPress);
        this.setOnMouseClicked(e -> this.requestFocus());
    }

    private void handleKeyPress(KeyEvent event) {
        if (imageFiles.isEmpty()) return;

        KeyCode code = event.getCode();
        if (code == KeyCode.SPACE || code == KeyCode.RIGHT) {
            nextImage();
        } else if (code == KeyCode.DIGIT1 || code == KeyCode.NUMPAD1) {
            moveFile(0);
        } else if (code == KeyCode.DIGIT2 || code == KeyCode.NUMPAD2) {
            moveFile(1);
        } else if (code == KeyCode.DIGIT3 || code == KeyCode.NUMPAD3) {
            moveFile(2);
        } else if (code == KeyCode.DIGIT4 || code == KeyCode.NUMPAD4) {
            moveFile(3);
        } else if (code == KeyCode.DIGIT5 || code == KeyCode.NUMPAD5) {
            moveFile(4);
        }
    }

    private void showFullScreenImage(File file) {
        Stage stage = new Stage();
        stage.initOwner(this.getScene().getWindow());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);

        ImageView fullView = new ImageView(new Image(file.toURI().toString()));
        fullView.setPreserveRatio(true);
        fullView.setSmooth(true);

        fullView.fitWidthProperty().bind(stage.widthProperty());
        fullView.fitHeightProperty().bind(stage.heightProperty());

        StackPane root = new StackPane(fullView);
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.95);");
        root.setOnMouseClicked(e -> stage.close());

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.SPACE) {
                stage.close();
            }
        });

        stage.setOnHidden(e -> this.requestFocus());

        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.show();
    }

    private void selectInputFolder() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Input Folder");
        File selected = dc.showDialog(getScene().getWindow());
        if (selected != null) {
            this.inputFolder = selected;
            this.inputFolderLabel.setText(selected.getName());
            loadImages();
        }
    }

    private void selectTargetFolder(int index) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Target Folder " + (index + 1));
        File selected = dc.showDialog(getScene().getWindow());
        if (selected != null) {
            targetFolders[index] = selected;
            folderButtons[index].setText(selected.getName());
            folderButtons[index].setTooltip(new Tooltip(selected.getAbsolutePath()));
        }
    }

    private void loadImages() {
        if (inputFolder == null) return;

        try (Stream<Path> paths = Files.list(inputFolder.toPath())) {
            imageFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> isImage(p.toFile()))
                    .map(Path::toFile)
                    .sorted(Comparator.comparing(File::getName))
                    .collect(Collectors.toCollection(ArrayList::new));

            currentIndex = 0;
            updateView();
            Platform.runLater(this::requestFocus);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isImage(File f) {
        String name = f.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg") || name.endsWith(".webp");
    }

    private void updateView() {
        if (imageFiles.isEmpty()) {
            statusLabel.setText("Done / Empty");
            imageView.setImage(null);
            return;
        }

        if (currentIndex >= imageFiles.size()) currentIndex = 0;

        File current = imageFiles.get(currentIndex);
        statusLabel.setText((currentIndex + 1) + " / " + imageFiles.size());

        try {
            imageView.setImage(new Image(current.toURI().toString()));
        } catch (Exception e) {
            System.err.println("Could not load image: " + current);
        }
    }

    private void nextImage() {
        if (imageFiles.isEmpty()) return;
        currentIndex++;
        if (currentIndex >= imageFiles.size()) currentIndex = 0;
        updateView();
    }

    private void moveFile(int targetIndex) {
        if (targetFolders[targetIndex] == null || imageFiles.isEmpty()) return;

        File source = imageFiles.get(currentIndex);
        File targetDir = targetFolders[targetIndex];
        File dest = new File(targetDir, source.getName());

        try {
            Files.move(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            imageFiles.remove(currentIndex);

            if (currentIndex >= imageFiles.size()) currentIndex = 0;

            updateView();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}