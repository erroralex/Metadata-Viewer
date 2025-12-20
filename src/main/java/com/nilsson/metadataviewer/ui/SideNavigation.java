package com.nilsson.metadataviewer.ui;

import com.nilsson.metadataviewer.ui.views.ExtractorView;
import com.nilsson.metadataviewer.ui.views.FavoritesView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

// Persistent Side Navigation for the Metadata Viewer
public class SideNavigation extends VBox {
    private final RootLayout rootLayout;
    private Button activeButton;

    private Button btnExtractor;
    private Button btnFavorites;

    /**
     * Constructor that accepts a RootLayout to enable view switching.
     * @param rootLayout The main layout container for the application.
     */
    public SideNavigation(RootLayout rootLayout) {
        this.rootLayout = rootLayout;
        this.getStyleClass().add("side-navigation");
        this.setSpacing(10);
        this.setPadding(new Insets(20));

        // --- Logo Area ---
        VBox logoArea = new VBox();
        logoArea.getStyleClass().add("profile-area");
        logoArea.setAlignment(Pos.CENTER);
        logoArea.setPadding(new Insets(0, 0, 20, 0));

        try {
            // Ensure logo.png exists in src/main/resources
            String logoPath = getClass().getResource("/logo.png").toExternalForm();
            Image logoImg = new Image(logoPath);
            ImageView logoView = new ImageView(logoImg);

            logoView.setFitWidth(180);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);

            logoArea.getChildren().add(logoView);
        } catch (Exception e) {
            // Fallback text logo if image file is missing
            Label fallbackLogo = new Label("METADATA VIEWER");
            fallbackLogo.getStyleClass().add("app-title");
            logoArea.getChildren().add(fallbackLogo);
        }
        this.getChildren().add(logoArea);

        // --- Navigation Buttons ---

        // 1. Extractor Button
        btnExtractor = createNavButton("Extractor", FontAwesome.DASHBOARD);
        btnExtractor.setOnAction(e -> {
            rootLayout.setContent(new ExtractorView());
            setActiveButton(btnExtractor);
        });

        // 2. Favorites Button - Pass rootLayout to enable "View" jumps
        btnFavorites = createNavButton("Favorites", FontAwesome.STAR);
        btnFavorites.setOnAction(e -> {
            rootLayout.setContent(new FavoritesView(rootLayout));
            setActiveButton(btnFavorites);
        });

        // Bottom Spacer and Exit Logic
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnExit = createNavButton("Exit App", FontAwesome.POWER_OFF);
        btnExit.setOnAction(e -> System.exit(0));

        this.getChildren().addAll(btnExtractor, btnFavorites, spacer, btnExit);

        // Default Active State
        setActiveButton(btnExtractor);
    }

    /**
     * Public method used by RootLayout to highlight the Extractor tab
     * when jumping back from the Favorites library.
     */
    public void highlightExtractor() {
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