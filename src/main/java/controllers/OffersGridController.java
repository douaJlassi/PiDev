package controllers;

import app.Session;
import entities.Offre;
import entities.OfferFilter;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import repositories.IOffreRepository;
import repositories.OffreRepository;

import java.io.IOException;
import java.util.List;

public class OffersGridController {

    @FXML private TilePane tilePane;

    @FXML private StackPane filtersHost;
    @FXML private Button filtersBtn;

    private OfferFiltersPanelController filtersCtrl;

    private final IOffreRepository repo = new OffreRepository();
    private OfferFilter currentFilter = new OfferFilter();

    private boolean filtersVisible = false;

    @FXML
    public void initialize() {
        // default filter: agency sees only its offers
        if (Session.isAgency()) {
            currentFilter.getAgencyIds().clear();
            currentFilter.getAgencyIds().add(Session.getUserId());
        }

        // load filters panel inside right area
        loadFiltersPanel();

        refresh();
    }

    private void loadFiltersPanel() {
        if (filtersHost == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OfferFiltersPanel.fxml"));
            Parent panelRoot = loader.load();
            filtersCtrl = loader.getController();

            filtersHost.getChildren().setAll(panelRoot);

            // initial state hidden
            filtersHost.setVisible(false);
            filtersHost.setManaged(false);

            // connect callbacks
            filtersCtrl.setFilter(currentFilter);
            filtersCtrl.setOnChanged(f -> {
                currentFilter = (f == null) ? new OfferFilter() : f;

                // keep agency scoped
                if (Session.isAgency()) {
                    currentFilter.getAgencyIds().clear();
                    currentFilter.getAgencyIds().add(Session.getUserId());
                }

                refresh();
            });

            filtersCtrl.setOnClose(this::hideFilters);

        } catch (Exception e) {
            e.printStackTrace();
            showError("UI error", "Cannot load filters panel: " + e.getMessage());
        }
    }

    @FXML
    private void onToggleFilters() {
        filtersVisible = !filtersVisible;

        if (filtersHost != null) {
            filtersHost.setVisible(filtersVisible);
            filtersHost.setManaged(filtersVisible);
        }
        if (filtersBtn != null) {
            filtersBtn.setText(filtersVisible ? "Filters ◂" : "Filters ▾");
        }
    }

    private void hideFilters() {
        filtersVisible = false;
        if (filtersHost != null) {
            filtersHost.setVisible(false);
            filtersHost.setManaged(false);
        }
        if (filtersBtn != null) {
            filtersBtn.setText("Filters ▾");
        }
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
        OffreFormController.openDialog(null, this::refresh);
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