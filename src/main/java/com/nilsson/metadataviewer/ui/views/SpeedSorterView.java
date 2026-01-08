package com.nilsson.metadataviewer.ui.views;

import com.nilsson.metadataviewer.model.FavoriteRegistry;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class SpeedSorterView extends VBox {

    private File currentInputDir;
    private List<File> images = new ArrayList<>();
    private int currentIndex = 0;

    // Undo History
    private final Stack<SortAction> history = new Stack<>();

    // 5 Slots for Target Folders
    private final File[] targetFolders = new File[5];
    private final Button[] targetButtons = new Button[5];

    private final ImageView mainImageView = new ImageView();
    private final Label progressLabel = new Label("0 / 0");
    private final Label currentPathLabel = new Label("");
    private final Label infoLabel = new Label("Select Input Folder to Start");
    private final Label fullscreenHint = new Label("Click image for Fullscreen");
    private final Label messageLabel = new Label(""); // For Undo/Delete feedback

    public SpeedSorterView() {
        this.setPadding(new Insets(20));
        this.setSpacing(15);
        this.getStyleClass().add("content-view");
        this.setAlignment(Pos.TOP_CENTER);

        // --- Top Bar ---
        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Speed Sorter");
        title.getStyleClass().add("content-title");

        Button btnSelectInput = new Button("Select Input Folder");
        btnSelectInput.setGraphic(new FontIcon(FontAwesome.FOLDER_OPEN));
        btnSelectInput.getStyleClass().add("button");
        btnSelectInput.setOnAction(e -> selectInputFolder());

        currentPathLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px; -fx-font-style: italic;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        progressLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -app-text-muted;");

        topBar.getChildren().addAll(title, btnSelectInput, currentPathLabel, spacer, progressLabel);

        // --- Main Image Area ---
        StackPane imageContainer = new StackPane();
        imageContainer.setStyle("-fx-background-color: #0b0e14; -fx-background-radius: 8; -fx-border-color: #2d3748; -fx-border-radius: 8;");
        imageContainer.setMinSize(0, 0);
        VBox.setVgrow(imageContainer, Priority.ALWAYS);

        mainImageView.setPreserveRatio(true);
        mainImageView.setSmooth(true);

        mainImageView.fitWidthProperty().bind(Bindings.max(0, imageContainer.widthProperty().subtract(20)));
        mainImageView.fitHeightProperty().bind(Bindings.max(0, imageContainer.heightProperty().subtract(20)));

        infoLabel.setStyle("-fx-text-fill: #718096; -fx-font-size: 16px;");

        fullscreenHint.setStyle("-fx-text-fill: rgba(255,255,255,0.3); -fx-font-size: 11px; -fx-padding: 5;");
        fullscreenHint.setVisible(false);
        StackPane.setAlignment(fullscreenHint, Pos.BOTTOM_RIGHT);

        // Feedback Label (Undo/Delete)
        messageLabel.setStyle("-fx-text-fill: #48bb78; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: rgba(0,0,0,0.5); -fx-padding: 5 10; -fx-background-radius: 5;");
        messageLabel.setVisible(false);
        StackPane.setAlignment(messageLabel, Pos.TOP_CENTER);
        StackPane.setMargin(messageLabel, new Insets(10));

        imageContainer.getChildren().addAll(infoLabel, mainImageView, fullscreenHint, messageLabel);

        Tooltip.install(imageContainer, new Tooltip("Click to view Fullscreen"));
        imageContainer.setCursor(javafx.scene.Cursor.HAND);

        imageContainer.setOnMouseClicked(e -> {
            if (!images.isEmpty() && currentIndex < images.size()) {
                showFullScreenImage(images.get(currentIndex));
            }
        });

        // --- Bottom Control Bar ---
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(10));

        for (int i = 0; i < 5; i++) {
            final int index = i;
            VBox box = new VBox(5);
            box.setAlignment(Pos.CENTER);

            Label keyLabel = new Label("Key [" + (i + 1) + "]");
            keyLabel.setStyle("-fx-text-fill: -app-accent; -fx-font-weight: bold;");

            Button btn = new Button("Set Folder");
            btn.getStyleClass().add("button");
            btn.setPrefWidth(120);
            btn.setOnAction(e -> selectTargetFolder(index));

            targetButtons[i] = btn;
            box.getChildren().addAll(keyLabel, btn);
            controls.getChildren().add(box);
        }

        VBox extraControls = new VBox(5);
        extraControls.setAlignment(Pos.CENTER_LEFT);
        Label deleteLabel = new Label("DEL / X : Recycle Bin");
        deleteLabel.setStyle("-fx-text-fill: #e53e3e; -fx-font-size: 11px;");
        Label undoLabel = new Label("Ctrl+Z : Undo Move");
        undoLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        Label spaceLabel = new Label("SPACE : Skip");
        spaceLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");

        extraControls.getChildren().addAll(deleteLabel, undoLabel, spaceLabel);
        controls.getChildren().add(extraControls);

        this.getChildren().addAll(topBar, imageContainer, controls);

        // --- Keyboard Handling ---
        this.setOnKeyPressed(this::handleKeyPress);

        Platform.runLater(this::loadPersistedSettings);
    }

    // --- Action History Class for Undo ---
    private static class SortAction {
        File source;
        File destination;
        boolean wasDelete;

        public SortAction(File source, File destination, boolean wasDelete) {
            this.source = source;
            this.destination = destination;
            this.wasDelete = wasDelete;
        }
    }

    // --- Core Logic ---

    private void handleKeyPress(KeyEvent e) {
        // Handle Undo (Ctrl+Z)
        if (new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN).match(e)) {
            undoLastAction();
            return;
        }

        if (images.isEmpty() || currentIndex >= images.size()) return;

        switch (e.getCode()) {
            case DIGIT1: moveImage(0); break;
            case DIGIT2: moveImage(1); break;
            case DIGIT3: moveImage(2); break;
            case DIGIT4: moveImage(3); break;
            case DIGIT5: moveImage(4); break;
            case DELETE:
            case X:
                deleteToRecycleBin();
                break;
            case SPACE: skipImage(); break;
            case RIGHT: skipImage(); break;
            case LEFT:
                if (currentIndex > 0) {
                    currentIndex--;
                    updateUI();
                }
                break;
        }
    }

    private void deleteToRecycleBin() {
        File source = images.get(currentIndex);
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
            try {
                if (Desktop.getDesktop().moveToTrash(source)) {
                    // Treat delete as a "move to void" for history
                    // Note: We cannot Programmatically Undo a Recycle Bin move easily in Java
                    // So we don't add it to history to avoid false hope, or we add it but show error on undo.
                    showMessage("Moved to Recycle Bin", "#e53e3e");
                    images.remove(currentIndex);
                    updateUI();
                } else {
                    showMessage("Recycle Bin not supported on this OS", "orange");
                }
            } catch (Exception e) {
                showMessage("Error deleting file", "red");
                e.printStackTrace();
            }
        } else {
            showMessage("Recycle Bin not supported", "orange");
        }
    }

    private void moveImage(int targetIndex) {
        if (targetFolders[targetIndex] == null) return;

        File source = images.get(currentIndex);
        File dest = new File(targetFolders[targetIndex], source.getName());

        // Handle duplicates
        if (dest.exists()) {
            String name = source.getName();
            String nameNoExt = name.substring(0, name.lastIndexOf('.'));
            String ext = name.substring(name.lastIndexOf('.'));
            dest = new File(targetFolders[targetIndex], nameNoExt + "_" + System.currentTimeMillis() + ext);
        }

        try {
            Files.move(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Push to history
            history.push(new SortAction(source, dest, false)); // false = not a delete

            images.remove(currentIndex);
            showMessage("Moved to " + targetFolders[targetIndex].getName(), "#48bb78");
            updateUI();
        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Error moving file", "red");
        }
    }

    private void undoLastAction() {
        if (history.isEmpty()) {
            showMessage("Nothing to undo", "orange");
            return;
        }

        SortAction lastAction = history.pop();
        if (lastAction.wasDelete) {
            showMessage("Cannot undo Recycle Bin delete", "orange");
            return;
        }

        if (lastAction.destination.exists()) {
            try {
                // Move back
                Files.move(lastAction.destination.toPath(), lastAction.source.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Add back to images list at current index
                images.add(currentIndex, lastAction.source);
                showMessage("Undid last move", "#4299e1");
                updateUI();
            } catch (IOException e) {
                e.printStackTrace();
                showMessage("Failed to undo move", "red");
            }
        } else {
            showMessage("File not found to undo", "red");
        }
    }

    private void showMessage(String msg, String colorHex) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: rgba(0,0,0,0.8); -fx-padding: 5 10; -fx-background-radius: 5;");
        messageLabel.setVisible(true);

        // Auto hide after 1.5s
        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException e) {}
            Platform.runLater(() -> messageLabel.setVisible(false));
        }).start();
    }

    private void skipImage() {
        currentIndex++;
        updateUI();
    }

    private void loadImages() {
        if (currentInputDir == null) return;
        File[] files = currentInputDir.listFiles((d, name) -> {
            String low = name.toLowerCase();
            return low.endsWith(".png") || low.endsWith(".jpg") || low.endsWith(".jpeg") || low.endsWith(".webp");
        });
        images.clear();
        if (files != null) {
            Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            images = new ArrayList<>(Arrays.asList(files));
        }
        currentIndex = 0;
        updateUI();
        this.requestFocus();
    }

    private void updateUI() {
        if (images.isEmpty()) {
            infoLabel.setText("No images found in folder");
            mainImageView.setImage(null);
            progressLabel.setText("0 / 0");
            fullscreenHint.setVisible(false);
            return;
        }
        if (currentIndex >= images.size()) {
            infoLabel.setText("All images processed!");
            mainImageView.setImage(null);
            progressLabel.setText(images.size() + " / " + images.size());
            fullscreenHint.setVisible(false);
            return;
        }
        File file = images.get(currentIndex);
        infoLabel.setText("");
        fullscreenHint.setVisible(true);
        loadImageIntoView(file, mainImageView);
        progressLabel.setText((currentIndex + 1) + " / " + images.size());
    }

    private void loadPersistedSettings() {
        FavoriteRegistry reg = FavoriteRegistry.getInstance();
        String savedInput = reg.getSetting("speed_input_dir", null);
        if (savedInput != null) {
            File f = new File(savedInput);
            if (f.exists() && f.isDirectory()) {
                currentInputDir = f;
                currentPathLabel.setText(f.getAbsolutePath());
                loadImages();
            }
        }
        for (int i = 0; i < 5; i++) {
            String savedTarget = reg.getSetting("speed_target_" + i, null);
            if (savedTarget != null) {
                File f = new File(savedTarget);
                if (f.exists() && f.isDirectory()) {
                    targetFolders[i] = f;
                    targetButtons[i].setText(f.getName());
                    targetButtons[i].setTooltip(new Tooltip(f.getAbsolutePath()));
                }
            }
        }
    }

    private void selectInputFolder() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Input Folder");
        if (currentInputDir != null) dc.setInitialDirectory(currentInputDir);
        File dir = dc.showDialog(this.getScene().getWindow());
        if (dir != null) {
            currentInputDir = dir;
            currentPathLabel.setText(dir.getAbsolutePath());
            FavoriteRegistry.getInstance().setSetting("speed_input_dir", dir.getAbsolutePath());
            loadImages();
        }
    }

    private void selectTargetFolder(int index) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Target Folder " + (index + 1));
        if (targetFolders[index] != null) dc.setInitialDirectory(targetFolders[index]);
        File dir = dc.showDialog(this.getScene().getWindow());
        if (dir != null) {
            targetFolders[index] = dir;
            targetButtons[index].setText(dir.getName());
            targetButtons[index].setTooltip(new Tooltip(dir.getAbsolutePath()));
            FavoriteRegistry.getInstance().setSetting("speed_target_" + index, dir.getAbsolutePath());
        }
    }

    private void showFullScreenImage(File file) {
        Stage stage = new Stage();
        stage.initOwner(this.getScene().getWindow());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        ImageView fullView = new ImageView();
        fullView.setPreserveRatio(true);
        fullView.setSmooth(true);
        loadImageIntoView(file, fullView);
        fullView.fitWidthProperty().bind(stage.widthProperty());
        fullView.fitHeightProperty().bind(stage.heightProperty());
        StackPane root = new StackPane(fullView);
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9);");
        root.setOnMouseClicked(e -> stage.close());
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) stage.close();
        });
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.show();
    }

    private void loadImageIntoView(File f, ImageView view) {
        try {
            Image img = new Image(f.toURI().toString(), 0, 0, true, true, true);
            if (img.isError()) {
                try {
                    byte[] bytes = Files.readAllBytes(f.toPath());
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
                        Image streamImg = new Image(bis, 0, 0, true, true);
                        if (!streamImg.isError()) {
                            view.setImage(streamImg);
                            return;
                        }
                    }
                } catch (Exception ignored) {}
                try {
                    BufferedImage bImg = ImageIO.read(f);
                    if (bImg != null) {
                        Image fxImg = SwingFXUtils.toFXImage(bImg, null);
                        view.setImage(fxImg);
                        return;
                    }
                } catch (Exception ignored) {}
                view.setImage(null);
            } else {
                view.setImage(img);
            }
        } catch (Exception e) {
            e.printStackTrace();
            view.setImage(null);
        }
    }
}