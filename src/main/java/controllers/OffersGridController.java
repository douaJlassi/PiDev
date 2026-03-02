package Controllers;

import entities.OfferFilter;
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


public class OffersGridController implements OfferFilterAware {

    @FXML private TilePane tilePane;

    private final IOffreRepository repo = new OffreRepository();
    private OfferFilter currentFilter = new OfferFilter();


    @FXML
    public void initialize() {
        // default: agency should see only their offers
        if (Session.isAgency()) {
            currentFilter.getAgencyIds().clear();
            currentFilter.getAgencyIds().add(Session.getUserId());
        }
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
    @Override
    public void applyFilter(OfferFilter f) {
        // Dashboard will push filter here
        currentFilter = (f == null) ? new OfferFilter() : f;

        // safety: agency must always stay scoped to itself
        if (Session.isAgency()) {
            currentFilter.getAgencyIds().clear();
            currentFilter.getAgencyIds().add(Session.getUserId());
        }

        refresh();
    }


    public void refresh() {
        tilePane.getChildren().clear();

        if (!Session.isAgency()) {
            showError("Access denied", "This screen is for agencies only.");
            return;
        }

        List<Offre> offers = repo.searchActiveOffers(currentFilter);
        for (Offre offer : offers) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OfferCard.fxml"));
                Parent card = loader.load();

                Controllers.OfferCardController ctrl = loader.getController();
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
