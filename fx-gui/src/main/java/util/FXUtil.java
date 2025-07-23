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

public class FXUtil {
    /**
     * Lässt den Rand des übergebenen GUI-Elements rot blinken.
     *
     * @param node das GUI-Element, auf das aufmerksam gemacht werden soll
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
     * Initialisiert eine gegebene ScrollPane so, dass darin ein Bild per Mausrad und
     * Tastatur (STRG + PLUS / MINUS) gezoomt und verschoben (panning) werden kann.
     * <p>
     * Dabei wird ein {@link ImageView} mit beibehaltenem Seitenverhältnis in einer
     * {@link Group} als Inhalt der ScrollPane gesetzt. Die Zoom-Stufen werden durch
     * eine dynamisch berechnete minimale Skalierung (minScale) begrenzt, die dafür sorgt,
     * dass das Bild immer vollständig in den aktuellen Viewport passt (maximal herausgezoomt),
     * während die maximale Skalierung (maxScale) arbiträr auf 4x festgelegt ist.
     * <p>
     * Folgende Features werden bereitgestellt:
     * <ul>
     *   <li>Automatische Neuberechnung von minScale bei Änderung der Viewport-Größe
     *       (Listener auf viewportBoundsProperty).</li>
     *   <li>Initiales Herauszoomen, sobald ein neues Bild gesetzt wird
     *       (Listener auf imageProperty des ImageView): scale wird auf minScale gesetzt.</li>
     *   <li>Mausrad-Zoom (STRG gedrückt) mit stufenweiser Skalierung um Faktor ±10 %.</li>
     *   <li>Tastatur-Zoom über STRG + PLUS/EQUALS (reinzoomen) und STRG + MINUS
     *       (herauszoomen), ebenfalls ±10 % pro Tastendruck.</li>
     *   <li>Panning der Bildansicht durch Ziehen innerhalb der ScrollPane.</li>
     * </ul>
     *
     * @param scrollPane Die ScrollPane, die als Container für das zoom- und panning-fähige Bild dienen soll.
     * @return Ein {@code Consumer<Image>}, der beim Aufruf das übergebene {@link Image} in das
     * zugeordnete {@link ImageView} einfügt und dabei automatisch auf die kleinstmögliche
     * Zoom-Stufe (minScale) einstellt.
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
