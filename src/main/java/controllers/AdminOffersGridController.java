package Controllers;

import app.Session;
import entities.Agency;
import entities.OfferFilter;
import entities.Offre;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import repositories.AgencyRepository;
import repositories.IOffreRepository;
import repositories.OffreRepository;
import javafx.scene.control.ComboBox;

import java.io.IOException;
import java.util.List;

public class AdminOffersGridController implements Controllers.OfferFilterAware {

    @FXML private TilePane tilePane;
    @FXML private ComboBox<Agency> agencyCb;
    @FXML private Label countLbl;


    private final IOffreRepository offreRepo = new OffreRepository();
    private final AgencyRepository agenceRepo = new AgencyRepository();
    private OfferFilter currentFilter = new OfferFilter();



    @FXML
    public void initialize() {
        if (!Session.isAdmin()) {
            showError("Access denied", "Admins only.");
            return;
        }

        loadAgencies();
        refresh();
    }

    private void loadAgencies() {
        agencyCb.getItems().clear();

        // "All agencies" entry
        agencyCb.getItems().add(new Agency(0, "All agencies"));
        agencyCb.getSelectionModel().selectFirst();

        agencyCb.getItems().addAll(agenceRepo.findAllValidated());
    }
    @Override
    public void applyFilter(OfferFilter filter) {
        this.currentFilter = (filter == null) ? new OfferFilter() : filter;
        refresh();
    }

    @FXML
    private void onAgencyChanged() {
        refresh();
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    @FXML
    private void onAdd() {
        OffreFormController.openDialog(null, this::refresh);
    }

    public void refresh() {
        tilePane.getChildren().clear();

        Agency selected = agencyCb.getSelectionModel().getSelectedItem();
        Integer agencyId = null;

        if (selected != null && selected.getIdUser() != 0) agencyId = selected.getIdUser();

        List<Offre> offers = offreRepo.searchActiveOffers(currentFilter);
        if (countLbl != null) {
            countLbl.setText("Offers: " + offers.size());
        }

        for (Offre offer : offers) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OfferCard.fxml"));
                Parent card = loader.load();

                OfferCardController ctrl = loader.getController();
                ctrl.setData(offer, this::refresh);

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
