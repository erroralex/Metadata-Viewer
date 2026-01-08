package com.nilsson.metadataviewer.ui.views;

import com.nilsson.metadataviewer.model.FavoriteData;
import com.nilsson.metadataviewer.model.FavoriteRegistry;
import com.nilsson.metadataviewer.service.MetadataService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ExtractorView extends ScrollPane {

    private final MetadataService service = new MetadataService();

    private final TextArea promptText = new TextArea();
    private final TextArea negativePromptText = new TextArea();

    private final TextField modelVal = createSelectableField("-");
    private final TextField softwareVal = createSelectableField("-");
    private final TextField samplerVal = createSelectableField("-");
    private final TextField stepsVal = createSelectableField("-");
    private final TextField cfgVal = createSelectableField("-");
    private final TextField seedVal = createSelectableField("-");
    private final TextField sizeVal = createSelectableField("-");
    private final TextField lorasVal = createSelectableField("-");

    private final ImageView previewImageView = new ImageView();
    private final StackPane previewContainer = new StackPane();

    private Map<String, String> lastData;
    private File lastFile;

    public ExtractorView() {
        this.setFitToWidth(true);
        this.getStyleClass().add("content-view");
        this.setHbarPolicy(ScrollBarPolicy.NEVER);

        VBox container = new VBox(15);
        container.setPadding(new Insets(20, 30, 30, 30));
        container.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("AI Image Metadata Extractor");
        title.getStyleClass().add("content-title");

        HBox dropSection = new HBox(20);
        dropSection.setAlignment(Pos.TOP_CENTER);

        VBox dropZone = createDropZone();
        HBox.setHgrow(dropZone, Priority.ALWAYS);

        setupPreviewContainer();

        VBox previewWrapper = new VBox(10);
        previewWrapper.setAlignment(Pos.TOP_CENTER);

        Label fullScreenHint = new Label("Click to Fullscreen");
        fullScreenHint.setStyle("-fx-font-size: 10px; -fx-text-fill: -app-text-muted;");

        previewWrapper.getChildren().addAll(fullScreenHint, previewContainer, createSaveButton(), createRawButton());

        dropSection.getChildren().addAll(dropZone, previewWrapper);

        VBox promptsWrapper = new VBox(15);
        promptsWrapper.getChildren().addAll(
                createPromptSection("Positive Prompt", promptText, 100),
                createPromptSection("Negative Prompt", negativePromptText, 60)
        );

        // --- Stats Grid ---
        VBox statsWrapper = new VBox(12);

        VBox modelCard = createStatCard("Model", modelVal, FontAwesome.CUBE);
        VBox softwareCard = createStatCard("Software", softwareVal, FontAwesome.TERMINAL);

        HBox.setHgrow(modelCard, Priority.ALWAYS);
        HBox.setHgrow(softwareCard, Priority.NEVER);
        softwareCard.setMinWidth(100);

        HBox row1 = new HBox(12, modelCard, softwareCard);

        VBox stepsCard = createStatCard("Steps", stepsVal, FontAwesome.TASKS);
        VBox cfgCard = createStatCard("CFG", cfgVal, FontAwesome.ADJUST);
        VBox seedCard = createStatCard("Seed", seedVal, FontAwesome.KEY);
        VBox samplerCard = createStatCard("Sampler", samplerVal, FontAwesome.SLIDERS);
        VBox sizeCard = createStatCard("Size", sizeVal, FontAwesome.IMAGE);

        stepsCard.setPrefWidth(90);
        cfgCard.setPrefWidth(90);
        HBox.setHgrow(seedCard, Priority.ALWAYS);
        samplerCard.setPrefWidth(140);
        sizeCard.setPrefWidth(140);

        HBox row2 = new HBox(12, stepsCard, cfgCard, seedCard, samplerCard, sizeCard);

        statsWrapper.getChildren().addAll(row1, row2, createStatCard("Loras Used", lorasVal, FontAwesome.PUZZLE_PIECE));

        container.getChildren().addAll(title, dropSection, promptsWrapper, statsWrapper);
        this.setContent(container);
    }

    public void populateFromFavorite(FavoriteData fav) {
        this.lastData = new HashMap<>();
        if (fav == null) return;

        lastData.put("Raw", fav.getRaw());
        lastData.put("Prompt", fav.getPrompt());
        lastData.put("Negative", fav.getNegative());
        lastData.put("Model", fav.getModel());
        lastData.put("Software", fav.getSoftware());
        lastData.put("Sampler", fav.getSampler());
        lastData.put("Steps", fav.getSteps());
        lastData.put("CFG", fav.getCfg());
        lastData.put("Seed", fav.getSeed());
        lastData.put("Size", fav.getSize());
        lastData.put("Loras", fav.getLoras());

        promptText.setText(fav.getPrompt());
        negativePromptText.setText(fav.getNegative());
        modelVal.setText(fav.getModel());
        softwareVal.setText(fav.getSoftware() != null ? fav.getSoftware() : "Unknown");
        samplerVal.setText(fav.getSampler());
        stepsVal.setText(fav.getSteps());
        cfgVal.setText(fav.getCfg());
        seedVal.setText(fav.getSeed());
        sizeVal.setText(fav.getSize() != null ? fav.getSize() : "N/A");
        lorasVal.setText(fav.getLoras());

        if (fav.getThumbnailPath() != null) {
            File thumbFile = new File(fav.getThumbnailPath());
            this.lastFile = thumbFile.exists() ? thumbFile : null;
            if (this.lastFile != null) {
                loadImageSafe(this.lastFile);
            }
        }
    }

    private void setupPreviewContainer() {
        previewContainer.getStyleClass().add("income-stats-box");
        previewContainer.setPrefSize(160, 160);

        previewImageView.setFitWidth(140);
        previewImageView.setFitHeight(140);
        previewImageView.setPreserveRatio(true);
        previewImageView.setSmooth(true);

        previewContainer.getChildren().add(previewImageView);
        previewContainer.setCursor(javafx.scene.Cursor.HAND);

        previewContainer.setOnMouseClicked(e -> {
            if (lastFile != null && lastFile.exists()) {
                showFullScreenImage(lastFile);
            }
        });
    }

    private void showFullScreenImage(File file) {
        Stage stage = new Stage();
        stage.initOwner(this.getScene().getWindow());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);

        ImageView fullView = new ImageView();
        fullView.setPreserveRatio(true);
        fullView.setSmooth(true);

        loadImageIntoView(file, fullView, false);

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

    private void loadImageSafe(File f) {
        loadImageIntoView(f, previewImageView, true);
    }

    private void loadImageIntoView(File f, ImageView view, boolean small) {
        try {
            Image img = MetadataService.loadFxImage(f);
            view.setImage(img);
        } catch (Exception e) {
            e.printStackTrace();
            view.setImage(null);
        }
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
            DialogPane pane = tid.getDialogPane();
            pane.getStyleClass().add("custom-dialog");
            if (this.getScene() != null) pane.getStylesheets().addAll(this.getScene().getStylesheets());
            setupDialogDragging(pane);

            tid.showAndWait().ifPresent(name -> {
                String size = lastData.getOrDefault("Size", "N/A");
                if (size.equals("N/A") && lastData.containsKey("Width") && lastData.containsKey("Height")) {
                    size = lastData.get("Width") + "x" + lastData.get("Height");
                }

                FavoriteData fav = new FavoriteData(
                        name,
                        lastData.getOrDefault("Prompt", ""),
                        lastData.getOrDefault("Negative", "None"),
                        lastData.getOrDefault("Model", "N/A"),
                        lastData.getOrDefault("Software", "Unknown"),
                        lastData.getOrDefault("Sampler", "N/A"),
                        lastData.getOrDefault("Steps", "N/A"),
                        lastData.getOrDefault("CFG", "N/A"),
                        lastData.getOrDefault("Seed", "N/A"),
                        size,
                        lastData.getOrDefault("Loras", "None"),
                        lastData.getOrDefault("Raw", ""),
                        lastFile != null ? lastFile.getAbsolutePath() : null
                );

                if (lastFile != null && lastFile.exists()) {
                    String savedPath = FavoriteRegistry.getInstance().saveImage(lastFile, fav.getId());
                    fav.setThumbnailPath(savedPath);
                }
                FavoriteRegistry.getInstance().addFavorite(fav);
            });
        });
        return fv;
    }

    private Button createRawButton() {
        Button btnRaw = new Button("Raw Metadata");
        btnRaw.setGraphic(new FontIcon(FontAwesome.CODE));
        btnRaw.getStyleClass().add("button");
        btnRaw.setMaxWidth(Double.MAX_VALUE);

        btnRaw.setOnAction(e -> {
            if (lastData == null || !lastData.containsKey("Raw")) return;
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.initStyle(StageStyle.UNDECORATED);
            DialogPane pane = dialog.getDialogPane();
            pane.getStyleClass().add("custom-dialog");
            if (this.getScene() != null) pane.getStylesheets().addAll(this.getScene().getStylesheets());

            TextArea rawArea = new TextArea(lastData.get("Raw"));
            rawArea.setEditable(false);
            rawArea.setWrapText(true);
            rawArea.getStyleClass().add("text-area");

            Button closeBtn = new Button("Close Viewer");
            closeBtn.setOnAction(ev -> dialog.setResult(ButtonType.CLOSE));

            VBox content = new VBox(10, new Label("Raw Image Metadata"), rawArea, closeBtn);
            content.setPadding(new Insets(20));
            pane.setContent(content);
            setupDialogDragging(pane);
            dialog.showAndWait();
        });
        return btnRaw;
    }

    private void setupDialogDragging(DialogPane pane) {
        AtomicReference<Double> xOffset = new AtomicReference<>(0.0);
        AtomicReference<Double> yOffset = new AtomicReference<>(0.0);
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
        copyBtn.setTooltip(new Tooltip("Copy to Clipboard"));

        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
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

    private VBox createDropZone() {
        VBox dz = new VBox(10);
        dz.getStyleClass().add("drop-zone");
        dz.setMinHeight(160);
        dz.setAlignment(Pos.CENTER);

        Label dropLabel = new Label("Drop Image Here");
        // Inline style ONLY for text color to reference CSS variable safely
        dropLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 14px;");

        FontIcon icon = new FontIcon(FontAwesome.CLOUD_UPLOAD);
        icon.setIconSize(32);

        dz.getChildren().addAll(icon, dropLabel);

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
                process(e.getDragboard().getFiles().get(0));
                e.setDropCompleted(true);
            }
            dz.getStyleClass().remove("drop-zone-active");
            e.consume();
        });
        return dz;
    }

    private TextField createSelectableField(String text) {
        TextField field = new TextField(text);
        field.setEditable(false);
        field.getStyleClass().add("selectable-stat-field");
        field.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
        return field;
    }

    private VBox createStatCard(String title, TextField valField, org.kordamp.ikonli.Ikon icon) {
        VBox card = new VBox(5);
        card.getStyleClass().add("income-stats-box");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon fi = new FontIcon(icon);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 0.9em;");

        header.getChildren().addAll(fi, titleLbl);
        card.getChildren().addAll(header, valField);

        return card;
    }

    public void process(File f) {
        this.lastFile = f;
        promptText.setText("Parsing metadata...");
        this.getScene().setCursor(javafx.scene.Cursor.WAIT);

        Task<Map<String, String>> extractionTask = new Task<Map<String, String>>() {
            @Override
            protected Map<String, String> call() throws Exception {
                return service.getExtractedData(f);
            }
        };

        extractionTask.setOnSucceeded(e -> {
            lastData = extractionTask.getValue();

            promptText.setText(lastData.getOrDefault("Prompt", ""));
            negativePromptText.setText(lastData.getOrDefault("Negative", "None"));

            modelVal.setText(lastData.getOrDefault("Model", "N/A"));
            softwareVal.setText(lastData.getOrDefault("Software", "Unknown"));
            samplerVal.setText(lastData.getOrDefault("Sampler", "N/A"));
            stepsVal.setText(lastData.getOrDefault("Steps", "N/A"));
            cfgVal.setText(lastData.getOrDefault("CFG", "N/A"));
            seedVal.setText(lastData.getOrDefault("Seed", "N/A"));
            lorasVal.setText(lastData.getOrDefault("Loras", "None"));

            String sizeText = lastData.get("Size");
            if (sizeText == null && lastData.containsKey("Width") && lastData.containsKey("Height")) {
                sizeText = lastData.get("Width") + "x" + lastData.get("Height");
            }
            sizeVal.setText(sizeText != null ? sizeText : "N/A");

            loadImageSafe(f);

            this.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
        });

        extractionTask.setOnFailed(e -> {
            Throwable ex = extractionTask.getException();
            promptText.setText("Error reading file: " + ex.getMessage());
            modelVal.setText("Error");
            this.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
            ex.printStackTrace();

            loadImageSafe(f);
        });

        new Thread(extractionTask).start();
    }
}