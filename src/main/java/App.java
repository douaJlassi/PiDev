import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;


public class App extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/GuideActivities.fxml"));
        //Parent root = FXMLLoader.load(getClass().getResource("/fxml/Dashboard.fxml"));

        Scene scene = new Scene(root);

        URL cssResource = getClass().getResource("/fxml/style.css");
        if (cssResource != null)
        {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }
        else
        {
            System.err.println("Could not find style.css in src/main/resources/");
        }

        primaryStage.setTitle("Travel Dashboard");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}