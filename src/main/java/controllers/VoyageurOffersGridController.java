package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.TilePane;
import entities.Offre;
import javafx.stage.Stage;
import repositories.IOffreRepository;
import repositories.OffreRepository;

import java.io.IOException;
import java.util.List;
import controllers.OfferFilterAware;
import entities.OfferFilter;

public class VoyageurOffersGridController implements OfferFilterAware {
    private OfferFilter currentFilter = new OfferFilter();


    @FXML private TilePane tilePane;
    private final IOffreRepository repo = new OffreRepository();

    @FXML
    public void initialize() { refresh(); }

    @FXML
    private void onRefresh() { refresh(); }

    public void refresh() {
        tilePane.getChildren().clear();

        List<Offre> offers = repo.searchActiveOffers(currentFilter); // all offers
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
    @Override
    public void applyFilter(OfferFilter filter) {
        this.currentFilter = (filter == null) ? new OfferFilter() : filter;
        refresh();
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    @FXML
    private void onMyCart() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CartView.fxml"));
            Parent root = loader.load();

            CartViewController ctrl = loader.getController();
            ctrl.loadCart(); // will use Session user

            Stage stage = new Stage();
            stage.setTitle("My Cart");
            Scene scene = new Scene(root, 950, 650);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            stage.setScene(scene);
            stage.show();


        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("UI error");
            a.setHeaderText(null);
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }



}
