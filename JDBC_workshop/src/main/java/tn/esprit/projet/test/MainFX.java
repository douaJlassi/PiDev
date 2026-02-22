package tn.esprit.projet.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.projet.controlles.AjouterPersonne;

import java.io.IOException;

public class MainFX extends Application {

    static {
        System.out.println("=== System Properties ===");
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("Java home: " + System.getProperty("java.home"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("OS arch: " + System.getProperty("os.arch"));

        // Set OpenCV to use less memory
        System.setProperty("opencv.threads", "2");
        System.setProperty("org.opencv.core.DEBUG", "true");

        try {
            System.out.println("Loading OpenCV...");
            nu.pattern.OpenCV.loadLocally();
            System.out.println("OpenCV loaded successfully!");
        } catch (Throwable t) {
            System.err.println("Failed to load OpenCV: " + t.getMessage());
            t.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("Loading FXML...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterPersonne.fxml"));

            Parent root = loader.load();
            System.out.println("FXML loaded successfully");

            AjouterPersonne.setPrimaryStage(primaryStage);

            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Rehletna.tn - Login");
            primaryStage.show();

            System.out.println("Application started successfully!");

        } catch (Exception e) {
            System.err.println("Error starting application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}