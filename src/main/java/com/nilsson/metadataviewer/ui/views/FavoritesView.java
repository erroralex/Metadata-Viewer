package com.nilsson.metadataviewer.ui.views;

import com.nilsson.metadataviewer.model.FavoriteData;
import com.nilsson.metadataviewer.model.FavoriteRegistry;
import com.nilsson.metadataviewer.ui.RootLayout;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.util.List;

/**
 * Favorites View with Persistent Toggleable List/Gallery Modes.
 */
public class FavoritesView extends VBox {

    private final ListView<FavoriteData> listView = new ListView<>();
    private final ScrollPane galleryScroll = new ScrollPane();
    private final TilePane galleryPane = new TilePane();
    private final RootLayout rootLayout;

    private boolean isGalleryMode = false;

    public FavoritesView(RootLayout rootLayout) {
        this.rootLayout = rootLayout;
        this.setPadding(new Insets(30));
        this.setSpacing(20);
        this.getStyleClass().add("content-view");
        this.setStyle("-fx-background-color: #1c212b;");

        // --- Load Preference ---
        boolean savedMode = Boolean.parseBoolean(
                FavoriteRegistry.getInstance().getSetting("gallery_mode", "false")
        );

        // --- Header ---
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Saved Favorites Library");
        title.getStyleClass().add("content-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // View Toggles
        ToggleButton btnList = new ToggleButton();
        btnList.setGraphic(new FontIcon(FontAwesome.LIST));
        btnList.getStyleClass().add("button");
        btnList.setTooltip(new Tooltip("List View"));

        ToggleButton btnGallery = new ToggleButton();
        btnGallery.setGraphic(new FontIcon(FontAwesome.TH_LARGE));
        btnGallery.getStyleClass().add("button");
        btnGallery.setTooltip(new Tooltip("Gallery View"));

        ToggleGroup viewGroup = new ToggleGroup();
        btnList.setToggleGroup(viewGroup);
        btnGallery.setToggleGroup(viewGroup);

        if (savedMode) {
            btnGallery.setSelected(true);
        } else {
            btnList.setSelected(true);
        }

        btnList.setOnAction(e -> switchView(false));
        btnGallery.setOnAction(e -> switchView(true));

        Button btnRefresh = new Button("Sync");
        btnRefresh.setGraphic(new FontIcon(FontAwesome.REFRESH));
        btnRefresh.getStyleClass().add("button");
        btnRefresh.setOnAction(e -> refresh());

        header.getChildren().addAll(title, spacer, btnList, btnGallery, btnRefresh);

        // --- Content Area ---
        setupListView();
        setupGalleryView();

        StackPane contentArea = new StackPane(listView, galleryScroll);
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        // Apply initial view mode
        switchView(savedMode);

        this.getChildren().addAll(header, contentArea);

        refresh();
    }

    private void switchView(boolean gallery) {
        this.isGalleryMode = gallery;
        listView.setVisible(!gallery);
        galleryScroll.setVisible(gallery);

        // Persist the choice
        FavoriteRegistry.getInstance().setSetting("gallery_mode", String.valueOf(gallery));

        if (gallery) {
            updateGallery();
        }
    }

    private void setupListView() {
        listView.setCellFactory(param -> new FavoriteCardCell());
        listView.getStyleClass().add("list-view");
        listView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
    }

    private void setupGalleryView() {
        galleryPane.setPadding(new Insets(10));
        galleryPane.setHgap(15);
        galleryPane.setVgap(15);
        galleryPane.setStyle("-fx-background-color: transparent;");

        galleryScroll.setContent(galleryPane);
        galleryScroll.setFitToWidth(true);
        galleryScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        galleryScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    }

    private void refresh() {
        List<FavoriteData> data = FavoriteRegistry.getInstance().getFavorites();

        // Update List
        listView.setItems(FXCollections.observableArrayList(data));

        // Update Gallery if active
        if (isGalleryMode) {
            updateGallery();
        }
    }

    private void updateGallery() {
        galleryPane.getChildren().clear();
        List<FavoriteData> favorites = FavoriteRegistry.getInstance().getFavorites();

        for (FavoriteData fav : favorites) {
            if (fav.getThumbnailPath() == null) continue;
            File imgFile = new File(fav.getThumbnailPath());
            if (!imgFile.exists()) continue;

            VBox tile = new VBox(5);
            tile.setAlignment(Pos.CENTER);
            tile.setStyle("-fx-background-color: -app-bg-secondary; -fx-background-radius: 8; -fx-padding: 5; -fx-border-color: -app-border-subtle; -fx-border-radius: 8;");
            tile.setCursor(javafx.scene.Cursor.HAND);

            ImageView iv = new ImageView();
            iv.setFitWidth(140);
            iv.setFitHeight(140);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);

            try {
                iv.setImage(new Image(imgFile.toURI().toString(), 140, 140, true, true, true));
            } catch (Exception e) { continue; }

            // Tooltip with truncated prompt
            String promptPreview = fav.getPrompt();
            if (promptPreview != null && promptPreview.length() > 400) {
                promptPreview = promptPreview.substring(0, 400) + "...";
            }
            Tooltip t = new Tooltip("Name: " + fav.getName() + "\nModel: " + fav.getModel() + "\n\n" + promptPreview);
            t.setWrapText(true);
            t.setMaxWidth(350);
            t.setStyle("-fx-font-size: 11px;");
            Tooltip.install(tile, t);

            // Click -> Go to Extractor
            tile.setOnMouseClicked(e -> rootLayout.navigateToExtractor(fav));

            tile.getChildren().add(iv);
            galleryPane.getChildren().add(tile);
        }
    }

    private class FavoriteCardCell extends ListCell<FavoriteData> {
        private final VBox root = new VBox(10);
        private final HBox header = new HBox(15);
        private final HBox body = new HBox(15);
        private final VBox info = new VBox(5);

        private final ImageView thumbnail = new ImageView();
        private final Label nameLabel = new Label();
        private final Label modelLabel = new Label();
        private final Label promptLabel = new Label();

        private final Button viewBtn = new Button();
        private final Button folderBtn = new Button();
        private final Button copyBtn = new Button();
        private final Button deleteBtn = new Button();

        public FavoriteCardCell() {
            root.getStyleClass().add("income-stats-box");
            root.setPadding(new Insets(15));
            root.setMaxWidth(Double.MAX_VALUE);
            this.setStyle("-fx-background-color: transparent; -fx-padding: 5 0;");

            nameLabel.getStyleClass().add("app-title");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            viewBtn.getStyleClass().add("button");
            viewBtn.setGraphic(new FontIcon(FontAwesome.EXTERNAL_LINK));
            viewBtn.setTooltip(new Tooltip("View in Extractor"));

            folderBtn.getStyleClass().add("button");
            folderBtn.setGraphic(new FontIcon(FontAwesome.FOLDER_OPEN));
            folderBtn.setTooltip(new Tooltip("Open Image Location"));

            copyBtn.getStyleClass().add("button");
            copyBtn.setGraphic(new FontIcon(FontAwesome.COPY));
            copyBtn.setTooltip(new Tooltip("Copy Positive Prompt"));

            deleteBtn.getStyleClass().addAll("button", "exit-button");
            deleteBtn.setGraphic(new FontIcon(FontAwesome.TRASH));
            deleteBtn.setTooltip(new Tooltip("Remove Favorite"));

            HBox actions = new HBox(10, viewBtn, folderBtn, copyBtn, deleteBtn);
            actions.setAlignment(Pos.CENTER_RIGHT);
            header.getChildren().addAll(nameLabel, spacer, actions);

            StackPane thumbWrapper = new StackPane(thumbnail);
            thumbWrapper.setPrefSize(80, 80);
            thumbWrapper.setStyle("-fx-background-color: #0b0e14; -fx-background-radius: 6;");
            thumbnail.setFitWidth(75);
            thumbnail.setFitHeight(75);
            thumbnail.setPreserveRatio(true);

            modelLabel.getStyleClass().add("income-stat-label");
            promptLabel.getStyleClass().add("label");
            promptLabel.setWrapText(true);
            promptLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

            // Bind to List width instead of Cell Root width
            promptLabel.maxWidthProperty().bind(listView.widthProperty().subtract(260));

            info.getChildren().addAll(modelLabel, promptLabel);
            HBox.setHgrow(info, Priority.ALWAYS);
            body.getChildren().addAll(thumbWrapper, info);

            root.getChildren().addAll(header, body);
        }

        @Override
        protected void updateItem(FavoriteData item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(item.getName());
                modelLabel.setText("Model: " + item.getModel());
                promptLabel.setText(item.getPrompt());

                if (item.getThumbnailPath() != null) {
                    File thumbFile = new File(item.getThumbnailPath());
                    if (thumbFile.exists()) {
                        // PERFORMANCE FIX: Background loading for list items too
                        thumbnail.setImage(new Image(thumbFile.toURI().toString(), 80, 80, true, true, true));
                    } else {
                        thumbnail.setImage(null);
                    }
                }

                viewBtn.setOnAction(e -> rootLayout.navigateToExtractor(item));

                folderBtn.setOnAction(e -> {
                    if (item.getThumbnailPath() != null) {
                        try {
                            java.awt.Desktop.getDesktop().open(new File(item.getThumbnailPath()).getParentFile());
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }
                });

                copyBtn.setOnAction(e -> {
                    final javafx.scene.input.ClipboardContent c = new javafx.scene.input.ClipboardContent();
                    c.putString(item.getPrompt());
                    javafx.scene.input.Clipboard.getSystemClipboard().setContent(c);
                });

                deleteBtn.setOnAction(e -> {
                    FavoriteRegistry.getInstance().removeFavorite(item);
                    refresh();
                });

                setGraphic(root);
            }
        }
    }
}