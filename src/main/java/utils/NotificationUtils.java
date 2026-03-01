package utils;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class NotificationUtils {

    public static void showNotification(StackPane container, String message, Runnable onFinished) {
        Label toast = new Label(message);
        toast.getStyleClass().add("notification-toast");
        toast.setMaxWidth(Double.MAX_VALUE);
        toast.setAlignment(Pos.CENTER);
        toast.setTranslateY(-50);

        container.getChildren().add(toast);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), toast);
        slideIn.setToY(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ParallelTransition enter = new ParallelTransition(slideIn, fadeIn);

        // Pause de 2 secondes
        PauseTransition delay = new PauseTransition(Duration.seconds(2));

        // Sortie : glissement + disparition
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), toast);
        slideOut.setToY(-50);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
        fadeOut.setToValue(0);

        ParallelTransition exit = new ParallelTransition(slideOut, fadeOut);

        exit.setOnFinished(e -> {
            container.getChildren().remove(toast);
            if (onFinished != null) onFinished.run();
        });

        enter.setOnFinished(e -> delay.play());
        delay.setOnFinished(e -> exit.play());

        enter.play();
    }
}