package com.nilsson.metadataviewer.ui;

import com.nilsson.metadataviewer.model.FavoriteData;
import com.nilsson.metadataviewer.ui.views.ExtractorView;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class RootLayout extends BorderPane {
    private SideNavigation sideNav;

    public RootLayout(Stage stage, CustomTitleBar titleBar) {
        this.getStyleClass().add("root-layout");

        this.sideNav = new SideNavigation(this);
        this.sideNav.setPrefWidth(260);
        this.setLeft(sideNav);

        setContent(new ExtractorView());
    }

    public void setContent(Node view) {
        this.setCenter(view);
    }

    // Helper to jump to Extractor and load a specific file.
    public void navigateToExtractor(FavoriteData favorite) {
        ExtractorView view = new ExtractorView();
        if (favorite != null) {
            view.populateFromFavorite(favorite);
        }
        setContent(view);
        sideNav.highlightExtractor();
    }
}