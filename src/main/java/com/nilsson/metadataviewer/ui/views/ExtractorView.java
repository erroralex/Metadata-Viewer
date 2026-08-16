package com.nilsson.metadataviewer.ui.views;

import com.nilsson.metadataviewer.service.MetadataService;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ExtractorView extends ScrollPane {

    private final MetadataService service = new MetadataService();

    private final TextArea promptText = new TextArea();
    private final TextArea negativePromptText = new TextArea();

    private final TextField modelVal = createSelectableField("-");
    private final TextField softwareVal = createSelectableField("-");
    private final TextField samplerVal = createSelectableField("-");
    private final TextField schedulerVal = createSelectableField("-");
    private final TextField stepsVal = createSelectableField("-");
    private final TextField cfgVal = createSelectableField("-");
    private final TextField seedVal = createSelectableField("-");
    private final TextField sizeVal = createSelectableField("-");
    private final TextField denoiseVal = createSelectableField("-");
    private final TextField hiresFixVal = createSelectableField("-");
    private final TextArea lorasVal = new TextArea("-");

    private final ImageView previewImageView = new ImageView();
    private final StackPane imageArea = new StackPane();
    private final VBox dropHint = new VBox(10);
    private final Label fullScreenHint = new Label("Click to Fullscreen");
    private final Label toast = new Label();

    private Map<String, String> lastData;
    private File lastFile;

    public ExtractorView() {
        this.setFitToWidth(true);
        this.getStyleClass().add("content-view");
        this.setHbarPolicy(ScrollBarPolicy.NEVER);

        VBox container = new VBox(15);
        container.setPadding(new Insets(20, 30, 30, 30));
        container.setAlignment(Pos.TOP_CENTER);

        HBox mainSplit = new HBox(20);
        mainSplit.setAlignment(Pos.TOP_CENTER);

        // --- Left: image column (drop zone doubles as preview) ---
        VBox imageColumn = new VBox(10);
        imageColumn.setAlignment(Pos.TOP_CENTER);
        imageColumn.setPrefWidth(360);
        imageColumn.setMinWidth(280);

        setupImageArea();

        fullScreenHint.setStyle("-fx-font-size: 10px; -fx-text-fill: -app-text-muted;");
        fullScreenHint.setVisible(false);
        fullScreenHint.setManaged(false);

        imageColumn.getChildren().addAll(imageArea, fullScreenHint, createRawButton());

        // --- Right: metadata panel ---
        VBox metadataPanel = new VBox(15);
        HBox.setHgrow(metadataPanel, Priority.ALWAYS);

        VBox promptsWrapper = new VBox(15);
        promptsWrapper.getChildren().addAll(
                createPromptSection("Positive Prompt", promptText, 100),
                createPromptSection("Negative Prompt", negativePromptText, 60)
        );

        VBox statsWrapper = new VBox(12);

        VBox modelCard = createStatCard("Model", modelVal, FontAwesome.CUBE);
        VBox softwareCard = createStatCard("Software", softwareVal, FontAwesome.TERMINAL);
        HBox.setHgrow(modelCard, Priority.ALWAYS);
        softwareCard.setPrefWidth(220);
        HBox.setHgrow(softwareCard, Priority.NEVER);
        HBox row1 = new HBox(12, modelCard, softwareCard);

        VBox stepsCard = createStatCard("Steps", stepsVal, FontAwesome.TASKS);
        VBox cfgCard = createStatCard("CFG", cfgVal, FontAwesome.ADJUST);
        VBox seedCard = createStatCard("Seed", seedVal, FontAwesome.KEY);
        VBox samplerCard = createStatCard("Sampler", samplerVal, FontAwesome.SLIDERS);
        VBox schedulerCard = createStatCard("Scheduler", schedulerVal, FontAwesome.CLOCK_O);
        stepsCard.setMinWidth(Region.USE_PREF_SIZE);
        cfgCard.setMinWidth(Region.USE_PREF_SIZE);
        seedCard.setMinWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(seedCard, Priority.NEVER);
        HBox.setHgrow(samplerCard, Priority.ALWAYS);
        HBox.setHgrow(schedulerCard, Priority.ALWAYS);
        HBox row2 = new HBox(12, stepsCard, cfgCard, seedCard, samplerCard, schedulerCard);

        VBox sizeCard = createStatCard("Size", sizeVal, FontAwesome.IMAGE);
        VBox denoiseCard = createStatCard("Denoise", denoiseVal, FontAwesome.TINT);
        VBox hiresCard = createStatCard("Hires. fix", hiresFixVal, FontAwesome.EXPAND);
        HBox row3 = new HBox(12, sizeCard, denoiseCard, hiresCard);
        HBox.setHgrow(sizeCard, Priority.ALWAYS);
        HBox.setHgrow(denoiseCard, Priority.ALWAYS);
        HBox.setHgrow(hiresCard, Priority.ALWAYS);

        lorasVal.setEditable(false);
        lorasVal.setWrapText(true);
        lorasVal.setPrefHeight(70);
        lorasVal.getStyleClass().add("text-area");
        lorasVal.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");

        statsWrapper.getChildren().addAll(
                row1, row2, row3,
                createStatCard("Loras Used", lorasVal, FontAwesome.PUZZLE_PIECE)
        );

        metadataPanel.getChildren().addAll(promptsWrapper, statsWrapper);

        mainSplit.getChildren().addAll(imageColumn, metadataPanel);

        Label title = new Label("AI Image Metadata Extractor");
        title.getStyleClass().add("content-title");

        container.getChildren().addAll(title, mainSplit);

        StackPane root = new StackPane(container);
        toast.getStyleClass().add("toast");
        toast.setVisible(false);
        toast.setMouseTransparent(true);
        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new Insets(0, 0, 24, 0));
        root.getChildren().add(toast);

        this.setContent(root);
    }

    /**
     * Steps/CFG/Seed cards are kept compact by default, but CFG in particular can
     * carry longer text (e.g. "1 (distilled 3.5)") — size the field to whatever
     * text it actually holds instead of clipping it behind a fixed width.
     */
    private static final Font STAT_VALUE_FONT = Font.font(null, FontWeight.BOLD, 19.6);

    private static void autoSizeStatField(TextField field, double minWidth) {
        Text measure = new Text(field.getText());
        measure.setFont(STAT_VALUE_FONT);
        double width = measure.getLayoutBounds().getWidth() + 24;
        field.setPrefWidth(Math.max(minWidth, width));
    }

    private static void flashToast(Label toastLabel, String message) {
        toastLabel.setText(message);
        toastLabel.setOpacity(0);
        toastLabel.setVisible(true);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), toastLabel);
        fadeIn.setToValue(1);
        PauseTransition hold = new PauseTransition(Duration.millis(1400));
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toastLabel);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> toastLabel.setVisible(false));

        new SequentialTransition(fadeIn, hold, fadeOut).play();
    }

    private void setupImageArea() {
        imageArea.getStyleClass().add("drop-zone");
        imageArea.setPrefSize(320, 320);
        imageArea.setCursor(javafx.scene.Cursor.HAND);
        imageArea.setFocusTraversable(false);

        FontIcon icon = new FontIcon(FontAwesome.CLOUD_UPLOAD);
        icon.setIconSize(32);
        Label dropLabel = new Label("Drop Image Here");
        dropLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 14px;");
        dropHint.setAlignment(Pos.CENTER);
        dropHint.getChildren().addAll(icon, dropLabel);

        previewImageView.setFitWidth(300);
        previewImageView.setFitHeight(300);
        previewImageView.setPreserveRatio(true);
        previewImageView.setSmooth(true);
        previewImageView.setVisible(false);
        previewImageView.setManaged(false);

        imageArea.getChildren().addAll(dropHint, previewImageView);

        imageArea.setOnMouseClicked(e -> {
            if (lastFile != null && lastFile.exists()) {
                showFullScreenImage(lastFile);
            }
        });

        imageArea.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
                imageArea.getStyleClass().add("drop-zone-active");
            }
            e.consume();
        });
        imageArea.setOnDragExited(e -> imageArea.getStyleClass().remove("drop-zone-active"));

        imageArea.setOnDragDropped(e -> {
            if (e.getDragboard().hasFiles()) {
                process(e.getDragboard().getFiles().get(0));
                e.setDropCompleted(true);
            }
            imageArea.getStyleClass().remove("drop-zone-active");
            e.consume();
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

        boolean hasImage = previewImageView.getImage() != null;
        dropHint.setVisible(!hasImage);
        dropHint.setManaged(!hasImage);
        previewImageView.setVisible(hasImage);
        previewImageView.setManaged(hasImage);
        fullScreenHint.setVisible(hasImage);
        fullScreenHint.setManaged(hasImage);

        imageArea.getStyleClass().removeAll("drop-zone", "image-frame");
        imageArea.getStyleClass().add(hasImage ? "image-frame" : "drop-zone");
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

    private Button createRawButton() {
        Button btnRaw = new Button("Raw Metadata");
        btnRaw.setGraphic(new FontIcon(FontAwesome.CODE));
        btnRaw.getStyleClass().add("button");
        btnRaw.setMaxWidth(Double.MAX_VALUE);

        btnRaw.setOnAction(e -> {
            if (lastData == null || !lastData.containsKey("Raw")) return;
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.initStyle(StageStyle.TRANSPARENT);
            DialogPane pane = dialog.getDialogPane();
            pane.getStyleClass().add("custom-dialog");
            if (this.getScene() != null) pane.getStylesheets().addAll(this.getScene().getStylesheets());

            TextArea rawArea = new TextArea(lastData.get("Raw"));
            rawArea.setEditable(false);
            rawArea.setWrapText(true);
            rawArea.getStyleClass().add("text-area");

            Label dialogToast = new Label();
            dialogToast.getStyleClass().add("toast");
            dialogToast.setVisible(false);
            dialogToast.setMouseTransparent(true);

            Button copyRawBtn = new Button("Copy Raw");
            copyRawBtn.setGraphic(new FontIcon(FontAwesome.COPY));
            copyRawBtn.getStyleClass().add("button");
            copyRawBtn.setOnAction(ev -> {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent clipContent = new ClipboardContent();
                clipContent.putString(rawArea.getText());
                clipboard.setContent(clipContent);
                flashToast(dialogToast, "Raw metadata copied to clipboard");
            });

            Button closeBtn = new Button("Close Viewer");
            closeBtn.getStyleClass().add("button");
            closeBtn.setOnAction(ev -> dialog.setResult(ButtonType.CLOSE));

            HBox actions = new HBox(10, copyRawBtn, closeBtn);
            actions.setAlignment(Pos.CENTER_RIGHT);

            VBox content = new VBox(10, new Label("Raw Image Metadata"), rawArea, actions);
            content.setPadding(new Insets(20));

            StackPane dialogRoot = new StackPane(content, dialogToast);
            StackPane.setAlignment(dialogToast, Pos.BOTTOM_CENTER);
            StackPane.setMargin(dialogToast, new Insets(0, 0, 16, 0));
            pane.setContent(dialogRoot);

            setupDialogDragging(pane);
            dialog.setOnShowing(ev -> pane.getScene().setFill(Color.TRANSPARENT));
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
            flashToast(toast, title + " copied to clipboard");
        });

        header.getChildren().addAll(label, copyBtn);
        area.setWrapText(true);
        area.setEditable(false);
        area.setPrefHeight(height);

        section.getChildren().addAll(header, area);
        return section;
    }

    private TextField createSelectableField(String text) {
        TextField field = new TextField(text);
        field.setEditable(false);
        field.getStyleClass().add("selectable-stat-field");
        field.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
        return field;
    }

    // Updated to accept generic Control (TextField or TextArea)
    private VBox createStatCard(String title, Control valField, org.kordamp.ikonli.Ikon icon) {
        VBox card = new VBox(5);
        card.getStyleClass().add("income-stats-box");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon fi = new FontIcon(icon);

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("stat-label");
        titleLbl.setMinWidth(Region.USE_PREF_SIZE);

        header.getChildren().addAll(fi, titleLbl);
        card.getChildren().addAll(header, valField);

        return card;
    }

    public void process(File f) {
        this.lastFile = f;
        promptText.setText("Parsing metadata...");
        if (this.getScene() != null) {
            this.getScene().setCursor(javafx.scene.Cursor.WAIT);
        }

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
            schedulerVal.setText(lastData.getOrDefault("Scheduler", "N/A"));
            stepsVal.setText(lastData.getOrDefault("Steps", "N/A"));
            cfgVal.setText(lastData.getOrDefault("CFG", "N/A"));
            seedVal.setText(lastData.getOrDefault("Seed", "N/A"));
            autoSizeStatField(stepsVal, 50);
            autoSizeStatField(cfgVal, 50);
            autoSizeStatField(seedVal, 90);
            sizeVal.setText(lastData.getOrDefault("Resolution", "N/A"));
            denoiseVal.setText(lastData.getOrDefault("Denoise", "N/A"));
            hiresFixVal.setText(lastData.getOrDefault("Hires. fix", "Disabled"));
            lorasVal.setText(lastData.getOrDefault("Loras", "None"));

            loadImageSafe(f);

            if (this.getScene() != null) {
                this.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
            }
        });

        extractionTask.setOnFailed(e -> {
            Throwable ex = extractionTask.getException();
            promptText.setText("Error reading file: " + (ex != null ? ex.getMessage() : "Unknown error"));
            modelVal.setText("Error");
            if (this.getScene() != null) {
                this.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
            }
            if (ex != null) {
                ex.printStackTrace();
            }

            loadImageSafe(f);
        });

        new Thread(extractionTask).start();
    }
}