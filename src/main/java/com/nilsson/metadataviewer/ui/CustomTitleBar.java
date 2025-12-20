package com.nilsson.metadataviewer.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

/**
 * Custom Title Bar implementation for an undecorated Stage.
 * Provides Minimize/Close buttons and window dragging functionality.
 */
public class CustomTitleBar extends HBox {
    private double xOffset = 0;
    private double yOffset = 0;

    /**
     * Constructor matching the requirements in MetadataApp.
     * @param primarystage The stage to control (minimize/close/drag)
     * @param onExitCleanup The logic to run when the close button is clicked
     */
    public CustomTitleBar(Stage primarystage, Runnable onExitCleanup) {
        this.getStyleClass().add("custom-title-bar");
        this.setAlignment(Pos.CENTER_LEFT);
        this.setPrefHeight(40);

        Label titleLabel = new Label("Metadata Viewer - AI Extractor");
        titleLabel.getStyleClass().add("title-label");

        // Spacer to push buttons to the right
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Window Controls
        Button minimizeBtn = new Button("—");
        minimizeBtn.getStyleClass().add("window-button");
        minimizeBtn.setOnAction(e -> primarystage.setIconified(true));

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().addAll("window-button", "window-close");
        closeBtn.setOnAction(e -> {
            if (onExitCleanup != null) {
                onExitCleanup.run();
            }
            primarystage.close();
        });

        this.getChildren().addAll(titleLabel, spacer, minimizeBtn, closeBtn);

        // Window Dragging Logic
        this.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        this.setOnMouseDragged(event -> {
            primarystage.setX(event.getScreenX() - xOffset);
            primarystage.setY(event.getScreenY() - yOffset);
        });
    }
}