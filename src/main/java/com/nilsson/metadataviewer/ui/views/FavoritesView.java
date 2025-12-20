package com.nilsson.metadataviewer.ui.views;

import com.nilsson.metadataviewer.model.FavoriteData;
import com.nilsson.metadataviewer.model.FavoriteRegistry;
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

/**
 * Modern Favorites View using a card-based ListView.
 * Updated to use the consistent lighter "Obsidian" background and render thumbnails.
 */
public class FavoritesView extends VBox {

    private final ListView<FavoriteData> listView = new ListView<>();

    public FavoritesView() {
        this.setPadding(new Insets(30));
        this.setSpacing(20);
        this.getStyleClass().add("content-view");

        // Set the background to match the lighter "panel" gray (#1c212b)
        this.setStyle("-fx-background-color: #1c212b;");

        Label title = new Label("Saved Favorites Library");
        title.getStyleClass().add("content-title");

        setupListView();

        Button btnRefresh = new Button("Sync Library");
        btnRefresh.setGraphic(new FontIcon(FontAwesome.REFRESH));
        btnRefresh.getStyleClass().add("button");
        btnRefresh.setOnAction(e -> refresh());

        HBox footer = new HBox(btnRefresh);
        footer.setAlignment(Pos.CENTER_RIGHT);

        this.getChildren().addAll(title, listView, footer);
        VBox.setVgrow(listView, Priority.ALWAYS);
    }

    private void setupListView() {
        listView.setCellFactory(param -> new FavoriteCardCell());
        listView.getStyleClass().add("list-view");

        // Ensure the list itself is transparent to see the VBox background
        listView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");

        refresh();
    }

    private void refresh() {
        listView.setItems(FXCollections.observableArrayList(FavoriteRegistry.getInstance().getFavorites()));
    }

    /**
     * Custom ListCell that renders each favorite as a card with a thumbnail.
     */
    private class FavoriteCardCell extends ListCell<FavoriteData> {
        private final VBox root = new VBox(10);
        private final HBox header = new HBox(15);
        private final HBox body = new HBox(15);
        private final VBox info = new VBox(5);

        private final ImageView thumbnail = new ImageView();
        private final Label nameLabel = new Label();
        private final Label modelLabel = new Label();
        private final Label promptLabel = new Label();

        private final Button copyBtn = new Button();
        private final Button deleteBtn = new Button();

        public FavoriteCardCell() {
            // Card styling
            root.getStyleClass().add("income-stats-box");
            root.setPadding(new Insets(15));
            root.setMaxWidth(Double.MAX_VALUE);

            // Cell background fix
            this.setStyle("-fx-background-color: transparent; -fx-padding: 5 0;");

            // --- Header Section ---
            nameLabel.getStyleClass().add("app-title");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            copyBtn.getStyleClass().add("button");
            copyBtn.setGraphic(new FontIcon(FontAwesome.COPY));
            copyBtn.setTooltip(new Tooltip("Copy Prompt"));

            deleteBtn.getStyleClass().addAll("button", "exit-button");
            deleteBtn.setGraphic(new FontIcon(FontAwesome.TRASH));
            deleteBtn.setTooltip(new Tooltip("Remove Favorite"));

            HBox actions = new HBox(10, copyBtn, deleteBtn);
            actions.setAlignment(Pos.CENTER_RIGHT);
            header.getChildren().addAll(nameLabel, spacer, actions);
            header.setAlignment(Pos.CENTER_LEFT);

            // --- Body Section (Thumbnail + Metadata) ---
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

            // Bind width to parent for wrapping
            promptLabel.maxWidthProperty().bind(root.widthProperty().subtract(120));

            info.getChildren().addAll(modelLabel, promptLabel);
            HBox.setHgrow(info, Priority.ALWAYS);

            body.getChildren().addAll(thumbWrapper, info);
            body.setAlignment(Pos.TOP_LEFT);

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

                // Thumbnail loading
                if (item.getImagePath() != null) {
                    File imgFile = new File(item.getImagePath());
                    if (imgFile.exists()) {
                        thumbnail.setImage(new Image(imgFile.toURI().toString(), 80, 80, true, true));
                    } else {
                        thumbnail.setImage(null);
                    }
                } else {
                    thumbnail.setImage(null);
                }

                copyBtn.setOnAction(e -> {
                    final javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
                    final javafx.scene.input.ClipboardContent c = new javafx.scene.input.ClipboardContent();
                    c.putString(item.getPrompt());
                    cb.setContent(c);
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