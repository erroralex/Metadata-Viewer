package com.nilsson.metadataviewer.ui.views;

import com.nilsson.metadataviewer.model.FavoriteData;
import com.nilsson.metadataviewer.model.FavoriteRegistry;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * View for managing saved favorites.
 */
public class FavoritesView extends VBox {

    private final TableView<FavoriteData> table = new TableView<>();

    public FavoritesView() {
        this.setPadding(new Insets(30));
        this.setSpacing(15);
        this.getStyleClass().add("content-view");

        Label title = new Label("Saved Favorites");
        title.getStyleClass().add("content-title");

        setupTable();

        Button btnDelete = new Button("Remove Selected");
        btnDelete.getStyleClass().add("exit-button");
        btnDelete.setOnAction(e -> {
            FavoriteData selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                FavoriteRegistry.getInstance().removeFavorite(selected);
                refreshTable();
            }
        });

        this.getChildren().addAll(title, table, btnDelete);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    private void setupTable() {
        TableColumn<FavoriteData, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);

        TableColumn<FavoriteData, String> modelCol = new TableColumn<>("Model");
        modelCol.setCellValueFactory(new PropertyValueFactory<>("model"));
        modelCol.setPrefWidth(150);

        TableColumn<FavoriteData, String> promptCol = new TableColumn<>("Prompt Clip");
        promptCol.setCellValueFactory(new PropertyValueFactory<>("prompt"));
        promptCol.setPrefWidth(400);

        table.getColumns().addAll(nameCol, modelCol, promptCol);
        refreshTable();
    }

    private void refreshTable() {
        table.setItems(FXCollections.observableArrayList(FavoriteRegistry.getInstance().getFavorites()));
    }
}