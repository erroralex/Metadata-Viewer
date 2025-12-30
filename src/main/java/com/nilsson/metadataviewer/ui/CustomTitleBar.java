package com.nilsson.metadataviewer.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Custom Title Bar implementation for an undecorated Stage.
 * Provides Minimize/Close buttons and window dragging functionality.
 */
public class CustomTitleBar extends HBox {
    private double xOffset = 0;
    private double yOffset = 0;

    public CustomTitleBar(Stage primaryStage, Runnable onExitCleanup) {
        this.getStyleClass().add("custom-title-bar");
        this.setAlignment(Pos.CENTER_LEFT);
        this.setPrefHeight(40);

        Label titleLabel = new Label("Metadata Extractor by ALX v.1.0.3");
        titleLabel.getStyleClass().add("title-label");

        // Spacer to push buttons to the right
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // --- Window Controls ---
        Button minimizeBtn = new Button();
        minimizeBtn.setGraphic(new FontIcon(FontAwesome.MINUS));
        minimizeBtn.getStyleClass().add("window-button");
        minimizeBtn.setOnAction(e -> primaryStage.setIconified(true));

        Button maximizeBtn = new Button();
        maximizeBtn.setGraphic(new FontIcon(FontAwesome.WINDOW_MAXIMIZE));
        maximizeBtn.getStyleClass().add("window-button");
        maximizeBtn.setOnAction(e -> toggleMaximize(primaryStage, maximizeBtn));

        Button closeBtn = new Button();
        closeBtn.setGraphic(new FontIcon(FontAwesome.TIMES));
        closeBtn.getStyleClass().addAll("window-button", "window-close");
        closeBtn.setOnAction(e -> {
            if (onExitCleanup != null) {
                onExitCleanup.run();
            }
            primaryStage.close();
        });

        this.getChildren().addAll(titleLabel, spacer, minimizeBtn, maximizeBtn, closeBtn);

        // --- Dragging Logic ---
        this.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });

        this.setOnMouseDragged(event -> {
            // Disable dragging if maximized
            if (primaryStage.isMaximized()) return;

            if (event.getButton() == MouseButton.PRIMARY) {
                primaryStage.setX(event.getScreenX() - xOffset);
                primaryStage.setY(event.getScreenY() - yOffset);
            }
        });

        // Double-click title bar to maximize
        this.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                toggleMaximize(primaryStage, maximizeBtn);
            }
        });
    }

    private void toggleMaximize(Stage stage, Button btn) {
        boolean max = !stage.isMaximized();
        stage.setMaximized(max);
        // Toggle icon based on state
        if (max) {
            btn.setGraphic(new FontIcon(FontAwesome.WINDOW_RESTORE));
        } else {
            btn.setGraphic(new FontIcon(FontAwesome.WINDOW_MAXIMIZE));
        }
    }
}