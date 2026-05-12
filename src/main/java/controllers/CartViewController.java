package controllers;

import app.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CartViewController {

    @FXML
    private VBox itemsBox;

    @FXML
    private Label totalLbl;

    @FXML
    private Label countLbl;

    public void loadCart() {
        showNoCartMessage();
    }

    @FXML
    public void initialize() {
        showNoCartMessage();
    }

    private void showNoCartMessage() {
        if (itemsBox != null) {
            itemsBox.getChildren().clear();

            Label msg = new Label(
                    "Cart is disabled.\nReservations are now created directly from each offer."
            );

            msg.setWrapText(true);
            msg.setStyle(
                    "-fx-font-size: 15px; " +
                            "-fx-text-fill: #475569; " +
                            "-fx-padding: 20;"
            );

            itemsBox.getChildren().add(msg);
        }

        if (totalLbl != null) {
            totalLbl.setText("Total: -");
        }

        if (countLbl != null) {
            countLbl.setText("0 items");
        }
    }

    @FXML
    private void onBack() {
        Stage stage = (Stage) itemsBox.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onClear() {
        showInfo(
                "Cart disabled",
                "There is no cart anymore. Please reserve directly from an offer."
        );
    }

    @FXML
    private void onCheckout() {
        showInfo(
                "Cart disabled",
                "There is no checkout anymore. Reservations are created directly from offers."
        );
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}