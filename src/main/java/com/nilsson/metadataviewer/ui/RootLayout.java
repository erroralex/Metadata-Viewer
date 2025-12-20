package com.nilsson.metadataviewer.ui;

import com.nilsson.metadataviewer.ui.views.ExtractorView;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class RootLayout extends BorderPane {

    public RootLayout(Stage stage, CustomTitleBar titleBar) {
        this.getStyleClass().add("root-layout");

        // Persistent Side Nav
        SideNavigation sideNav = new SideNavigation(this);
        sideNav.setPrefWidth(260);
        this.setLeft(sideNav);

        // Default Content
        setContent(new ExtractorView());
    }

    public void setContent(Node view) {
        this.setCenter(view);
    }
}