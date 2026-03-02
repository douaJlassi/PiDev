package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Session.loginAs(1, "AGENCE"); // test

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MyDashboard.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 750);

//existing styles for cards, etc.
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());


// dashboard theme (separate)
        scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());

        stage.setScene(scene);
        stage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
