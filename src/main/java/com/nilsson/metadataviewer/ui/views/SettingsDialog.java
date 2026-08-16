package com.nilsson.metadataviewer.ui.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.awt.Desktop;
import java.net.URI;

public class SettingsDialog {

    private static final String VERSION = "1.1.0";
    private static final String SPONSOR_GITHUB_URL = "https://github.com/sponsors/erroralex";
    private static final String SPONSOR_KOFI_URL = "https://ko-fi.com/error_alex";

    private SettingsDialog() {
    }

    public static void show(Window owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);

        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("custom-dialog");
        if (owner != null && owner.getScene() != null) {
            pane.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        pane.getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(15);
        content.setPadding(new Insets(25));
        content.setAlignment(Pos.CENTER);
        content.setPrefWidth(320);

        ImageView logoView = new ImageView();
        if (SettingsDialog.class.getResource("/alx_logo.png") != null) {
            logoView.setImage(new Image(SettingsDialog.class.getResource("/alx_logo.png").toExternalForm()));
        }
        logoView.setFitWidth(120);
        logoView.setPreserveRatio(true);
        logoView.setSmooth(true);

        Label appName = new Label("Metadata Extractor");
        appName.getStyleClass().add("app-title");

        Label version = new Label("v" + VERSION);
        version.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 0.85em;");

        Label movedNote = new Label(
                "Favorites, the Metadata Scrubber, and Speed Sorter have moved to Latent Library.");
        movedNote.setWrapText(true);
        movedNote.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 0.85em; -fx-text-alignment: center;");
        movedNote.setAlignment(Pos.CENTER);

        HBox links = new HBox(10);
        links.setAlignment(Pos.CENTER);

        Button sponsorGithubBtn = new Button("GitHub Sponsors");
        sponsorGithubBtn.getStyleClass().add("button");
        sponsorGithubBtn.setOnAction(e -> openLink(SPONSOR_GITHUB_URL));

        Button sponsorKofiBtn = new Button("Ko-fi");
        sponsorKofiBtn.getStyleClass().add("button");
        sponsorKofiBtn.setOnAction(e -> openLink(SPONSOR_KOFI_URL));

        links.getChildren().addAll(sponsorGithubBtn, sponsorKofiBtn);

        content.getChildren().addAll(logoView, appName, version, movedNote, links);
        pane.setContent(content);

        dialog.showAndWait();
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
