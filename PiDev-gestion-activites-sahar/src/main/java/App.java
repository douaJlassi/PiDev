import services.ActiviteService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;
import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;

import java.util.concurrent.TimeUnit;


public class App extends Application {
    private ScheduledExecutorService scheduler;
    @Override
    public void start(Stage primaryStage) throws Exception
    {
       Parent root = FXMLLoader.load(getClass().getResource("/views/GuideActivities.fxml"));
        //   Parent root = FXMLLoader.load(getClass().getResource("/views/Dashboard.fxml"));

        Scene scene = new Scene(root);

        URL cssResource = getClass().getResource("/views/style.css");
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
        startActivityCleanupScheduler();
    }
    private void startActivityCleanupScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                new ActiviteService().deleteExpiredActivities();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.DAYS); // démarre immédiatement, puis tous les jours
    }


    @Override
    public void stop() throws Exception {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}