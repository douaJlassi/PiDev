package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        //  Parent root = FXMLLoader.load(getClass().getResource("/views/GuideActivities.fxml"));
        Parent root = FXMLLoader.load(getClass().getResource("/views/Dashboard.fxml"));
        Scene scene = new Scene(root);


//existing styles for cards, etc.
        scene.getStylesheets().add(getClass().getResource("/views/style.css").toExternalForm());


// dashboard theme (separate)
        //scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());

        stage.setScene(scene);
        stage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}