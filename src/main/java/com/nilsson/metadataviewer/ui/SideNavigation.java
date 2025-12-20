package com.nilsson.metadataviewer.ui;

import com.nilsson.metadataviewer.ui.views.ExtractorView;
import com.nilsson.metadataviewer.ui.views.FavoritesView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Persistent Side Navigation for the Metadata Viewer.
 * Optimized for Java 8 and Ikonli 2.4.0.
 */
public class SideNavigation extends VBox {
    private final RootLayout rootLayout;
    private Button activeButton;

    /**
     * Constructor that accepts a RootLayout to enable view switching.
     * @param rootLayout The main layout container for the application.
     */
    public SideNavigation(RootLayout rootLayout) {
        this.rootLayout = rootLayout;
        this.getStyleClass().add("side-navigation");
        this.setSpacing(10);
        this.setPadding(new Insets(20));

        // Profile Area
        VBox profileArea = new VBox();
        profileArea.getStyleClass().add("profile-area");
        profileArea.setAlignment(Pos.CENTER);
        FontIcon userIcon = new FontIcon(FontAwesome.USER);
        userIcon.setIconSize(40);
        profileArea.getChildren().addAll(userIcon, new Label("Developer"));
        this.getChildren().add(profileArea);

        // Navigation Buttons
        Button btnExtractor = createNavButton("Extractor", FontAwesome.DASHBOARD);
        btnExtractor.setOnAction(e -> {
            rootLayout.setContent(new ExtractorView());
            setActiveButton(btnExtractor);
        });

        Button btnFavorites = createNavButton("Favorites", FontAwesome.STAR);
        btnFavorites.setOnAction(e -> {
            rootLayout.setContent(new FavoritesView());
            setActiveButton(btnFavorites);
        });

        // Bottom Spacer and Exit Logic
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnExit = createNavButton("Exit App", FontAwesome.POWER_OFF);
        btnExit.setOnAction(e -> System.exit(0));

        this.getChildren().addAll(btnExtractor, btnFavorites, spacer, btnExit);
        setActiveButton(btnExtractor);
    }

    private Button createNavButton(String text, org.kordamp.ikonli.Ikon iconCode) {
        Button btn = new Button(text);
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(20);
        btn.setGraphic(icon);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setGraphicTextGap(10);
        return btn;
    }

    private void setActiveButton(Button btn) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button-active");
        }
        btn.getStyleClass().add("nav-button-active");
        activeButton = btn;
    }
}