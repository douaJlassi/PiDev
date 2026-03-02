package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class MainLayoutBack {
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private void showDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/BackofficeView.fxml"));
            mainBorderPane.setCenter(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
