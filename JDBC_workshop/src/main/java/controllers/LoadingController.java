package controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Duration;

public class LoadingController {

    @FXML
    private Label rehLabel;

    @FXML
    private Label letLabel;

    @FXML
    private Label natnLabel;

    private int dots = 0;

    @FXML
    public void initialize() {

        // Shadow effect
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(255,255,255,0.5));
        shadow.setRadius(25);

        rehLabel.setEffect(shadow);
        letLabel.setEffect(shadow);
        natnLabel.setEffect(shadow);

        // Start sophisticated loading animation
        startSophisticatedLoading();
    }

    private void startSophisticatedLoading() {
        // Hide all initial labels
        rehLabel.setOpacity(0);
        letLabel.setOpacity(0);
        natnLabel.setOpacity(0);

        // Store original labels for later restoration
        Label originalReh = rehLabel;
        Label originalLet = letLabel;
        Label originalNatn = natnLabel;

        // Get container
        HBox container = (HBox) rehLabel.getParent();

        // Create a VBox to hold loading text and line animation
        VBox loadingContainer = new VBox(30);
        loadingContainer.setAlignment(Pos.CENTER);

        // Create loading text
        Label loadingText = new Label("Loading");
        loadingText.setStyle("-fx-font-size: 72px; " +
                "-fx-font-weight: 900; " +
                "-fx-text-fill: white;");

        // Add shadow to loading text
        DropShadow textShadow = new DropShadow();
        textShadow.setColor(Color.rgb(255,255,255,0.3));
        textShadow.setRadius(20);
        loadingText.setEffect(textShadow);

        // Create line and airplane container
        StackPane lineContainer = new StackPane();
        lineContainer.setPrefWidth(400);
        lineContainer.setPrefHeight(60);

        // Create the line (background track)
        Line trackLine = new Line(0, 0, 400, 0);
        trackLine.setStroke(Color.web("#0FA5A2"));
        trackLine.setStrokeWidth(4);
        trackLine.setOpacity(0.3);
        trackLine.setTranslateY(0);

        // Create the progress line (fills up)
        Line progressLine = new Line(0, 0, 0, 0);
        progressLine.setStroke(Color.web("#0FA5A2"));
        progressLine.setStrokeWidth(4);
        progressLine.setTranslateY(0);

        // Create airplane emoji (using Label with Unicode)
        Label airplane = new Label("✈");
        airplane.setStyle("-fx-font-size: 30px;");
        airplane.setTranslateY(-15);

        // Position above the line

        // Add all to line container
        lineContainer.getChildren().addAll(trackLine, progressLine, airplane);

        // Create dots container (keep original dots)
        HBox dotsContainer = new HBox(8);
        dotsContainer.setAlignment(Pos.CENTER);

        // Create 3 dots with different colors
        Label[] dots = new Label[3];
        Color[] dotColors = {Color.web("#0FA5A2"), Color.web("#FEC74C"), Color.web("#1D4D7C")};

        for (int i = 0; i < 3; i++) {
            dots[i] = new Label(".");
            dots[i].setStyle("-fx-font-size: 72px; " +
                    "-fx-font-weight: 900; " +
                    "-fx-text-fill: " + toHexString(dotColors[i]) + ";");
            dots[i].setOpacity(0);

            // Add individual shadows
            DropShadow dotShadow = new DropShadow();
            dotShadow.setColor(Color.rgb(255,255,255,0.2));
            dotShadow.setRadius(15);
            dots[i].setEffect(dotShadow);

            dotsContainer.getChildren().add(dots[i]);
        }

        // Add everything to loading container
        loadingContainer.getChildren().addAll(loadingText, lineContainer, dotsContainer);

        // Clear container and add loading container
        container.getChildren().clear();
        container.getChildren().add(loadingContainer);

        // Create pulse animation for loading text
        Timeline pulseText = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(loadingText.scaleXProperty(), 1),
                        new KeyValue(loadingText.scaleYProperty(), 1),
                        new KeyValue(loadingText.opacityProperty(), 1)),
                new KeyFrame(Duration.seconds(1),
                        new KeyValue(loadingText.scaleXProperty(), 1.1),
                        new KeyValue(loadingText.scaleYProperty(), 1.1),
                        new KeyValue(loadingText.opacityProperty(), 0.8)),
                new KeyFrame(Duration.seconds(2),
                        new KeyValue(loadingText.scaleXProperty(), 1),
                        new KeyValue(loadingText.scaleYProperty(), 1),
                        new KeyValue(loadingText.opacityProperty(), 1))
        );
        pulseText.setCycleCount(Animation.INDEFINITE);
        pulseText.play();

        // Animate the line filling up over 4 seconds
        Timeline lineAnimation = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(progressLine.endXProperty(), 0),
                        new KeyValue(airplane.translateXProperty(), 0)),
                new KeyFrame(Duration.seconds(4),
                        new KeyValue(progressLine.endXProperty(), 400),
                        new KeyValue(airplane.translateXProperty(), 190))
        );
        lineAnimation.setCycleCount(Animation.INDEFINITE);
        lineAnimation.play();

        // Animate dots appearing one by one with bounce effect
        Timeline dotsAppear = new Timeline();

        for (int i = 0; i < 3; i++) {
            final int index = i;

            // Fade in
            KeyFrame fadeFrame = new KeyFrame(
                    Duration.seconds(i * 0.4),
                    e -> {
                        FadeTransition ft = new FadeTransition(Duration.millis(300), dots[index]);
                        ft.setToValue(1);
                        ft.play();
                    }
            );
            dotsAppear.getKeyFrames().add(fadeFrame);

            // Bounce animation for each dot
            KeyFrame bounceFrame = new KeyFrame(
                    Duration.seconds(i * 0.4 + 0.3),
                    e -> {
                        Timeline bounce = new Timeline(
                                new KeyFrame(Duration.millis(0),
                                        new KeyValue(dots[index].translateYProperty(), 0)),
                                new KeyFrame(Duration.millis(150),
                                        new KeyValue(dots[index].translateYProperty(), -25)),
                                new KeyFrame(Duration.millis(300),
                                        new KeyValue(dots[index].translateYProperty(), 0))
                        );
                        bounce.setCycleCount(2);
                        bounce.play();
                    }
            );
            dotsAppear.getKeyFrames().add(bounceFrame);
        }

        // Reset dots after they've all appeared
        KeyFrame resetFrame = new KeyFrame(
                Duration.seconds(2.5),
                e -> {
                    // Fade out all dots
                    for (int i = 0; i < 3; i++) {
                        FadeTransition ft = new FadeTransition(Duration.millis(300), dots[i]);
                        ft.setToValue(0);
                        ft.play();
                    }
                }
        );
        dotsAppear.getKeyFrames().add(resetFrame);

        dotsAppear.setCycleCount(3); // Repeat the whole pattern 3 times

        // After dots animation completes, fade to logo sequence
        dotsAppear.setOnFinished(e -> {
            // Stop animations
            pulseText.stop();
            lineAnimation.stop();

            // Fade out loading container
            FadeTransition fadeOutContainer = new FadeTransition(Duration.millis(500), loadingContainer);
            fadeOutContainer.setToValue(0);

            fadeOutContainer.setOnFinished(e2 -> {
                // Clear container and restore original labels
                container.getChildren().clear();
                container.getChildren().addAll(originalReh, originalLet, originalNatn);
                container.setOpacity(1);

                // Reset opacities
                originalReh.setOpacity(1);
                originalReh.setScaleX(1);
                originalReh.setScaleY(1);

                originalLet.setOpacity(0);
                originalLet.setScaleX(0.9);
                originalLet.setScaleY(0.9);

                originalNatn.setOpacity(0);
                originalNatn.setScaleX(0.9);
                originalNatn.setScaleY(0.9);

                // Start original sequence
                startLoadingSequence();
            });

            fadeOutContainer.play();
        });

        dotsAppear.play();
    }

    private void startLoadingSequence() {
        // Show Reh for 2 seconds
        Timeline showReh = new Timeline(
                new KeyFrame(Duration.seconds(2), e -> {
                    // After 2 seconds, transition to "let"
                    transitionToLet();
                })
        );
        showReh.play();
    }

    private void transitionToLet() {
        // Animate Reh out and Let in
        animateTransition(rehLabel, letLabel, () -> {
            // After transition completes, show Let for 2 seconds
            Timeline showLet = new Timeline(
                    new KeyFrame(Duration.seconds(2), e -> {
                        // After 2 seconds, transition to "na.tn"
                        transitionToNatn();
                    })
            );
            showLet.play();
        });
    }

    private void transitionToNatn() {
        // Animate Let out and Natn in
        animateTransition(letLabel, natnLabel, () -> {
            // After transition completes, start dots animation on "na.tn"
            startDotsAnimation();

            // Keep "na.tn" visible for 3 seconds
            Timeline showNatn = new Timeline(
                    new KeyFrame(Duration.seconds(3), e -> {
                        // Optional: Add any final action here
                        System.out.println("Loading sequence complete");
                    })
            );
            showNatn.play();
        });
    }

    private void animateTransition(Label hide, Label show, Runnable onComplete) {
        // Hide animation
        FadeTransition fadeOut = new FadeTransition(Duration.millis(800), hide);
        fadeOut.setToValue(0);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(800), hide);
        scaleOut.setToX(0.9);
        scaleOut.setToY(0.9);

        ParallelTransition hideAnim = new ParallelTransition(fadeOut, scaleOut);

        // Show animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), show);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(800), show);
        scaleIn.setFromX(1.1);
        scaleIn.setFromY(1.1);
        scaleIn.setToX(1);
        scaleIn.setToY(1);

        ParallelTransition showAnim = new ParallelTransition(fadeIn, scaleIn);

        // Chain animations
        hideAnim.setOnFinished(e -> {
            showAnim.play();
        });

        showAnim.setOnFinished(e -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });

        hideAnim.play();
    }

    private void startDotsAnimation() {
        Timeline dotsAnimation = new Timeline(
                new KeyFrame(Duration.seconds(0.6), e -> {
                    dots = (dots + 1) % 4;

                    StringBuilder txt = new StringBuilder("na.tn");
                    for (int i = 0; i < dots; i++) {
                        txt.append(".");
                    }

                    natnLabel.setText(txt.toString());
                })
        );

        dotsAnimation.setCycleCount(Animation.INDEFINITE);
        dotsAnimation.play();
    }

    // Helper method to convert Color to hex string
    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}