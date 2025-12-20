package com.nilsson.metadataviewer.ui.views;

import com.nilsson.metadataviewer.model.FavoriteData;
import com.nilsson.metadataviewer.model.FavoriteRegistry;
import com.nilsson.metadataviewer.service.MetadataService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.util.Map;

/**
 * Enhanced Extractor View with Image Preview and Auto-Wrapping Metadata Cards.
 * Fixed the "funky" layout by using managed widths and forced text wrapping.
 */
public class ExtractorView extends ScrollPane {
    private final MetadataService service = new MetadataService();
    private final TextArea promptText = new TextArea();

    private final Label modelVal = new Label("-");
    private final Label samplerVal = new Label("-");
    private final Label stepsVal = new Label("-");
    private final Label lorasVal = new Label("-");

    private final ImageView previewImageView = new ImageView();
    private final StackPane previewContainer = new StackPane();

    private Map<String, String> lastData;
    private File lastFile;

    public ExtractorView() {
        this.setFitToWidth(true);
        this.getStyleClass().add("content-view");
        this.setHbarPolicy(ScrollBarPolicy.NEVER);

        VBox container = new VBox(25);
        container.setPadding(new Insets(30));
        container.setAlignment(Pos.TOP_CENTER);

        // 1. Header
        Label title = new Label("AI Metadata Extractor");
        title.getStyleClass().add("content-title");

        // 2. Drop & Preview Section
        HBox dropSection = new HBox(20);
        dropSection.setAlignment(Pos.CENTER);

        VBox dropZone = createDropZone();
        HBox.setHgrow(dropZone, Priority.ALWAYS);

        setupPreviewContainer();
        dropSection.getChildren().addAll(dropZone, previewContainer);

        // 3. Prompt Area
        VBox pSection = new VBox(10);
        Label pTitle = new Label("Prompt");
        pTitle.getStyleClass().add("app-title");
        promptText.setWrapText(true);
        promptText.setEditable(false);
        promptText.setPrefHeight(120);
        pSection.getChildren().addAll(pTitle, promptText);

        // 4. Metadata Grid (Refactored to be more stable)
        VBox statsWrapper = new VBox(15);

        HBox topRow = new HBox(15);
        VBox modelCard = createStatCard("Model", modelVal, FontAwesome.CUBE);
        VBox samplerCard = createStatCard("Sampler / Scheduler", samplerVal, FontAwesome.SLIDERS);
        HBox.setHgrow(modelCard, Priority.ALWAYS);
        HBox.setHgrow(samplerCard, Priority.ALWAYS);
        topRow.getChildren().addAll(modelCard, samplerCard);

        HBox middleRow = new HBox(15);
        VBox stepsCard = createStatCard("Steps", stepsVal, FontAwesome.TASKS);
        HBox.setHgrow(stepsCard, Priority.ALWAYS);
        middleRow.getChildren().addAll(stepsCard);

        VBox loraCard = createStatCard("Loras Used", lorasVal, FontAwesome.PUZZLE_PIECE);

        statsWrapper.getChildren().addAll(topRow, middleRow, loraCard);

        // 5. Actions
        HBox actions = createActions();

        container.getChildren().addAll(title, dropSection, pSection, statsWrapper, actions);
        this.setContent(container);
    }

    private void setupPreviewContainer() {
        previewContainer.getStyleClass().add("income-stats-box");
        previewContainer.setPrefSize(160, 160);
        previewContainer.setMaxSize(160, 160);

        previewImageView.setFitWidth(140);
        previewImageView.setFitHeight(140);
        previewImageView.setPreserveRatio(true);

        Label placeholder = new Label("PREVIEW");
        placeholder.setStyle("-fx-text-fill: #475569; -fx-font-weight: bold;");
        previewContainer.getChildren().addAll(placeholder, previewImageView);
    }

    private VBox createDropZone() {
        VBox dz = new VBox(10);
        dz.getStyleClass().add("drop-zone");
        dz.setMinHeight(160);
        dz.setAlignment(Pos.CENTER);
        dz.getChildren().addAll(new FontIcon(FontAwesome.CLOUD_UPLOAD), new Label("Drop Image Here"));

        dz.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
                dz.getStyleClass().add("drop-zone-active");
            }
            e.consume();
        });
        dz.setOnDragExited(e -> dz.getStyleClass().remove("drop-zone-active"));
        dz.setOnDragDropped(e -> {
            if (e.getDragboard().hasFiles()) {
                process(e.getDragboard().getFiles().get(0));
                e.setDropCompleted(true);
            }
            dz.getStyleClass().remove("drop-zone-active");
            e.consume();
        });
        return dz;
    }

    private VBox createStatCard(String title, Label val, org.kordamp.ikonli.Ikon icon) {
        VBox card = new VBox(8);
        card.getStyleClass().add("income-stats-box");
        card.setPadding(new Insets(15));
        card.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox(10, new FontIcon(icon), new Label(title));
        header.setAlignment(Pos.CENTER_LEFT);

        val.getStyleClass().add("income-stat-value");
        val.setWrapText(true);
        // Force wrap based on parent width
        val.maxWidthProperty().bind(card.widthProperty().subtract(30));

        card.getChildren().addAll(header, val);
        return card;
    }

    private HBox createActions() {
        Button cp = new Button("Copy Prompt");
        cp.setGraphic(new FontIcon(FontAwesome.COPY));
        cp.setOnAction(e -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(promptText.getText());
            clipboard.setContent(content);
        });

        Button fv = new Button("Save Favorite");
        fv.setGraphic(new FontIcon(FontAwesome.STAR));
        fv.setOnAction(e -> {
            if (lastData == null) return;
            TextInputDialog tid = new TextInputDialog("Prompt Name");
            tid.showAndWait().ifPresent(name -> {
                FavoriteRegistry.getInstance().addFavorite(new FavoriteData(
                        name, lastData.get("Prompt"), lastData.get("Model"),
                        lastData.get("Steps"), lastData.get("Loras"),
                        lastFile != null ? lastFile.getAbsolutePath() : null
                ));
            });
        });

        HBox h = new HBox(15, cp, fv);
        h.setAlignment(Pos.CENTER_RIGHT);
        h.setPadding(new Insets(0, 0, 20, 0));
        return h;
    }

    private void process(File f) {
        this.lastFile = f;
        lastData = service.getExtractedData(f);
        promptText.setText(lastData.getOrDefault("Prompt", ""));
        modelVal.setText(lastData.getOrDefault("Model", "N/A"));
        samplerVal.setText(lastData.getOrDefault("Sampler", "N/A"));
        stepsVal.setText(lastData.getOrDefault("Steps", "N/A"));
        lorasVal.setText(lastData.getOrDefault("Loras", "None"));

        try {
            previewImageView.setImage(new Image(f.toURI().toString(), 140, 140, true, true));
        } catch (Exception e) {
            previewImageView.setImage(null);
        }
    }
}