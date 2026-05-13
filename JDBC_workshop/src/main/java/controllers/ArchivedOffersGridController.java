package controllers;

import app.Session;
import entities.Offre;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.layout.TilePane;
import repositories.IOffreRepository;
import repositories.OffreRepository;

import java.util.List;

public class ArchivedOffersGridController {

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

    private void refresh() {
        tilePane.getChildren().clear();

        if (!Session.isAgency()) {
            showError("Access denied", "Archive is available for agencies only.");
            return;
        }

        List<Offre> offers = repo.findArchivedByAgency(Session.getUserId());

        if (offers.isEmpty()) return;

        for (Offre offer : offers) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ArchivedOfferCard.fxml"));
                Parent card = loader.load();

                ArchivedOfferCardController ctrl = loader.getController();
                ctrl.setData(offer, this::refresh);

                tilePane.getChildren().add(card);

            } catch (Exception e) {
                showError("UI error", e.getMessage());
            }
        }
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}