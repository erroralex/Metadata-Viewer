package com.nilsson.metadataviewer.ui;

import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

public class CustomTitleBar extends HBox {
    private double xOffset = 0;
    private double yOffset = 0;

    public CustomTitleBar(Stage primaryStage, Runnable onExitCleanup) {
        this.getStyleClass().add("custom-title-bar");
        this.setAlignment(Pos.CENTER_LEFT);
        this.setPrefHeight(40);

        Label titleLabel = new Label("Metadata Extractor by ALX v.1.0.7");
        titleLabel.getStyleClass().add("title-label");

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
            if (onExitCleanup != null) onExitCleanup.run();
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
            if (primaryStage.isMaximized()) return;

            if (event.getButton() == MouseButton.PRIMARY) {
                double newX = event.getScreenX() - xOffset;
                double newY = event.getScreenY() - yOffset;

                // --- CONSTRAINT LOGIC ---
                // Get the screen the window is currently mostly on
                Screen screen = Screen.getScreensForRectangle(newX, newY, 100, 100).get(0);
                Rectangle2D bounds = screen.getVisualBounds();

                // Simple snap/constraint: Prevent the top of the title bar from going above the screen
                // or too far below the taskbar.
                if (newY < bounds.getMinY()) {
                    newY = bounds.getMinY();
                }

                // Allow moving partially off-screen horizontally (standard OS behavior),
                // but strictly constrain the Y-axis so the title bar is always accessible.
                primaryStage.setX(newX);
                primaryStage.setY(newY);
            }
        });

        this.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                toggleMaximize(primaryStage, maximizeBtn);
            }
        });
    }

    private void toggleMaximize(Stage stage, Button btn) {
        // Fix for Undecorated Maximize covering Taskbar
        if (stage.isMaximized()) {
            stage.setMaximized(false);
            btn.setGraphic(new FontIcon(FontAwesome.WINDOW_MAXIMIZE));
        } else {
            // We can rely on standard setMaximized for most cases,
            // but for undecorated stages, this ensures it respects taskbar bounds explicitly.
            stage.setMaximized(true);
            btn.setGraphic(new FontIcon(FontAwesome.WINDOW_RESTORE));
        }
    }
}