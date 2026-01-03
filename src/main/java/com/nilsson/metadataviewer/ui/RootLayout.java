package com.nilsson.metadataviewer.ui;

import com.nilsson.metadataviewer.model.FavoriteData;
import com.nilsson.metadataviewer.ui.views.ExtractorView;
import com.nilsson.metadataviewer.ui.views.FavoritesView;
import com.nilsson.metadataviewer.ui.views.ScrubView;
import com.nilsson.metadataviewer.ui.views.SpeedSorterView;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class RootLayout extends BorderPane {
    private SideNavigation sideNav;

    // Persistent Views
    private final ExtractorView extractorView;
    private final ScrubView scrubView;
    private final SpeedSorterView speedSorterView; // New Persistent View

    public RootLayout(Stage stage, CustomTitleBar titleBar) {
        this.getStyleClass().add("root-layout");

        // Initialize views ONCE
        this.extractorView = new ExtractorView();
        this.scrubView = new ScrubView();
        this.speedSorterView = new SpeedSorterView(); // Init

        this.sideNav = new SideNavigation(this);
        this.sideNav.setPrefWidth(260);
        this.setLeft(sideNav);

        // Default content
        setContent(this.extractorView);
    }

    public void setContent(Node view) {
        this.setCenter(view);
    }

    public void navigateToExtractor(FavoriteData favorite) {
        if (favorite != null) {
            this.extractorView.populateFromFavorite(favorite);
        }
        setContent(this.extractorView);
        sideNav.highlightExtractor();
    }

    public void navigateToFavorites(FavoriteData data) {
        // Favorites view is usually refreshed on load, so we create new or reuse
        setContent(new FavoritesView(this));
        sideNav.highlightFavorites();
    }

    public void navigateToScrub() {
        setContent(this.scrubView);
        sideNav.highlightScrub();
    }

    // New Navigation Method
    public void navigateToSpeedSorter() {
        setContent(this.speedSorterView);
        sideNav.highlightSort();
        // Request focus so keyboard shortcuts work immediately
        this.speedSorterView.requestFocus();
    }
}