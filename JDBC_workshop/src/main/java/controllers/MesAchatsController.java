package controllers;

import services.AchatService;
import services.ActiviteService;
import gestion_activite.Achat;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

public class MesAchatsController {

    @FXML
    private ListView<Achat> achatsListView;
    @FXML
    private Button backButton;

    private DashboardControllersahar dashboardController;
    private int clientId;
    private AchatService achatService = new AchatService();
    private ActiviteService activiteService = new ActiviteService();

    public void setClientId(int clientId) {
        this.clientId = clientId;
        chargerAchats();
    }

    public void setDashboardController(DashboardControllersahar controller) {
        this.dashboardController = controller;
    }

    private void chargerAchats() {
        try {
            List<Achat> achats = achatService.selectByClient(clientId);
            achatsListView.getItems().setAll(achats);
            achatsListView.setCellFactory(lv -> new ListCell<Achat>() {
                @Override
                protected void updateItem(Achat achat, boolean empty) {
                    super.updateItem(achat, empty);
                    if (empty || achat == null) {
                        setGraphic(null);
                    } else {
                        setGraphic(creerCellule(achat));
                    }
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Node creerCellule(Achat achat) {
        try {
            var activite = activiteService.selectById(achat.getIdActivite());
            String titre = (activite != null) ? activite.getTitre() : "Activité inconnue";

            VBox cell = new VBox(5);
            cell.setStyle("-fx-padding: 10; -fx-border-color: #ddd; -fx-border-radius: 5;");
            Text titreText = new Text(titre);
            titreText.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
            Text info = new Text(
                    String.format("Date: %s | Places: %d | Total: %.0f DT | Statut: %s",
                            new SimpleDateFormat("dd/MM/yyyy").format(achat.getDateAchat()),
                            achat.getNbPlaces(),
                            achat.getMontantTotal(),
                            achat.getStatut())
            );
            info.setStyle("-fx-font-size: 12;");
            cell.getChildren().addAll(titreText, info);



            return cell;
        } catch (SQLException e) {
            e.printStackTrace();
            return new Text("Erreur chargement");
        }
    }


/*
    @FXML
    private void handleBack() {
        if (dashboardController != null) {
            dashboardController.showDashboardView();
        }
    }*/
}