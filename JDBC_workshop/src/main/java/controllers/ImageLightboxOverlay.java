package controllers;

import javafx.animation.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * ImageLightboxOverlay
 *
 * Injects a full-screen overlay into the dashboard's contentContainer (StackPane).
 * Click image, click backdrop, or press ESC to dismiss.
 * Smooth zoom-in spring animation in, zoom-out + fade on dismiss.
 */
public class ImageLightboxOverlay {

    private static final Duration ANIM_IN  = Duration.millis(280);
    private static final Duration ANIM_OUT = Duration.millis(200);

    public static void show(StackPane root, Image image) {
        if (image == null) return;

        // ── Dim backdrop ──────────────────────────────────────────────────────
        Rectangle backdrop = new Rectangle();
        backdrop.setFill(Color.rgb(0, 0, 0, 0.85));
        backdrop.widthProperty().bind(root.widthProperty());
        backdrop.heightProperty().bind(root.heightProperty());

        // ── Focused ImageView ─────────────────────────────────────────────────
        ImageView focusedImage = new ImageView(image);
        focusedImage.setPreserveRatio(true);
        focusedImage.setSmooth(true);
        focusedImage.fitWidthProperty().bind(root.widthProperty().multiply(0.90));
        focusedImage.fitHeightProperty().bind(root.heightProperty().multiply(0.90));
        focusedImage.setStyle("-fx-cursor: hand;");

        // ── Close hint ────────────────────────────────────────────────────────
        Label hint = new Label("Click image or press ESC to close");
        hint.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.40);" +
                        "-fx-font-size: 12px;"
        );

        VBox imageBox = new VBox(8, focusedImage, hint);
        imageBox.setAlignment(javafx.geometry.Pos.CENTER);

        // ── Overlay ───────────────────────────────────────────────────────────
        StackPane overlay = new StackPane(backdrop, imageBox);
        overlay.setAlignment(javafx.geometry.Pos.CENTER);
        overlay.setFocusTraversable(true);

        root.getChildren().add(overlay);
        overlay.requestFocus();

        // ── Dismiss logic ─────────────────────────────────────────────────────
        Runnable dismiss = () -> animateOut(overlay, root);
        backdrop.setOnMouseClicked(e -> dismiss.run());
        focusedImage.setOnMouseClicked(e -> dismiss.run());
        overlay.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) dismiss.run(); });

        // ── Animate IN ────────────────────────────────────────────────────────
        backdrop.setOpacity(0);
        imageBox.setOpacity(0);

        FadeTransition fadeBg = new FadeTransition(ANIM_IN, backdrop);
        fadeBg.setFromValue(0); fadeBg.setToValue(1);
        fadeBg.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scaleIn = new ScaleTransition(ANIM_IN, imageBox);
        scaleIn.setFromX(0.5); scaleIn.setFromY(0.5);
        scaleIn.setToX(1.0);   scaleIn.setToY(1.0);
        // Cubic bezier giving a satisfying spring overshoot
        scaleIn.setInterpolator(new Interpolator() {
            @Override
            protected double curve(double t) {
                // Manual spring: overshoot at ~70%, settles at 1.0
                return 1 - Math.pow(1 - t, 3) * Math.cos(t * Math.PI * 2.4);
            }
        });

        FadeTransition fadeImg = new FadeTransition(ANIM_IN, imageBox);
        fadeImg.setFromValue(0); fadeImg.setToValue(1);
        fadeImg.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fadeBg, scaleIn, fadeImg).play();
    }

    private static void animateOut(StackPane overlay, StackPane root) {
        ScaleTransition scaleOut = new ScaleTransition(ANIM_OUT, overlay);
        scaleOut.setFromX(1.0); scaleOut.setFromY(1.0);
        scaleOut.setToX(0.78);  scaleOut.setToY(0.78);
        scaleOut.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fadeOut = new FadeTransition(ANIM_OUT, overlay);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition out = new ParallelTransition(scaleOut, fadeOut);
        out.setOnFinished(e -> root.getChildren().remove(overlay));
        out.play();
    }
}