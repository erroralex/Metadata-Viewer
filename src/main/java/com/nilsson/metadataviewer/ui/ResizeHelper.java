package com.nilsson.metadataviewer.ui;

import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class ResizeHelper {
    private static final int BORDER = 8;

    public static void addResizeListener(Stage stage) {
        ResizeListener listener = new ResizeListener(stage);

        // Listener to attach event filters whenever the scene is set or changed
        stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                addListeners(newScene, listener);
            }
        });

        // If scene is already available, attach immediately
        if (stage.getScene() != null) {
            addListeners(stage.getScene(), listener);
        }
    }

    private static void addListeners(Scene scene, ResizeListener listener) {
        // USE FILTERS (Capture Phase) instead of Handlers.
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, listener::processMouseMoved);
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, listener::processMousePressed);
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, listener::processMouseDragged);
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, listener::processMouseReleased);
    }

    private static class ResizeListener {
        private final Stage stage;
        private boolean resizing = false;
        private Cursor cursor = Cursor.DEFAULT;

        // Start variables
        private double startScreenX, startScreenY, startW, startH, startX, startY;

        public ResizeListener(Stage stage) {
            this.stage = stage;
        }

        public void processMouseMoved(MouseEvent e) {
            Scene scene = stage.getScene();
            if (scene == null || stage.isMaximized()) return;

            double mouseX = e.getSceneX();
            double mouseY = e.getSceneY();
            double width = scene.getWidth();
            double height = scene.getHeight();

            Cursor newCursor = Cursor.DEFAULT;

            boolean left = mouseX < BORDER;
            boolean right = mouseX > width - BORDER;
            boolean top = mouseY < BORDER;
            boolean bottom = mouseY > height - BORDER;

            if (top && left) newCursor = Cursor.NW_RESIZE;
            else if (top && right) newCursor = Cursor.NE_RESIZE;
            else if (bottom && right) newCursor = Cursor.SE_RESIZE;
            else if (bottom && left) newCursor = Cursor.SW_RESIZE;
            else if (left) newCursor = Cursor.W_RESIZE;
            else if (right) newCursor = Cursor.E_RESIZE;
            else if (top) newCursor = Cursor.N_RESIZE;
            else if (bottom) newCursor = Cursor.S_RESIZE;

            scene.setCursor(newCursor);
            cursor = newCursor; // Store for press event
        }

        public void processMousePressed(MouseEvent e) {
            if (stage.isMaximized() || cursor == Cursor.DEFAULT) return;

            // Start resizing
            resizing = true;
            startW = stage.getWidth();
            startH = stage.getHeight();
            startX = stage.getX();
            startY = stage.getY();
            startScreenX = e.getScreenX();
            startScreenY = e.getScreenY();

            // Consume event so UI elements below don't get clicked
            e.consume();
        }

        public void processMouseDragged(MouseEvent e) {
            if (!resizing || stage.isMaximized()) return;

            double dx = e.getScreenX() - startScreenX;
            double dy = e.getScreenY() - startScreenY;

            // Handle Horizontal Resize (Right / Left)
            if (cursor == Cursor.E_RESIZE || cursor == Cursor.NE_RESIZE || cursor == Cursor.SE_RESIZE) {
                stage.setWidth(Math.max(600, startW + dx));
            } else if (cursor == Cursor.W_RESIZE || cursor == Cursor.NW_RESIZE || cursor == Cursor.SW_RESIZE) {
                double newW = startW - dx;
                if (newW >= 600) {
                    stage.setX(startX + dx);
                    stage.setWidth(newW);
                }
            }

            // Handle Vertical Resize (Bottom / Top)
            if (cursor == Cursor.S_RESIZE || cursor == Cursor.SE_RESIZE || cursor == Cursor.SW_RESIZE) {
                stage.setHeight(Math.max(400, startH + dy));
            } else if (cursor == Cursor.N_RESIZE || cursor == Cursor.NE_RESIZE || cursor == Cursor.NW_RESIZE) {
                double newH = startH - dy;
                if (newH >= 400) {
                    stage.setY(startY + dy);
                    stage.setHeight(newH);
                }
            }

            e.consume();
        }

        public void processMouseReleased(MouseEvent e) {
            if (resizing) {
                resizing = false;
                e.consume();
            }
        }
    }
}