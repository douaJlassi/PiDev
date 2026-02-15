package controllers;

import app.Session;
import entities.CartItem;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import repositories.LignePanierRepository;
import repositories.ReservationRepository;

import java.util.List;

public class CartViewController {

    @FXML private VBox itemsBox;
    @FXML private Label totalLbl;

    private final ReservationRepository reservationRepo = new ReservationRepository();
    private final LignePanierRepository ligneRepo = new LignePanierRepository();

    private int cartId;

    public void loadCart() {
        if (!Session.isClient()) {
            showError("Access denied", "Only clients can access the cart.");
            return;
        }

        cartId = reservationRepo.getOrCreateDraftCart(Session.getUserId());
        refresh();
    }

    private void refresh() {
        itemsBox.getChildren().clear();

        List<CartItem> items = ligneRepo.findCartItems(cartId);

        // total: easiest = recompute then query it (we add a method next step)
        reservationRepo.recomputeTotal(cartId);
        totalLbl.setText("Total: " + reservationRepo.getTotal(cartId) + " TND");

        if (items.isEmpty()) {
            itemsBox.getChildren().add(new Label("Your cart is empty."));
            return;
        }

        for (CartItem it : items) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CartItemCard.fxml"));
                Parent card = loader.load();

                CartItemCardController ctrl = loader.getController();
                ctrl.setData(it, () -> {
                    ligneRepo.removeOffer(cartId, it.getIdOffre());
                    reservationRepo.recomputeTotal(cartId);
                    refresh();
                });

                itemsBox.getChildren().add(card);
            } catch (Exception e) {
                itemsBox.getChildren().add(new Label("Error loading cart item: " + e.getMessage()));
            }
        }
    }

    @FXML
    private void onBack() {
        Stage stage = (Stage) itemsBox.getScene().getWindow();
        stage.close();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
