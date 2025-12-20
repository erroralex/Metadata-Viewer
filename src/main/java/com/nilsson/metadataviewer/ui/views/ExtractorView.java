package com.nilsson.metadataviewer.ui.views;

import com.nilsson.metadataviewer.model.FavoriteData;
import com.nilsson.metadataviewer.model.FavoriteRegistry;
import com.nilsson.metadataviewer.service.MetadataService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Main Extraction View featuring a Drag & Drop zone and metadata cards.
 * Updated with Save Favorites, Raw JSON viewer, and smooth scrolling.
 */
public class ExtractorView extends ScrollPane {
    private final MetadataService service = new MetadataService();

    private final TextArea promptText = new TextArea();
    private final TextArea negativePromptText = new TextArea();

    private final Label modelVal = new Label("-");
    private final Label samplerVal = new Label("-");
    private final Label stepsVal = new Label("-");
    private final Label cfgVal = new Label("-");
    private final Label seedVal = new Label("-");
    private final Label lorasVal = new Label("-");

    private final ImageView previewImageView = new ImageView();
    private final StackPane previewContainer = new StackPane();

    private Map<String, String> lastData;
    private File lastFile;

    public ExtractorView() {
        this.setFitToWidth(true);
        this.getStyleClass().add("content-view");
        this.setHbarPolicy(ScrollBarPolicy.NEVER);

        // Optimized scrolling speed for long prompt lists
        this.addEventFilter(ScrollEvent.SCROLL, event -> {
            double deltaY = event.getDeltaY() * 4.0;
            double height = this.getContent().getBoundsInLocal().getHeight();
            double vValue = this.getVvalue();
            this.setVvalue(vValue - deltaY / height);
            event.consume();
        });

        VBox container = new VBox(15);
        container.setPadding(new Insets(20, 30, 30, 30));
        container.setAlignment(Pos.TOP_CENTER);

        // 1. Header
        Label title = new Label("AI Metadata Extractor");
        title.getStyleClass().add("content-title");

        // 2. Drop & Preview Section
        HBox dropSection = new HBox(20);
        dropSection.setAlignment(Pos.TOP_CENTER);

        VBox dropZone = createDropZone();
        HBox.setHgrow(dropZone, Priority.ALWAYS);

        setupPreviewContainer();

        // Action panel with Preview and multi-button controls
        VBox previewWrapper = new VBox(10);
        previewWrapper.setAlignment(Pos.TOP_CENTER);

        Button btnSave = createSaveButton();
        Button btnRaw = createRawButton(); // New Raw JSON toggle

        previewWrapper.getChildren().addAll(previewContainer, btnSave, btnRaw);

        dropSection.getChildren().addAll(dropZone, previewWrapper);

        // 3. Prompt Areas
        VBox promptsWrapper = new VBox(15);
        VBox pSection = createPromptSection("Positive Prompt", promptText, 100);
        VBox nSection = createPromptSection("Negative Prompt", negativePromptText, 60);
        promptsWrapper.getChildren().addAll(pSection, nSection);

        // 4. Metadata Grid
        VBox statsWrapper = new VBox(12);

        HBox row1 = new HBox(12);
        VBox modelCard = createStatCard("Model", modelVal, FontAwesome.CUBE);
        VBox samplerCard = createStatCard("Sampler / Scheduler", samplerVal, FontAwesome.SLIDERS);
        HBox.setHgrow(modelCard, Priority.ALWAYS);
        HBox.setHgrow(samplerCard, Priority.ALWAYS);
        row1.getChildren().addAll(modelCard, samplerCard);

        HBox row2 = new HBox(12);
        VBox stepsCard = createStatCard("Steps", stepsVal, FontAwesome.TASKS);
        VBox cfgCard = createStatCard("CFG Scale", cfgVal, FontAwesome.ADJUST);
        VBox seedCard = createStatCard("Seed", seedVal, FontAwesome.KEY);
        HBox.setHgrow(stepsCard, Priority.ALWAYS);
        HBox.setHgrow(cfgCard, Priority.ALWAYS);
        HBox.setHgrow(seedCard, Priority.ALWAYS);
        row2.getChildren().addAll(stepsCard, cfgCard, seedCard);

        VBox loraCard = createStatCard("Loras Used", lorasVal, FontAwesome.PUZZLE_PIECE);
        statsWrapper.getChildren().addAll(row1, row2, loraCard);

        container.getChildren().addAll(title, dropSection, promptsWrapper, statsWrapper);
        this.setContent(container);
    }

    // Creates a draggable, themed dialog to view the exact raw metadata string.
    private Button createRawButton() {
        Button btnRaw = new Button("Raw Metadata");
        btnRaw.setGraphic(new FontIcon(FontAwesome.CODE));
        btnRaw.getStyleClass().add("button");
        btnRaw.setMaxWidth(Double.MAX_VALUE);

        btnRaw.setOnAction(e -> {
            if (lastData == null || !lastData.containsKey("Raw")) return;

            // Dialog<ButtonType> for better compatibility with Java 8
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.initStyle(StageStyle.UNDECORATED);

            DialogPane pane = dialog.getDialogPane();
            pane.getStyleClass().add("custom-dialog");
            if (this.getScene() != null) {
                pane.getStylesheets().addAll(this.getScene().getStylesheets());
            }
            pane.setPrefSize(600, 500);

            Label header = new Label("Raw Image Metadata");
            header.getStyleClass().add("content-title");
            header.setStyle("-fx-font-size: 1.2em; -fx-padding: 0 0 10 0;");

            TextArea rawArea = new TextArea(lastData.get("Raw"));
            rawArea.setEditable(false);
            rawArea.setWrapText(true);
            rawArea.getStyleClass().add("text-area");
            VBox.setVgrow(rawArea, Priority.ALWAYS);

            Button closeBtn = new Button("Close Viewer");
            closeBtn.getStyleClass().add("button");

            // Explicitly hide the dialog window
            closeBtn.setOnAction(ev -> {
                dialog.setResult(ButtonType.CLOSE);
                dialog.hide();
            });

            VBox content = new VBox(10, header, rawArea, closeBtn);
            content.setPadding(new Insets(20));
            content.setAlignment(Pos.CENTER_LEFT);
            pane.setContent(content);

            // Required to allow the dialog to close in some Java 8 versions
            pane.getButtonTypes().add(ButtonType.CLOSE);
            pane.lookupButton(ButtonType.CLOSE).setVisible(false);

            setupDialogDragging(pane);
            dialog.showAndWait();
        });
        return btnRaw;
    }

    private Button createSaveButton() {
        Button fv = new Button("Save Favorite");
        fv.setGraphic(new FontIcon(FontAwesome.STAR));
        fv.getStyleClass().add("button");
        fv.setMaxWidth(Double.MAX_VALUE);

        fv.setOnAction(e -> {
            if (lastData == null) return;

            TextInputDialog tid = new TextInputDialog("Prompt Name");
            tid.initStyle(StageStyle.UNDECORATED);
            tid.setHeaderText("Add to Favorites Library");
            tid.setContentText("Enter a name for this prompt:");

            DialogPane dialogPane = tid.getDialogPane();
            dialogPane.getStyleClass().add("custom-dialog");

            if (this.getScene() != null) {
                dialogPane.getStylesheets().addAll(this.getScene().getStylesheets());
            }

            setupDialogDragging(dialogPane);

            tid.showAndWait().ifPresent(name -> {
                FavoriteRegistry.getInstance().addFavorite(new FavoriteData(
                        name, lastData.get("Prompt"), lastData.get("Model"),
                        lastData.get("Steps"), lastData.get("Loras"),
                        lastFile != null ? lastFile.getAbsolutePath() : null
                ));
            });
        });
        return fv;
    }

    private void setupDialogDragging(DialogPane pane) {
        AtomicReference<Double> xOffset = new AtomicReference<>((double) 0);
        AtomicReference<Double> yOffset = new AtomicReference<>((double) 0);

        pane.setOnMousePressed(event -> {
            xOffset.set(event.getSceneX());
            yOffset.set(event.getSceneY());
        });

        pane.setOnMouseDragged(event -> {
            Window window = pane.getScene().getWindow();
            if (window != null) {
                window.setX(event.getScreenX() - xOffset.get());
                window.setY(event.getScreenY() - yOffset.get());
            }
        });
    }

    private VBox createPromptSection(String title, TextArea area, int height) {
        VBox section = new VBox(5);
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(title);
        label.getStyleClass().add("app-title");

        Button copyBtn = new Button();
        copyBtn.setGraphic(new FontIcon(FontAwesome.COPY));
        copyBtn.getStyleClass().add("button");
        copyBtn.setOnAction(e -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(area.getText());
            clipboard.setContent(content);
        });

        header.getChildren().addAll(label, copyBtn);
        area.setWrapText(true);
        area.setEditable(false);
        area.setPrefHeight(height);

        section.getChildren().addAll(header, area);
        return section;
    }

    private void setupPreviewContainer() {
        previewContainer.getStyleClass().add("income-stats-box");
        previewContainer.setPrefSize(160, 160);
        previewContainer.setMaxSize(160, 160);
        previewImageView.setFitWidth(140);
        previewImageView.setFitHeight(140);
        previewImageView.setPreserveRatio(true);
        previewContainer.getChildren().add(previewImageView);
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
        VBox card = new VBox(5);
        card.getStyleClass().add("income-stats-box");
        card.setPadding(new Insets(10, 15, 10, 15));
        card.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox(8, new FontIcon(icon), new Label(title));
        header.setAlignment(Pos.CENTER_LEFT);

        val.getStyleClass().add("income-stat-value");
        val.setWrapText(true);
        val.maxWidthProperty().bind(card.widthProperty().subtract(30));

        card.getChildren().addAll(header, val);
        return card;
    }

    public void process(File f) {
        this.lastFile = f;
        lastData = service.getExtractedData(f);
        promptText.setText(lastData.getOrDefault("Prompt", ""));
        negativePromptText.setText(lastData.getOrDefault("Negative", "None"));
        modelVal.setText(lastData.getOrDefault("Model", "N/A"));
        samplerVal.setText(lastData.getOrDefault("Sampler", "N/A"));
        stepsVal.setText(lastData.getOrDefault("Steps", "N/A"));
        cfgVal.setText(lastData.getOrDefault("CFG", "N/A"));
        seedVal.setText(lastData.getOrDefault("Seed", "N/A"));
        lorasVal.setText(lastData.getOrDefault("Loras", "None"));
        try {
            previewImageView.setImage(new Image(f.toURI().toString(), 140, 140, true, true));
        } catch (Exception e) {
            previewImageView.setImage(null);
        }
    }
}