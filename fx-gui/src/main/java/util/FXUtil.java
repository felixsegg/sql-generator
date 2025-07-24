package util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Utility class for common JavaFX operations used throughout the library.
 *
 * <p>
 * Provides helper methods for signaling UI element borders, enabling zoom and pan behavior
 * for images in scroll panes, and creating modal popup windows.
 * </p>
 *
 * <p>
 * All methods are static and the class is not intended to be instantiated.
 * </p>
 *
 * @author Felix Seggebäing
 */
public class FXUtil {
    /**
     * Temporarily highlights the border of the given JavaFX node by making it flash red.
     * <p>
     * Intended to draw the user's attention to a specific UI element (e.g., in case of invalid input).
     * The border will return to its default style after the animation completes.
     *
     * @param node the JavaFX UI element to signal
     */
    public static void signalBorder(Node node) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(100), e -> node.setStyle("-fx-border-color: red; -fx-border-width: 3px;")),
                new KeyFrame(Duration.millis(200), e -> node.setStyle("-fx-border-color: transparent; -fx-border-width: 3px;")),
                new KeyFrame(Duration.millis(300), e -> node.setStyle("-fx-border-color: red; -fx-border-width: 3px;")),
                new KeyFrame(Duration.millis(400), e -> node.setStyle("-fx-border-color: transparent; -fx-border-width: 3px;")),
                new KeyFrame(Duration.millis(500), e -> node.setStyle("-fx-border-color: red; -fx-border-width: 3px;")),
                new KeyFrame(Duration.millis(600), e -> node.setStyle("-fx-border-color: transparent; -fx-border-width: 3px;")),
                new KeyFrame(Duration.millis(700), e -> node.setStyle("-fx-border-color: red; -fx-border-width: 3px;")),
                new KeyFrame(Duration.millis(800), e -> node.setStyle("-fx-border-color: transparent; -fx-border-width: 3px;")),
                new KeyFrame(Duration.millis(900), e -> node.setStyle("-fx-border-color: red; -fx-border-width: 3px;")),
                new KeyFrame(Duration.millis(1000), e -> node.setStyle("")) // Zurück zum Standardstil
        );
        
        timeline.setCycleCount(1);
        timeline.play();
    }
    
    /**
     * Initializes the given {@link ScrollPane} so that it supports zooming and panning for images.
     * <p>
     * Sets up an {@link ImageView} inside a {@link Group} as the scroll pane's content.
     * Enables smooth zooming via mouse wheel (with Ctrl), keyboard shortcuts (Ctrl + Plus/Minus), and panning via mouse drag.
     * The zoom level is dynamically limited so that the image always fits in the viewport (minScale) and cannot exceed 4x zoom (maxScale).
     * <p>
     * Returns a {@link Consumer} that can be used to set a new {@link Image} in the zoomable view,
     * automatically resetting the zoom to the minimum scale so the image fits.
     *
     * @param scrollPane the {@link ScrollPane} to be initialized for zoom and pan functionality
     * @return a consumer that displays an image in the scroll pane and resets zoom
     */
    
    public static Consumer<Image> initializeZoomableScrollPane(ScrollPane scrollPane) {
        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        
        Group container = new Group(imageView);
        scrollPane.setContent(container);
        scrollPane.setPannable(true);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        
        DoubleProperty scale = new SimpleDoubleProperty(1.0);
        DoubleProperty minScale = new SimpleDoubleProperty(1.0);
        double maxScale = 4.0;
        
        imageView.scaleXProperty().bind(scale);
        imageView.scaleYProperty().bind(scale);
        
        // Bei Änderung der Viewport-Größe ggf. Bild-Zoom neu anpassen:
        scrollPane.viewportBoundsProperty().addListener((obs, oldB, newB) -> {
            Image img = imageView.getImage();
            if (img == null) return;
            double sx = newB.getWidth() / img.getWidth();
            double sy = newB.getHeight() / img.getHeight();
            double newMin = Math.max(sx, sy);
            minScale.set(newMin);
            if (scale.get() < newMin) scale.set(newMin);
        });
        
        // Beim Setzen des Bildes auf minScale zoom-out setzen
        imageView.imageProperty().addListener((obs, oldImg, newImg) -> {
            if (newImg == null) return;
            Bounds vb = scrollPane.getViewportBounds();
            double sx = vb.getWidth() / newImg.getWidth();
            double sy = vb.getHeight() / newImg.getHeight();
            double newMin = Math.max(sx, sy);
            minScale.set(newMin);
            scale.set(newMin);
        });
        
        BiFunction<Double, Double, Double> clamp = (v, m) ->
                Math.min(Math.max(v, m), maxScale);
        
        // Zoom per Strg und Maus-Scroll
        scrollPane.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isControlDown()) {
                double delta = e.getDeltaY() > 0 ? 1.1 : 0.9;
                scale.set(clamp.apply(scale.get() * delta, minScale.get()));
                e.consume();
            }
        });
        
        // Zoom per Strg +/-
        scrollPane.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown()) {
                if (e.getCode() == KeyCode.PLUS || e.getCode() == KeyCode.EQUALS) {
                    scale.set(clamp.apply(scale.get() * 1.1, minScale.get()));
                    e.consume();
                } else if (e.getCode() == KeyCode.MINUS) {
                    scale.set(clamp.apply(scale.get() * 0.9, minScale.get()));
                    e.consume();
                }
            }
        });
        
        return imageView::setImage;
    }
    
    /**
     * Creates and configures a modal popup {@link Stage} with the specified content and positioning.
     * <p>
     * The popup is centered with the given offsets relative to the parent window and uses {@link Modality#WINDOW_MODAL}.
     *
     * @param parent   the parent window for modality and positioning
     * @param content  the content to display in the popup
     * @param offsetX  the horizontal offset from the parent window's X position
     * @param offsetY  the vertical offset from the parent window's Y position
     * @param title    the window title for the popup
     * @return the configured and ready-to-show modal {@link Stage}
     */
    public static Stage createModalPopup(Window parent, Parent content, double offsetX, double offsetY, String title) {
        Stage popup = new Stage();
        popup.initOwner(parent);
        popup.initModality(Modality.WINDOW_MODAL);
        popup.setTitle(title);
        
        popup.setScene(new Scene(content));
        
        popup.setX(parent.getX() + offsetX);
        popup.setY(parent.getY() + offsetY);
        
        return popup;
    }
}
