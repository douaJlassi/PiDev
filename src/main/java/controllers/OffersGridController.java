package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.layout.TilePane;
import entities.Offre;
import repositories.IOffreRepository;
import repositories.OffreRepository;
import app.Session;


import java.io.IOException;
import java.util.List;


public class OffersGridController {

    @FXML private TilePane tilePane;

    private final IOffreRepository repo = new OffreRepository();

    @FXML
    public void initialize() {
        refresh();
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    @FXML
    private void onAdd() {
        if (!Session.isAgency()) {
            showError("Access denied", "Only agencies can add offers.");
            return;
        }

        OffreFormController.openDialog(null, () -> refresh());
    }

    public void refresh() {
        tilePane.getChildren().clear();

        if (!Session.isAgency()) {
            showError("Access denied", "This screen is for agencies only.");
            return;
        }

        List<Offre> offers = repo.findAllByAgency(Session.getUserId());
        for (Offre offer : offers) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OfferCard.fxml"));
                Parent card = loader.load();

                OfferCardController ctrl = loader.getController();
                ctrl.setData(offer, () -> refresh());

                tilePane.getChildren().add(card);

            } catch (IOException e) {
                showError("UI error", e.getMessage());
            }
        }
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
