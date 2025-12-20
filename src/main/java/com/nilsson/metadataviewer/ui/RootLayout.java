package com.nilsson.metadataviewer.ui;

import com.nilsson.metadataviewer.ui.views.ExtractorView;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.io.File;

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
    public void navigateToExtractor(File file) {
        ExtractorView view = new ExtractorView();
        if (file != null && file.exists()) {
            view.process(file);
        }
        setContent(view);
        sideNav.highlightExtractor();
    }
}