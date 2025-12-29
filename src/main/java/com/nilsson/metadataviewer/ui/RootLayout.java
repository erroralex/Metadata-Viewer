package com.nilsson.metadataviewer.ui;

import com.nilsson.metadataviewer.model.FavoriteData;
import com.nilsson.metadataviewer.ui.views.ExtractorView;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class RootLayout extends BorderPane {
    private SideNavigation sideNav;

    // 1. Maintain a single, persistent instance of the view
    private final ExtractorView extractorView;

    public RootLayout(Stage stage, CustomTitleBar titleBar) {
        this.getStyleClass().add("root-layout");

        // 2. Initialize it only ONCE
        this.extractorView = new ExtractorView();

        this.sideNav = new SideNavigation(this);
        this.sideNav.setPrefWidth(260);
        this.setLeft(sideNav);

        // 3. Set the persistent view as the default content
        setContent(this.extractorView);
    }

    public void setContent(Node view) {
        this.setCenter(view);
    }

    // Helper to jump to Extractor
    public void navigateToExtractor(FavoriteData favorite) {
        // Only update data if a specific favorite was clicked.
        // If 'favorite' is null (tab switch), this is skipped, preserving current state.
        if (favorite != null) {
            this.extractorView.populateFromFavorite(favorite);
        }

        // 5. Display the cached instance
        setContent(this.extractorView);
        sideNav.highlightExtractor();
    }
    // Helper to jump to Favorites
    public void navigateToFavorites(FavoriteData data) {
        // Logic to switch center view to FavoritesView
        setContent(new com.nilsson.metadataviewer.ui.views.FavoritesView(this));
    }
}