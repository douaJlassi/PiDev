package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import controllers.AjouterPersonne;

import java.io.File;
import java.io.IOException;

public class MainFX extends Application {

    static {
        System.out.println("=== System Properties ===");
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("Java home: " + System.getProperty("java.home"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("OS arch: " + System.getProperty("os.arch"));
        System.out.println("java.library.path: " + System.getProperty("java.library.path"));

        // Set JavaCV/OpenCV to use less memory
        System.setProperty("org.bytedeco.javacpp.maxbytes", "512M");
        System.setProperty("org.bytedeco.javacpp.maxphysicalbytes", "1G");

        try {
            System.out.println("Loading JavaCV/OpenCV...");

            // Test loading by trying to access OpenCV version
            org.bytedeco.opencv.global.opencv_core.CV_VERSION.getClass();

            System.out.println("✅ JavaCV/OpenCV loaded successfully!");
            System.out.println("OpenCV version: " + org.bytedeco.opencv.global.opencv_core.CV_VERSION);

        } catch (Throwable t) {
            System.err.println("❌ Failed to load JavaCV/OpenCV: " + t.getMessage());
            t.printStackTrace();

            // Try to load from specific path if needed
            try {
                String dllPath = "C:/opencv-build/opencv/build/bin/Release/opencv_java480.dll";
                File dllFile = new File(dllPath);
                if (dllFile.exists()) {
                    System.load(dllPath);
                    System.out.println("✅ OpenCV loaded from: " + dllPath);
                }
            } catch (Throwable t2) {
                System.err.println("❌ Fallback loading also failed: " + t2.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("Loading FXML...");
            //FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AjouterPersonne.fxml"));
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/mainpage.fxml"));
            Parent root = loader.load();
            System.out.println("FXML loaded successfully");

            AjouterPersonne.setPrimaryStage(primaryStage);

            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Rehletna.tn - Login");
            primaryStage.show();
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());


            System.out.println("Application started successfully!");

        } catch (Exception e) {
            System.err.println("Error starting application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        /*FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainLayout.fxml"));
        try {
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Rehletna.tn");
            primaryStage.show();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }*/
    }
}