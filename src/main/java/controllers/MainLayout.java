package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import services.ServiceMessage;

import java.io.IOException;
import java.sql.SQLException;

public class MainLayout {

    @FXML
    private BorderPane mainBorderPane;

    @FXML
    private VBox sideBar;
    @FXML
    private Label lblChat;
    @FXML
    private Button btnChat;

    @FXML
    private Label lblBadge;
    private ServiceMessage serMsg = new ServiceMessage();
    private int currentUserId =12;

    private static MainLayout instance;
    public static MainLayout getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;

        Platform.runLater(() -> {
            refreshBadge();
        });
    }
    public void refreshBadge() {
        int count = 0;
        try {
            count = serMsg.countUnreadMessages(currentUserId);
            System.out.println("DEBUG BADGE - Nombre trouvé : " + count);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (count > 0) {
                lblBadge.setText(String.valueOf(count > 99 ? "99+" : count)); // On limite à 99+
                lblBadge.setVisible(true);
                lblBadge.setManaged(true);
            } else {
                lblBadge.setVisible(false);
                lblBadge.setManaged(false);
            }

    }
    @FXML
    private void showChatView() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/ChatView.fxml"));
            mainBorderPane.setCenter(root);
            refreshBadge();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
