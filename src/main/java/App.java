import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Load the FXML
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Dashboard.fxml"));

        // 2. Create the Scene ONCE
        Scene scene = new Scene(root);

        // 3. Find the CSS (Looking in the root, not in /fxml/)
        URL cssResource = getClass().getResource("/fxml/style.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        } else {
            System.err.println("Could not find style.css in src/main/resources/");
        }

        // 4. Set the stage
        primaryStage.setTitle("Travel Dashboard");
        primaryStage.setScene(scene); // Use the variable 'scene' here
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}