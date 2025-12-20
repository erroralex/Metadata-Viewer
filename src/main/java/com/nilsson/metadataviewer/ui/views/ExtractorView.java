package com.nilsson.metadataviewer.ui.views;

import com.nilsson.metadataviewer.model.FavoriteData;
import com.nilsson.metadataviewer.model.FavoriteRegistry;
import com.nilsson.metadataviewer.service.MetadataService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import java.io.File;
import java.util.Map;

/**
 * Main View for dragging images and viewing metadata.
 */
public class ExtractorView extends VBox {

    private final MetadataService metadataService = new MetadataService();
    private final TextArea promptArea = new TextArea();
    private final Label modelLabel = new Label("Model: -");
    private final Label stepsLabel = new Label("Steps: -");
    private final Label loraLabel = new Label("Loras: -");

    private Map<String, String> currentData;

    public ExtractorView() {
        this.setPadding(new Insets(30));
        this.setSpacing(20);
        this.getStyleClass().add("content-view");

        Label title = new Label("AI Metadata Extractor");
        title.getStyleClass().add("content-title");

        // Drag and Drop Zone
        VBox dropZone = new VBox(new Label("Drop AI Image Here (PNG/JPEG)"));
        dropZone.setAlignment(Pos.CENTER);
        dropZone.setMinHeight(150);
        dropZone.getStyleClass().add("drop-zone");

        setupDragAndDrop(dropZone);

        // Results Area
        promptArea.setEditable(false);
        promptArea.setWrapText(true);
        promptArea.setPrefHeight(200);
        promptArea.setPromptText("Extracted prompt will appear here...");

        HBox statsBox = new HBox(20, modelLabel, stepsLabel, loraLabel);
        statsBox.setAlignment(Pos.CENTER_LEFT);

        // Actions
        Button btnCopy = new Button("Copy Prompt");
        btnCopy.setOnAction(e -> {
            final javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            final javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(promptArea.getText());
            clipboard.setContent(content);
        });

        Button btnFav = new Button("Save as Favorite");
        btnFav.setOnAction(e -> handleSaveFavorite());

        HBox actionBox = new HBox(15, btnCopy, btnFav);

        this.getChildren().addAll(title, dropZone, new Label("Prompt:"), promptArea, statsBox, actionBox);
    }

    private void setupDragAndDrop(VBox zone) {
        zone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        zone.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getDragboard().hasFiles()) {
                File file = event.getDragboard().getFiles().get(0);
                processFile(file);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void processFile(File file) {
        currentData = metadataService.getExtractedData(file);
        promptArea.setText(currentData.getOrDefault("Prompt", ""));
        modelLabel.setText("Model: " + currentData.getOrDefault("Model", "N/A"));
        stepsLabel.setText("Steps: " + currentData.getOrDefault("Steps", "N/A"));
        loraLabel.setText("Loras: " + currentData.getOrDefault("Loras", "N/A"));
    }

    private void handleSaveFavorite() {
        if (currentData == null) return;

        TextInputDialog dialog = new TextInputDialog("My Favorite Prompt");
        dialog.setTitle("Save Favorite");
        dialog.setHeaderText("Enter a name for this favorite:");
        dialog.showAndWait().ifPresent(name -> {
            FavoriteData fav = new FavoriteData(
                    name,
                    currentData.getOrDefault("Prompt", ""),
                    currentData.getOrDefault("Model", ""),
                    currentData.getOrDefault("Steps", ""),
                    currentData.getOrDefault("Loras", "")
            );
            FavoriteRegistry.getInstance().addFavorite(fav);
        });
    }
}