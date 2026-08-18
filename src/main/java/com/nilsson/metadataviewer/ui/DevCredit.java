package com.nilsson.metadataviewer.ui;

import javafx.scene.Cursor;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;

import java.awt.Desktop;
import java.net.URI;

/**
 * Lower-left developer credit logo, matching the subtle corner placement
 * used across the other Latent apps (opacity 0.55, full opacity on hover,
 * links to the developer's GitHub).
 */
public final class DevCredit {

    private static final String GITHUB_URL = "https://github.com/erroralex";

    private DevCredit() {
    }

    public static ImageView create() {
        ImageView logo = new ImageView();
        if (DevCredit.class.getResource("/alx_logo.png") != null) {
            logo.setImage(new Image(DevCredit.class.getResource("/alx_logo.png").toExternalForm()));
        }
        logo.setFitWidth(100);
        logo.setFitHeight(36);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        logo.setOpacity(0.55);
        logo.setCursor(Cursor.HAND);

        Tooltip.install(logo, new Tooltip("Alexander Nilsson — GitHub"));

        logo.setOnMouseEntered(e -> logo.setOpacity(1.0));
        logo.setOnMouseExited(e -> logo.setOpacity(0.55));
        logo.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                openLink(GITHUB_URL);
            }
        });

        return logo;
    }

    private static void openLink(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
